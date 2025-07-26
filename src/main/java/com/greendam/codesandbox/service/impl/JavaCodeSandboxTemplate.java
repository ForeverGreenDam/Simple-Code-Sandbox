package com.greendam.codesandbox.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.greendam.codesandbox.common.utils.ProcessUtils;
import com.greendam.codesandbox.constant.CodeSandBoxConstant;
import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;
import com.greendam.codesandbox.model.ExecuteMessage;
import com.greendam.codesandbox.model.TemplateDTO;
import com.greendam.codesandbox.model.enums.JudgeStatusEnum;
import com.greendam.codesandbox.service.CodeSandBox;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Java代码沙箱模板类
 *
 * @author ForeverGreenDam
 */
public abstract class   JavaCodeSandboxTemplate implements CodeSandBox {
    public static final DockerClient dockerClient= DockerClientBuilder.getInstance().build();
    public String saveCodeAsFile(String code){
        String fileDir = System.getProperty(CodeSandBoxConstant.USER_DIR) + File.separator + CodeSandBoxConstant.TEMP_CODE+ File.separator + UUID.randomUUID();
        String fileName = fileDir + File.separator + CodeSandBoxConstant.JAVA_FILE_NAME;
        FileUtil.writeString(code,fileName, StandardCharsets.UTF_8);
        return fileDir;
    }
    public ExecuteMessage compileFile(String fileName){
        String compileCommand = String.format(CodeSandBoxConstant.JAVAC_COMMAND, fileName);
        Process compileProcess = null;
        try {
            compileProcess = Runtime.getRuntime().exec(compileCommand);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ExecuteMessage javacMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, CodeSandBoxConstant.JAVAC);
        return javacMessage;
    }
    public abstract TemplateDTO runFile(String fileDir, List<String> inputList);
    public abstract ExecuteCodeResponse output(List<ExecuteMessage> executeMessageList,List<String> inputList);
    public  void freeResource(String fileDir,String dockerId){
        if(FileUtil.exist(fileDir)){
            boolean del = FileUtil.del(fileDir);
            System.out.println("删除临时文件 "+fileDir +(del ? "成功" : "失败"));
        }
        if (dockerId != null) {
            dockerClient.killContainerCmd(dockerId).exec();
        }
    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {

           String code = request.getCode();
           List<String> inputList = request.getInputList();
           String language = request.getLanguage();
           ExecuteCodeResponse response= null;
            String fileDir =null;
            String dockerId = null;
        try {
           //1. 将用户的代码保存为文件
            fileDir = saveCodeAsFile(code);
           String fileName = fileDir + File.separator + CodeSandBoxConstant.JAVA_FILE_NAME;
           //2.编译代码，得到class文件
           ExecuteMessage javacMessage = compileFile(fileName);
           if (javacMessage.getExitValue() != 0) {
               // 编译失败，返回错误信息
               return ExecuteCodeResponse.builder()
                       .message(javacMessage.getErrorMessage())
                       .status(JudgeStatusEnum.FAILED.getValue())
                       .build();
           }
           //3.执行程序
           TemplateDTO dto = runFile(fileDir, inputList);
           dockerId= dto.getDockerId();
           //4. 处理输出结果
           response= output(dto.getExecuteMessageList(),inputList);
       }catch (Exception e){
            return ExecuteCodeResponse.builder()
                    .message(e.getMessage())
                    .status(JudgeStatusEnum.FAILED.getValue())
                    .build();
        }
       finally {
           freeResource(fileDir,dockerId);
       }
        return response;
    }
}
