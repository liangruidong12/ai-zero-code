package com.wait.aizerocode.ai;



import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.wait.aizerocode.ai.tools.*;
import com.wait.aizerocode.model.enums.CodeGenTypeEnum;
import com.wait.aizerocode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private ChatHistoryService  chatHistoryService;

    @Autowired
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private ToolManager toolManager;



//    @Bean
//    public AiCodeGeneratorService aiCodeGeneratorService() {
//        return getAiCodeGeneratorService(0L);
//    }

    private final Cache<String,AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .expireAfterWrite(Duration.ofMinutes(30))
            .removalListener((key, value, cause) -> {
                log.info("ai服务实例已删除: {}, value: {}, cause: {}", key, value, cause);
            })
            .build();





    public AiCodeGeneratorService getAiCodeGeneratorService(long appId,CodeGenTypeEnum codeGenTypeEnum) {
        String cacheKey=appId+"_"+codeGenTypeEnum.getValue();
        return serviceCache.get(cacheKey,key ->createAiCodeGeneratorService(appId,codeGenTypeEnum));
    }


    public AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum  codeGenType) {
        //根据appId构建独立的回话记忆
        MessageWindowChatMemory chatMemory=MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        //导入回话记忆到缓存
        chatHistoryService.loadChatHistoryToMemory(appId,chatMemory,20);
        return switch (codeGenType) {
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(toolManager.getAllTools())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest,
                            "Error,there is no tool called "+toolExecutionRequest.name()))
                    .build();

            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
        };
    }
}
