package com.demo.trustdeskAi.config;

import com.demo.trustdeskAi.tools.OrderDetailsLookupTool;
import com.demo.trustdeskAi.tools.PolicyLookupTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AgentConfig {

    /**
     * Automatically creates the 'vector' extension in PostgreSQL on application startup
     * if it does not already exist.
     */
    @Bean
    public CommandLineRunner initPgVectorExtension(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector;");
        };
    }

    /**
     * Single ChatClient bean configured once for the entire application.
     * Spring AI automatically supplies the 'builder' parameter.
     */
    @Bean
    public ChatClient chatClient(
            ChatClient.Builder builder,
            PolicyLookupTool policyLookupTool,
            OrderDetailsLookupTool orderDetailsLookupTool,
            @Value("classpath:guardrails/main_agent_guardrail.md") Resource guardrail) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

//        return builder
//                .defaultSystem(guardrail)
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
//                .defaultTools(policyLookupTool)
//                .build();
//    }
        return builder
                .defaultSystem(guardrail)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(policyLookupTool,orderDetailsLookupTool)//mention the default tools here or all tools here comma seperated
                .build();

    }
}