# 版本说明

本文件记录 `ops-agent` 的版本演进、已交付能力、未完成事项与验证结果。

---

## v0.1.0-SNAPSHOT — P0 骨架阶段（2026-10-25）

> 首个可独立构建、独立启动的版本。目标是把「规则检测 → 分析 → 门禁 → 执行 → 验证 → 知识沉淀」的骨架与安全边界打牢，真实数据源、LLM 推理与持久化留待后续阶段。

### 一、已完成

#### 1. 工程与构建

- 采用 Maven 多模块结构，统一父 POM 管理版本：`Spring Boot 3.5.15`、`Java 17`、`Spring AI 1.1.2`、`Spring AI Alibaba 1.1.2.0`。
- 9 个模块：`ops-common`、`ops-probe-sdk`、`ops-mcp-core`、`ops-mcp-plugins`、`ops-analyzer`、`ops-knowledge`、`ops-gate`、`ops-orchestrator`、`ops-eval`。
- 提供 `start.cmd` / `start.sh`，支持 `start`、`dev`、`build`、`test`、`run` 五种模式。
- 可执行 jar：`ops-orchestrator/target/ops-orchestrator-0.1.0-SNAPSHOT.jar`。
- `.gitignore` 已排除构建产物、运行日志与本地敏感配置（`application-local.yml`）。

#### 2. L0 采集层：探针 SDK（`ops-probe-sdk`）

- `@OpsProbe` 注解：支持探针类型、SpEL 业务键、采样率、参数大小记录、脱敏开关。
- AOP 切面：采集逻辑全程 try-catch，任何采集异常都不会影响业务方法。
- `ProbeTypeResolver`：按方法特征自动推断 `GENERAL` / `MEMORY_PRESSURE` / `SQL_CONTEXT` / `MQ_CONTEXT`。
- `ProbeSampler`：按 1~100 采样率随机采样。
- `ParamDesensitizer`：手机号、身份证、银行卡、邮箱脱敏。
- `SpelBizKeyExtractor`：从方法参数中提取业务键。
- 有界队列 + 单线程批量异步上报（`ProbeRecordQueue` / `ProbeRecordUploader`），上报失败只记录日志并丢弃本批次。
- Spring Boot 自动装配，`ops.probe.enabled=false` 可整体关闭；默认 `LoggingProbeRecordSink`，可通过自定义 `ProbeRecordSink` 替换。

#### 3. MCP 微内核（`ops-mcp-core`）

- `ToolRegistry` 读写权限隔离：只读 Registry 拒绝注册写插件，写 Registry 拒绝注册只读插件。
- 两级发现：`list(namespace)` 返回工具清单，`detail(toolName)` 按需返回 schema。
- 插件注册 / 注销、工具名冲突检测、命名空间过滤。
- 写工具调用强制携带 `idempotencyKey`，缺失直接抛异常。
- 提供 `snapshot` 与 `execute` 统一入口，写操作执行前可获取当前状态快照。

#### 4. 内置插件（`ops-mcp-plugins`，P0 占位）

只读插件（契约已定义，数据源未接入）：

| 工具 | 用途 |
| --- | --- |
| `gc.queryGcLog` | 查询时间窗口内 GC 日志摘要 |
| `gc.queryJvmMetrics` | 查询 JVM 堆、线程、CPU 指标 |
| `sql.querySlowLog` | 查询慢 SQL 日志 |
| `sql.explain` | 获取 SQL 执行计划 |
| `sql.queryTableStats` | 查询表行数与索引基数 |
| `mq.queryDlqMessages` | 查询死信消息列表 |
| `mq.queryDlqSummary` | 按异常类型聚合死信 |

写插件（内存占位，不会改动真实应用）：

| 工具 | 风险 | 说明 |
| --- | --- | --- |
| `exec.updateThreadPool` | 低 / 高（按扩容比例） | 调整线程池 coreSize / maxSize |
| `exec.scaleConsumer` | 低 | 调整消费者实例数 |
| `dlq.replay` | 高 | 死信重放（必须人工审批） |

#### 5. L1 规则引擎

- FullGC：`gcCount5m > 3` 或 `gcAvgPauseMs > 1000` 触发，去重窗口 300 秒。
- 慢 SQL：`slowSqlCountPerMin > 10` 或 `slowSqlP99Ms > 2000` 触发，去重窗口 300 秒。
- DLQ：`dlqDepth > 0` 触发，去重窗口 60 秒。
- 阈值与去重窗口均可通过 `ops.rules.*` 配置。
- 输出标准化 `AnalysisRequest`，不消耗任何 token。

#### 6. L2 编排层

