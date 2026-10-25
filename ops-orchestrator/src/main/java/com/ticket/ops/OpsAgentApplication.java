package com.ticket.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 运维 Agent 独立启动入口。
 * <p>
 * 默认端口 18080，P0 不依赖 MySQL / Redis / RabbitMQ / Nacos，
 * 仅通过 HTTP 提供规则、事件、门禁与健康检查能力。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class OpsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsAgentApplication.class, args);
    }
}
