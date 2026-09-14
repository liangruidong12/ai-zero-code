package com.wait.aizerocode.ai;



import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wait.aizerocode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private ChatHistoryService  chatHistoryService;

//    @Bean
//    public AiCodeGeneratorService aiCodeGeneratorService() {
//        return getAiCodeGeneratorService(0L);
//    }

    private final Cache<Long,AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.info("ai服务实例已删除: {}, value: {}, cause: {}", key, value, cause);
            })
            .build();



    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId,this::createAiCodeGeneratorService);
    }


    public AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        MessageWindowChatMemory chatMemory=MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId,chatMemory,20);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }
}
