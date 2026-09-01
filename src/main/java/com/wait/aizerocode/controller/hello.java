package com.wait.aizerocode.controller;


import com.wait.aizerocode.common.BaseResponse;
import com.wait.aizerocode.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
@Tag(name="test")
public class hello {

    @GetMapping
    @Operation(summary = "sayhello")
    public BaseResponse<String> sayHello(){
        return ResultUtils.success("hello");
    }

}
