# ops-agent

面向 **12306 票务系统**的独立智能运维 Agent，覆盖 FullGC、慢 SQL、DLQ 死信三类故障场景。

项目遵循一条核心链路：**检测用规则，分析用 Agent，执行用确定性代码，验证独立**。当前版本 `0.1.0-SNAPSHOT` 处于 **P0 骨架阶段**：分层结构、MCP 权限隔离、执行门禁、审批超时、回滚注册、知识治理与审计账本均已落地，但 LLM 分析、真实数据源、持久化与审批通知仍为占位实现。

> 已实现 / 未实现清单、验证记录与后续路线图详见 [CHANGELOG.md](CHANGELOG.md)。

---

## 1. 项目定位

| 项目 | 说明 |
| --- | --- |
| 是什么 | 一个可独立启动的 Spring Boot 多模块工程，提供规则检测、事故编排、执行门禁、知识治理与探针 SDK |
| 不是什么 | 不是生产可用的自动化运维平台；当前不含真实 LLM 调用、真实数据源接入和生产级持久化 |
| 运行依赖 | **零外部中间件**：不依赖 MySQL / Redis / RabbitMQ / Nacos / 向量库，默认全部使用内存实现 |
| 与主项目关系 | 从 12306 主项目中抽离的独立子项目，可单独构建、单独部署；探针 SDK 尚未接入主项目应用 |

### 覆盖的故障场景

| 场景 | 触发信号 | Agent 目标 |
| --- | --- | --- |
| FullGC 排查 | GC 频率 / 平均停顿超阈值 | 关联 GC、JVM、堆内存证据，推断根因并给出调优建议 |
| 慢 SQL 排查 | 慢查询条数 / P99 超阈值 | 结合慢日志、执行计划、表统计信息归因（索引、统计信息、锁竞争） |
| DLQ 死信处理 | 死信队列深度 > 阈值 | 失败聚类、根因分类，给出重放 / 修复 / 丢弃建议 |

---

## 2. 核心设计原则

1. **检测用规则，分析用 Agent**：L1 规则引擎零 token，只做阈值判断、告警去重与事件标准化。
2. **分析只读，写入受控**：分析层只能注入只读 Registry；写 Registry 由门禁独占，从结构上隔离写权限。
3. **门禁不接受自然语言**：只接受结构化 `SuggestedAction`，且必须携带「工具名 + 参数 + 回滚点 + 幂等键」。
4. **低风险自动放行，高风险人工审批**：风险评估不经过 LLM，由确定性规则完成。
5. **所有变更可回滚，所有决策可审计**：回滚点先注册再执行，审计账本 append-only。
6. **Agent 自身可降级**：分析异常时返回结构化失败报告，不拖垮运维流程；探针采集异常全部吞掉，不影响业务方法。

---

## 3. 分层架构

| 层 | 职责 | 当前实现状态 |
| --- | --- | --- |
| L0 采集层 | `@OpsProbe` 注解切面、异步队列、采样、脱敏、SpEL 业务键提取 | 已实现（SDK 可用，未接入被监控应用） |
| L1 规则引擎 | GC / 慢 SQL / DLQ 阈值判断、去重窗口、标准化 `AnalysisRequest` | 已实现 |
| L2 编排层 | 事故生命周期状态流转 | 已实现顺序编排骨架（Graph 依赖已预留，未替换） |
| L3 分析层 | Scene Skill、Prompt 上下文构造、RAG 检索、降级 | **占位**：Skill 返回空报告，RAG 为 NoOp，未接 LLM |
| L4 执行门禁 | 三件套校验、参数硬边界、风险分级、审批回调、幂等 | 已实现（确定性代码） |
| L5 验证层 | 确定性指标判定、失败触发回滚 | 骨架已实现（仅校验写工具返回，未接真实指标阈值） |
| L6 知识层 | 候选池、冲突检测、风险分级、自动晋升、审计账本 | 已实现（内存版，未接 MySQL / 向量库） |

事故状态机：

```text
RECEIVED → ANALYZING → GATE_CHECK → APPROVING → EXECUTING → VERIFYING → ARCHIVED
                                    └──────────────┴──────────→ FAILED / ROLLED_BACK
```

---

## 4. 模块结构

