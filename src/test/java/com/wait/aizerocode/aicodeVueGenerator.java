package com.wait.aizerocode;


import com.wait.aizerocode.core.AiCodeGeneratorFacade;
import com.wait.aizerocode.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
public class aicodeVueGenerator {
    @Autowired
    private AiCodeGeneratorFacade  aiCodeGeneratorFacade;

    @Test
    public void generateVueProjectCodeStream() {
        Flux<String> flux = aiCodeGeneratorFacade.generateAndSaveCode("简单的任务记录网站，不超过50行", CodeGenTypeEnum.VUE_PROJECT, 12345678L);

        List<String> block = flux.collectList().block();

        System.out.println(block);
    }


}
