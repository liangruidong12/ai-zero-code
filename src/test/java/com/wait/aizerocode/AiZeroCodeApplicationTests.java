package com.wait.aizerocode;

import com.wait.aizerocode.ai.AiCodeGeneratorService;
import com.wait.aizerocode.ai.model.HtmlCodeResult;
import com.wait.aizerocode.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiZeroCodeApplicationTests {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateMultiFileCode() {

        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("帮我生成一个游戏战绩查询的页面,不超过100行");
        if(result!=null){
            System.out.println(result);
        }

    }

    @Test
    void generateHtmlCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("帮我生成一个游戏战绩查询的页面,不超过200行");
        if(result!=null){
            System.out.println(result);
        }
    }


}
