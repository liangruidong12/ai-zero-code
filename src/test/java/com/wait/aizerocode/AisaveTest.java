package com.wait.aizerocode;


import com.wait.aizerocode.core.AiCodeGeneratorFacade;
import com.wait.aizerocode.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AisaveTest {

    @Autowired
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    public void saveHtml(){
        aiCodeGeneratorFacade.generateAndSaveCode("帮我生成一个登录界面,不超过50行", CodeGenTypeEnum.HTML);
    }

}
