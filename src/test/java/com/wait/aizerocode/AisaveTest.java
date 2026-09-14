package com.wait.aizerocode;


import com.wait.aizerocode.core.AiCodeGeneratorFacade;
import com.wait.aizerocode.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
public class AisaveTest {

    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    public void saveHtml(){
        Flux<String> flux = aiCodeGeneratorFacade.generateAndSaveCode("帮我生成一个登录界面,不超过50行", CodeGenTypeEnum.HTML, 0l);
    }

}