```text
ops-agent/
├── pom.xml                     # 聚合父 POM（Spring Boot 3.5.15 / Java 17）
├── start.cmd                   # Windows 构建与启动脚本
├── start.sh                    # Linux / macOS 构建与启动脚本
├── ops-common/                 # 领域模型与信号对象
├── ops-probe-sdk/              # 注解探针 SDK（被监控应用引入）
├── ops-mcp-core/               # MCP 微内核与读写 Registry 隔离
├── ops-mcp-plugins/            # 内置只读 / 写插件（P0 占位）
├── ops-analyzer/               # 场景分析 Skill、Prompt 构造、RAG 接口
├── ops-gate/                   # 执行门禁、参数边界、风险分级、审批、回滚
├── ops-knowledge/              # 知识治理、冲突检测、审计账本
├── ops-orchestrator/           # 可运行入口：规则、事件 API、控制台
└── ops-eval/                   # 离线评测集与运行器骨架
```

| 模块 | 关键内容 |
| --- | --- |
| `ops-common` | `AnalysisRequest`、`AnalysisReport`、`SuggestedAction`、`GateResult`、`IncidentState`、`SceneType`、`OpsSignal`、`EvidenceRef` 等 |
| `ops-probe-sdk` | `@OpsProbe`、`OpsProbeAspect`、`ProbeTypeResolver`、`ProbeSampler`、`ParamDesensitizer`、`SpelBizKeyExtractor`、`ProbeRecordQueue`、`ProbeRecordUploader`、Spring Boot 自动装配 |
| `ops-mcp-core` | `ToolRegistry`（读写隔离、两级发现 `list → detail → execute`）、`McpPlugin`、`McpPluginManifest`、写工具幂等键强制校验 |
| `ops-mcp-plugins` | 只读：`gc.queryGcLog`、`gc.queryJvmMetrics`、`sql.querySlowLog`、`sql.explain`、`sql.queryTableStats`、`mq.queryDlqMessages`、`mq.queryDlqSummary`；写：`exec.updateThreadPool`、`exec.scaleConsumer`、`dlq.replay` |
| `ops-analyzer` | `FullGcSkill` / `SlowSqlSkill` / `DlqSkill`（占位）、`AnalysisSkillRegistry`、`PromptContextBuilder`（不可信数据隔离 + 注入模式剥离）、`RagRetriever` / `NoOpRagRetriever`、`AnalysisFallbackHandler` |
| `ops-gate` | `ExecutionGate`、`ParameterBoundValidator`、`RiskClassifier`、`ApprovalCallbackStore`、`ApprovalNotifier`、`ApprovalTimeoutScheduler`、`RollbackRegistry`、`ActionExecutor` |
| `ops-knowledge` | `InMemoryKnowledgeRepository`、`KnowledgeGovernanceService`、`KnowledgeConflictDetector`、`DefaultKnowledgeRiskPolicy`、`InMemoryAuditLedger` |
| `ops-orchestrator` | 规则引擎、事件 Service/Workflow、REST Controller、控制台静态页、Actuator / Prometheus |
| `ops-eval` | `EvalDataset`（3 条示例用例）、`EvalRunner`（仅验证评测集可加载） |

---

## 5. 技术栈

| 组件 | 版本 / 说明 |
| --- | --- |
| JDK | 17+（实测 JDK 24） |
| Maven | 3.9.x（实测 3.9.6） |
| Spring Boot | 3.5.15 |
| Spring AI | 1.1.2（BOM，供后续 LLM / RAG / MCP 接入） |
| Spring AI Alibaba | 1.1.2.0（BOM + `spring-ai-alibaba-graph-core`，Graph 编排预留） |
| Web / 校验 / 监控 | `spring-boot-starter-web`、`starter-validation`、`starter-actuator`、`micrometer-registry-prometheus` |
| 探针 | `spring-aop`、`aspectjweaver`、`spring-expression` |
| 测试 | JUnit 5、Mockito（`spring-boot-starter-test`） |

---

## 6. 快速开始

### 6.1 环境要求

- JDK 17 及以上
- Maven 3.9.x
- 无需启动数据库、缓存或消息队列

### 6.2 构建与启动

```bash
# 构建并运行（默认）
./start.sh

# 只构建
./start.sh build

# 运行全部测试
./start.sh test

# 开发模式（spring-boot:run，配合 devtools）
./start.sh dev

# 运行已构建的 jar
./start.sh run
```

Windows 使用 `start.cmd {start|dev|build|test|run}`，参数含义相同。

等价 Maven 命令：

```bash
mvn -DskipTests package
java -jar ops-orchestrator/target/ops-orchestrator-0.1.0-SNAPSHOT.jar
```

启动后默认监听 **18080**：

- 运维控制台：<http://localhost:18080/>
- 健康检查：<http://localhost:18080/ops/health>
- Actuator：<http://localhost:18080/actuator/health>、`/actuator/prometheus`

### 6.3 冒烟验证

