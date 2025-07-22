package com.greendam.codesandbox.controller;

import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;
import com.greendam.codesandbox.service.impl.JavaNativeCodeSandBox;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TestController {
    @GetMapping("/helloworld")
    public ExecuteCodeResponse helloWorld() {
        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, NewWorld!\");\n" +
                "        System.out.println(1/0);\n" +
                "    }\n" +
                "}";
        List<String> inputList = new ArrayList<>();
        inputList.add("test");
        ExecuteCodeRequest java = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language("Java")
                .build();
        ExecuteCodeResponse response = javaNativeCodeSandBox.executeCode(java);
        System.out.println(response.toString());
        System.out.println("ok");
        return response;
    }
}
