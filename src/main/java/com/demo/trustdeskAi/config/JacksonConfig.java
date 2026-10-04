package com.demo.trustdeskAi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JacksonConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        // Create a mock/stub embedding model to allow the application to start
        // without trying to download the ONNX model from GitHub
        return new EmbeddingModel() {
            @Override
            public float[] embed(String text) {
                return new float[384]; // Return a dummy vector
            }

            @Override
            public java.util.List<float[]> embed(java.util.List<String> texts) {
                java.util.List<float[]> results = new java.util.ArrayList<>();
                for (String text : texts) {
                    results.add(new float[384]);
                }
                return results;
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return embed(document.getText());
            }

            @Override
            public org.springframework.ai.embedding.EmbeddingResponse call(org.springframework.ai.embedding.EmbeddingRequest request) {
                throw new UnsupportedOperationException("Stub model does not support direct call() - use embed() methods");
            }
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Support for LocalDateTime used in entities
        mapper.registerModule(new JavaTimeModule());
        // Prevent failing on unknown properties in JSONL files
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