- 事故生命周期状态：`RECEIVED → ANALYZING → GATE_CHECK → APPROVING → EXECUTING → VERIFYING → ARCHIVED / ROLLED_BACK / FAILED`。
- `IncidentWorkflow` 串起分析、门禁、执行、验证、知识晋升。
- 记录采用不可变快照，每次状态变更生成新记录，便于审计与并发读取。
- 内存在线事件存储，支持创建、列表、查询、推进。

#### 7. L3 分析层骨架

- `AnalysisSkill` 接口与 `AnalysisSkillRegistry`，按场景路由到 `FullGcSkill` / `SlowSqlSkill` / `DlqSkill`。
- `PromptContextBuilder`：系统指令与不可信证据分区，剥离 `ignore previous instructions`、`you are now`、`system:` 等注入模式。
- `AnalysisFallbackHandler`：分析异常时返回结构化失败报告，避免拖垮运维流程。
- `RagRetriever` 接口 + `NoOpRagRetriever` 占位实现。

#### 8. L4 执行门禁（确定性代码）

- 只接受结构化 `AnalysisReport` / `SuggestedAction`，不接受自然语言。
- 三件套校验：工具名、参数、回滚点、幂等键缺一不可。
- 工具授权校验：只允许写 Registry 中已注册的工具。
- 参数硬边界：`exec.updateThreadPool`、`exec.scaleConsumer`、`dlq.replay` 已配置 min/max。
- 风险分级：低风险自动放行，高风险进入人工审批；未识别动作默认高风险。
- 审批回调存储、日志式审批通知、定时超时检查（默认 30 秒检查一次，15 分钟超时自动拒绝）。
- 幂等前置检查：已消费的 `idempotencyKey` 直接跳过重复执行。
- 回滚点注册：写操作执行前先注册快照与回滚点。
- 审计账本：门禁判定、执行成功、执行失败均落账。

#### 9. L5 验证层骨架

- `DeterministicVerifier` 不使用 LLM，仅根据执行结果判定成功 / 失败。

#### 10. L6 知识层

- 知识分层：`CANDIDATE → VERIFIED → DEPRECATED`。
- 候选池写入前强制冲突检测。
- 风险策略分级：高风险知识必须人工审批，低风险且证据充分才自动晋升。
- 自动晋升条件：验证次数 ≥ 3、置信度 ≥ 0.75、适用边界非空。
- 审计账本 append-only 语义（内存实现）。

#### 11. HTTP API 与可观测性

- 端口 `18080`，提供健康检查、规则上报、事件 CRUD/推进、门禁诊断接口。
- 内置静态运维控制台，可查看服务状态、事件列表、接口清单与模块清单。
- Actuator 暴露 `health`、`info`、`prometheus`、`metrics`。
- Prometheus 直方图已为 `http.server.requests` 开启。

#### 12. 测试

- 16 个测试类、49 个测试用例。
- 覆盖门禁、规则引擎、探针切面/采样/脱敏/自动装配、知识治理、审计账本、HTTP API、评测集加载。

---

### 二、未完成

