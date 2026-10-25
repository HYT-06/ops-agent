package com.ticket.ops.config;

import com.ticket.ops.analyzer.fallback.AnalysisFallbackHandler;
import com.ticket.ops.analyzer.prompt.PromptContextBuilder;
import com.ticket.ops.analyzer.rag.NoOpRagRetriever;
import com.ticket.ops.analyzer.rag.RagRetriever;
import com.ticket.ops.analyzer.skill.AnalysisSkillRegistry;
import com.ticket.ops.analyzer.skill.DlqSkill;
import com.ticket.ops.analyzer.skill.FullGcSkill;
import com.ticket.ops.analyzer.skill.SlowSqlSkill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 分析层装配。P0 技能均为占位实现，依赖中不包含任何写 Registry。
 */
@Configuration
public class AnalysisConfig {

    @Bean
    public AnalysisSkillRegistry analysisSkillRegistry() {
        return new AnalysisSkillRegistry(List.of(
                new FullGcSkill(), new SlowSqlSkill(), new DlqSkill()));
    }

    @Bean
    public PromptContextBuilder promptContextBuilder() {
        return new PromptContextBuilder();
    }

    @Bean
    public RagRetriever ragRetriever() {
        return new NoOpRagRetriever();
    }

    @Bean
    public AnalysisFallbackHandler analysisFallbackHandler() {
        return new AnalysisFallbackHandler();
    }
}
