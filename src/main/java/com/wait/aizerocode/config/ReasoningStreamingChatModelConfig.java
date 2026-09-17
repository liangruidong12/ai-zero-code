package com.wait.aizerocode.config;


import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Data
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {

        final int maxToken=16484;

        System.out.println("------------------------------"+modelName);

        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .maxTokens(maxToken).build();
    }
}
