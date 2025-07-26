package com.greendam.codesandbox.controller;

import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;
import com.greendam.codesandbox.service.impl.JavaDockerCodeSandBox;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
public class CodeController {
    @Resource
    private JavaDockerCodeSandBox javaDockerCodeSandBox;
    @GetMapping("/runcode")
    public ExecuteCodeResponse helloWorld(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        return javaDockerCodeSandBox.executeCode(executeCodeRequest);
    }
}