| # | 事项 | 当前状态 | 目标阶段 | 关键位置 |
| --- | --- | --- | --- | --- |
| 1 | **LLM 分析接入** | 三个 Skill 均返回占位报告，不调用任何模型 | P1 | `ops-analyzer/.../skill/*Skill.java` |
| 2 | **Spring AI Alibaba Graph 状态机** | 仅引入 `spring-ai-alibaba-graph-core` 依赖，编排仍是普通 Java 顺序调用 | P1 | `ops-orchestrator/.../workflow/IncidentWorkflow.java` |
| 3 | **真实只读数据源** | GC/JVM、慢 SQL、DLQ 插件均返回占位数据 | P1 / P3 | `ops-mcp-plugins/.../common/AbstractReadOnlyPlugin.java` |
| 4 | **真实写操作** | 线程池/消费者调整、DLQ 重放均为内存占位，不影响真实应用 | P2 / P3 | `ThreadPoolExecWritePlugin`、`DlqReplayWritePlugin` |
| 5 | **真实补偿回滚** | 回滚仅记录日志并移除回滚点，没有补偿动作 | P2 | `ops-gate/.../rollback/RollbackRegistry.java` |
| 6 | **审批通知通道** | 只有日志输出，未接企业微信 / 钉钉 / QQ Webhook | P2 | `LoggingApprovalNotifier`、`LoggingKnowledgeApprovalSink` |
| 7 | **审批回调 API** | 只有内存回调存储与超时拒绝，没有 HTTP 审批/驳回接口，也没有升级通知 | P2 | `ApprovalCallbackStore`、`ApprovalTimeoutTask` |
| 8 | **事件持久化** | 事故记录存放在 `ConcurrentHashMap`，重启丢失；无 MySQL 表结构 | P3 | `IncidentService` |
| 9 | **知识与审计持久化** | 知识库、审计账本均为内存实现，无法回放 | P3 / P4 | `InMemoryKnowledgeRepository`、`InMemoryAuditLedger` |
| 10 | **RAG 检索** | `NoOpRagRetriever`，未接向量库，未检索可信区知识 | P4 | `AnalysisConfig#ragRetriever` |
| 11 | **离线评测指标** | 仅有 3 条示例用例与「加载成功」输出，没有根因命中率、工具正确率、时延、成本等指标 | P4 | `ops-eval/.../EvalRunner.java` |
| 12 | **Agent 自身可观测性** | 未统计 token 消耗、分析耗时分布、工具调用成功率 | P4 | — |
| 13 | **成本预算控制** | `daily-tokens` / `per-scene-tokens` 仅存在于配置，未实际生效 | P4 | `OpsAgentProperties.Llm.Budget` |
| 14 | **接口鉴权** | `/ops/**` 全部匿名可访问；诊断接口无管理员校验 | 安全加固 | `DiagnosticsController` 注释已标注 |
| 15 | **配置中心与规则热更新** | 规则阈值只能改配置文件后重启，未接 Nacos | P5 | `RuleProperties` |
| 16 | **探针接入被监控应用** | SDK 可用，但尚未在 12306 demo 应用中引入 `@OpsProbe` | P0 验收项 | 主项目 `demo` 模块 |
| 17 | **多实例协同** | 规则去重窗口、事件、审批、知识均为单 JVM 内存态，多实例部署各自计数 | 架构演进 | `RuleEngine` |
| 18 | **MCP 外部协议接入** | 当前是内置微内核，未接 Spring AI MCP Client/Server 与外部 MCP 服务 | P5 | `ops-mcp-core` |
| 19 | **容器化与 CI** | 无 Dockerfile、docker-compose、K8s 清单与 CI 流水线 | 工程化 | — |
| 20 | **接口文档** | 无 OpenAPI / Swagger，仅 README 手写接口说明 | 工程化 | — |

---

### 三、验证记录

验证环境：Windows 11、JDK 24.0.1、Maven 3.9.6。

| 验证项 | 命令 | 结果 |
| --- | --- | --- |
| 单元 / 集成测试 | `mvn test` | 通过：16 个测试类、49 个用例，0 失败、0 错误 |
| 打包 | `mvn -DskipTests package` | 通过，生成可执行 jar |
| 启动 | `java -jar ops-orchestrator/target/ops-orchestrator-0.1.0-SNAPSHOT.jar` | 通过，监听 18080 |
| 健康检查 | `GET /ops/health` | 返回 `status=UP`、`version=0.1.0-SNAPSHOT`、7 个模块 |
| 规则上报 | `POST /ops/signals/evaluate` | 触发 1 条 FullGC 事件 |
| 门禁诊断 | `POST /ops/diagnostics/gate-check`（`dlq.replay`） | 返回 `REQUIRES_APPROVAL`、`riskLevel=HIGH` |

---

### 四、已知限制

- 分析 Skill 为占位实现，推进事件会直接以 `NO_ACTION` 归档，属于 P0 预期行为。
- 所有状态在内存中，重启后事故、审批、知识、审计数据全部丢失。
- 写插件不会修改真实应用状态，`snapshot` 返回的是插件内部计数器。
- 规则去重窗口、审批回调、知识库均不支持跨实例共享。
- 无鉴权，默认只建议本地或隔离内网运行。

---

### 五、下一步计划（P1 起）

1. 接入 Spring AI ChatModel，实现 `FullGcSkill` 的只读证据拉取 + 结构化报告输出。
2. 将 `IncidentWorkflow` 迁移为 Spring AI Alibaba Graph 状态机，保留现有状态枚举与节点语义。
3. 为只读插件接入真实 GC 日志 / JFR / Micrometer、慢日志 / EXPLAIN / 表统计、RabbitMQ 死信查询。
4. 实现 Webhook 审批通知与 HTTP 审批回调（通过 / 驳回 / 升级）。
5. 引入 MySQL 存储事故、审批、知识与审计账本，补 append-only 约束与回放能力。
6. 接入向量库与可信区 RAG 检索。
7. 扩充评测集并实现根因命中率、工具调用正确率、时延与 token 成本指标。
8. 为 `/ops/**` 增加鉴权，并支持关闭诊断接口。
9. 将 `@OpsProbe` 接入 12306 demo 应用，完成 P0 验收闭环。
