package com.ticket.ops.config;

import com.ticket.ops.rule.RuleEngine;
import com.ticket.ops.rule.RuleProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * L1 规则引擎装配。
 */
@Configuration
public class RuleConfig {

    @Bean
    public RuleEngine ruleEngine(RuleProperties properties) {
        return new RuleEngine(properties);
    }
}
