package com.wait.aizerocode;



import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.wait.aizerocode.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiZeroCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiZeroCodeApplication.class, args);
    }



}
