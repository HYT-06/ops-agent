package com.ticket.ops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 运维 Agent 主配置，字段与设计文档第 7 章对齐。
 */
@ConfigurationProperties(prefix = "ops.agent")
public record OpsAgentProperties(
        Llm llm,
        Analysis analysis,
        Gate gate,
        Rag rag,
        Knowledge knowledge) {

    public OpsAgentProperties {
        llm = llm == null ? new Llm("qwen-max", "qwen-turbo", 120,
                new Budget(5_000_000L, 1_500_000L)) : llm;
        analysis = analysis == null ? new Analysis(300, 50) : analysis;
        gate = gate == null ? new Gate(15, 20, Map.of()) : gate;
        rag = rag == null ? new Rag(5, 0.6, true) : rag;
        knowledge = knowledge == null ? new Knowledge(3, 0.75) : knowledge;
    }

    public record Llm(String strongModel, String lightModel, int timeoutSeconds, Budget budget) {

        public Llm {
            strongModel = strongModel == null || strongModel.isBlank() ? "qwen-max" : strongModel;
            lightModel = lightModel == null || lightModel.isBlank() ? "qwen-turbo" : lightModel;
            timeoutSeconds = timeoutSeconds <= 0 ? 120 : timeoutSeconds;
            budget = budget == null ? new Budget(5_000_000L, 1_500_000L) : budget;
        }
    }

    public record Budget(long dailyTokens, long perSceneTokens) {

        public Budget {
            dailyTokens = dailyTokens <= 0 ? 5_000_000L : dailyTokens;
            perSceneTokens = perSceneTokens <= 0 ? 1_500_000L : perSceneTokens;
        }
    }

    public record Analysis(int maxDurationSeconds, int evidenceMaxItems) {

        public Analysis {
            maxDurationSeconds = maxDurationSeconds <= 0 ? 300 : maxDurationSeconds;
            evidenceMaxItems = evidenceMaxItems <= 0 ? 50 : evidenceMaxItems;
        }
    }

    public record Gate(int approvalTimeoutMinutes, int approvalEscalationMinutes,
                       Map<String, Map<String, Bound>> bounds) {

        public Gate {
            approvalTimeoutMinutes = approvalTimeoutMinutes <= 0 ? 15 : approvalTimeoutMinutes;
            approvalEscalationMinutes = approvalEscalationMinutes <= 0 ? 20 : approvalEscalationMinutes;
            bounds = bounds == null ? Map.of() : Map.copyOf(bounds);
        }
    }

    public record Bound(double min, double max) {

        public Bound {
            if (min > max) {
                throw new IllegalArgumentException("参数边界非法: min=" + min + " max=" + max);
            }
        }
    }

    public record Rag(int topK, double minConfidence, boolean verifiedOnly) {

        public Rag {
            topK = topK <= 0 ? 5 : topK;
            minConfidence = Math.max(0.0, Math.min(1.0, minConfidence));
        }
    }

    public record Knowledge(int autoPromoteThreshold, double autoPromoteConfidence) {

        public Knowledge {
            autoPromoteThreshold = autoPromoteThreshold <= 0 ? 3 : autoPromoteThreshold;
            autoPromoteConfidence = Math.max(0.0, Math.min(1.0, autoPromoteConfidence));
        }
    }
}