```bash
# 1. 健康检查
curl http://localhost:18080/ops/health

# 2. 上报指标，触发规则
curl -X POST http://localhost:18080/ops/signals/evaluate \
  -H "Content-Type: application/json" \
  -d '{"serviceName":"order-service","gcCount5m":20,"gcAvgPauseMs":1800,
       "slowSqlCountPerMin":0,"slowSqlP99Ms":0,"dlqDepth":0}'

# 3. 门禁诊断：高风险动作应返回 REQUIRES_APPROVAL
curl -X POST http://localhost:18080/ops/diagnostics/gate-check \
  -H "Content-Type: application/json" \
  -d '{"scene":"DLQ","resourceId":"order-queue","toolName":"dlq.replay",
       "params":{"messageId":"m1","batchSize":10},
       "rollbackPoint":"none","idempotencyKey":"idem-1"}'
```

> 当前分析 Skill 为占位实现，`POST /ops/incidents/{requestId}/analyze` 会直接进入 `ARCHIVED`（门禁判定 `NO_ACTION`），这是 P0 的预期行为。

---

## 7. API 一览

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/ops/health` | 基础健康检查，返回版本、事件数与模块清单 |
| POST | `/ops/signals/evaluate` | 上报指标快照，按规则生成运维事件 |
| POST | `/ops/incidents` | 人工创建运维事件 |
| GET | `/ops/incidents` | 查询全部事件（按创建时间倒序） |
| GET | `/ops/incidents/{requestId}` | 查询单个事件 |
| POST | `/ops/incidents/{requestId}/analyze` | 推进事件：分析 → 门禁 → 执行 → 验证 |
| POST | `/ops/diagnostics/gate-check` | 直接验证门禁三件套 / 参数边界 / 风险分级（仅诊断用） |
| GET | `/actuator/health`、`/actuator/prometheus` | Actuator 健康与 Prometheus 指标 |

### 请求示例

**上报指标** `POST /ops/signals/evaluate`

```json
{
  "serviceName": "order-service",
  "gcCount5m": 20,
  "gcAvgPauseMs": 1800,
  "slowSqlCountPerMin": 30,
  "slowSqlP99Ms": 3500,
  "dlqDepth": 0
}
```

**创建事件** `POST /ops/incidents`

```json
{
  "scene": "FULLGC",
  "resourceId": "order-service",
  "summary": "GC 频率突增",
  "attrs": {"pod": "order-0"},
  "metrics": {"gcCount5m": 20, "gcAvgPauseMs": 1800}
}
```

**门禁诊断（低风险，自动放行）** `POST /ops/diagnostics/gate-check`

```json
{
  "scene": "FULLGC",
  "resourceId": "order-service",
  "toolName": "exec.updateThreadPool",
  "params": {"currentCoreSize": 8, "coreSize": 10, "maxSize": 40},
  "rollbackPoint": "coreSize=8,maxSize=32",
  "idempotencyKey": "idem-2"
}
```

门禁判定结果 `GateDecision`：

| 判定 | 含义 |
| --- | --- |
| `NO_ACTION` | 仅为诊断，无执行建议 |
| `AUTO_APPROVED` | 低风险，自动放行 |
| `REQUIRES_APPROVAL` | 高风险，等待人工审批 |
| `REJECTED` | 三件套不完整、工具未授权或参数越界 |

---

## 8. 配置说明

主配置位于 `ops-orchestrator/src/main/resources/application.yml`，核心片段：

```yaml
server:
  port: 18080

spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

ops:
  agent:
    llm:                       # 预留：当前未接入 LLM
      strong-model: qwen-max
      light-model: qwen-turbo
      timeout-seconds: 120
      budget:
        daily-tokens: 5000000
        per-scene-tokens: 1500000
    analysis:
      max-duration-seconds: 300
      evidence-max-items: 50
    gate:
      approval-timeout-minutes: 15
      approval-escalation-minutes: 20
      approval-check-interval-ms: 30000
      bounds:                  # 参数硬边界
        "[exec.updateThreadPool]":
          coreSize: { min: 8, max: 64 }
          maxSize:  { min: 8, max: 128 }
        "[exec.scaleConsumer]":
          consumerCount: { min: 1, max: 16 }
        "[dlq.replay]":
          batchSize: { min: 1, max: 100 }
    rag:                       # 预留：当前为 NoOp 检索
      top-k: 5
      min-confidence: 0.6
      verified-only: true
    knowledge:
      auto-promote-threshold: 3
      auto-promote-confidence: 0.75
  rules:
    fullgc:   { max-gc-count5m: 3, max-avg-pause-ms: 1000, dedup-window-seconds: 300 }
    slow-sql: { max-slow-count-per-min: 10, max-p99-ms: 2000, dedup-window-seconds: 300 }
    dlq:      { max-depth: 0, dedup-window-seconds: 60 }
```

本地差异化配置放入 `ops-orchestrator/src/main/resources/application-local.yml`（已在 `.gitignore` 中忽略，不会入库）。直接运行 jar 时，也可通过 `--spring.config.additional-location` 或环境变量覆盖。

配置项说明：

| 配置 | 是否生效 | 说明 |
| --- | --- | --- |
| `ops.rules.*` | 是 | L1 规则阈值与去重窗口 |
| `ops.agent.gate.*` | 是 | 审批超时/检查间隔、参数硬边界 |
| `ops.agent.knowledge.*` | 是 | 知识自动晋升阈值 |
| `ops.agent.llm.*` | 否 | 预留给后续 LLM 接入 |
| `ops.agent.analysis.*` | 否 | 预留给分析超时与证据裁剪 |
| `ops.agent.rag.*` | 否 | 预留给向量检索 |

---

## 9. 探针 SDK 使用

`ops-probe-sdk` 通过 Spring Boot 自动装配生效，被监控应用只需引入依赖并在方法上标注注解。

### 9.1 引入依赖

```xml
<dependency>
    <groupId>com.ticket.ops</groupId>
    <artifactId>ops-probe-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

模块尚未发布到远程仓库，需先在 `ops-agent` 根目录执行 `mvn install`。

### 9.2 标注方法

```java
@OpsProbe(
        value = ProbeType.SQL_CONTEXT,
        bizKey = "#order.orderId",
        sampleRate = 20,
        desensitize = true)
public OrderDetail queryOrder(Order order) {
    return orderMapper.selectDetail(order);
}
```

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | `AUTO` | 探针类型：`AUTO` / `GENERAL` / `MEMORY_PRESSURE` / `SQL_CONTEXT` / `MQ_CONTEXT` |
| `bizKey` | 空 | SpEL 表达式，提取订单号 / 车次号 / 用户 ID 等业务键 |
| `sampleRate` | `100` | 采样率 1~100 |
| `recordSize` | `false` | 是否记录入参 / 返回值大小（`MEMORY_PRESSURE` 场景） |
| `desensitize` | `true` | 参数脱敏开关（手机号、身份证、银行卡、邮箱） |

### 9.3 SDK 配置

```yaml
ops:
  probe:
    enabled: true            # 总开关
    queue-capacity: 10000    # 有界队列容量
    batch-size: 500          # 批量上报条数
    upload-interval-ms: 2000 # 上报间隔
```

默认上报实现为 `LoggingProbeRecordSink`；接入真实后端时实现 `ProbeRecordSink` Bean 即可覆盖，无需改动切面。

---

## 10. 测试

```bash
mvn test
```

当前共 **16 个测试类、49 个测试用例**，覆盖：

- 门禁：三件套校验、越界拒绝、工具未授权、低/高风险判定、审批超时
- 规则引擎：阈值触发与去重窗口
- 探针 SDK：切面采集、类型推断、采样、脱敏、自动装配
- 知识层：冲突检测、风险策略、审计账本
- API：健康检查、事件创建/查询/推进、信号上报、门禁诊断
- 评测集：用例加载

---

## 11. 版本与路线图

当前版本：**`v0.1.0-SNAPSHOT`（P0 骨架阶段）**

- 已完成：多模块骨架、探针 SDK、MCP 微内核与读写隔离、规则引擎、事件编排骨架、确定性门禁、审批超时、回滚注册、知识治理、审计账本、控制台与监控端点
- 未完成：LLM 分析、真实只读数据源、真实写操作与补偿回滚、Webhook 审批、MySQL / 向量库持久化、RAG、离线评测指标、鉴权、Nacos 热更新、容器化与 CI

逐项说明、验证记录与 P1–P5 计划见 **[CHANGELOG.md](CHANGELOG.md)**。

---

## 12. 已知限制

- 所有事故、审批、知识、审计数据均存放在进程内存中，重启即丢失。
- `/ops/**` 接口目前没有鉴权，仅适合本地或内网调试。
- `/ops/diagnostics/gate-check` 为框架验证入口，生产环境应关闭或加管理员鉴权。
- 分析 Skill 均为占位实现，无法产出真实根因与执行建议。
- MCP 读写插件均为占位实现，不会真正修改线程池、消费者或重放消息。
- 规则去重窗口保存在单 JVM 内存中，多实例部署时各自独立计数。

---

## 13. 许可证

本项目暂未添加开源许可证。如需引用或二次分发，请先与作者联系。
