package com.greendam.codesandbox.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.greendam.codesandbox.common.utils.ProcessUtils;
import com.greendam.codesandbox.constant.CodeSandBoxConstant;
import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;
import com.greendam.codesandbox.model.ExecuteMessage;
import com.greendam.codesandbox.model.JudgeInfo;
import com.greendam.codesandbox.model.enums.JudgeStatusEnum;
import com.greendam.codesandbox.service.CodeSandBox;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Java原生代码沙箱
 *
 * @author ForeverGreenDam
 */
public class JavaNativeCodeSandBox implements CodeSandBox {
    private static final String ROOT_PATH;
    private static final String TEMP_CODE_PATH;
    //初始化工作路径以及临时代码路径
    static{
        ROOT_PATH = System.getProperty(CodeSandBoxConstant.USER_DIR);
        TEMP_CODE_PATH = ROOT_PATH + File.separator + CodeSandBoxConstant.TEMP_CODE;
        if(!FileUtil.exist(TEMP_CODE_PATH)){
            FileUtil.mkdir(TEMP_CODE_PATH);
        }
    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        String code = request.getCode();
        List<String> inputList = request.getInputList();
        String language = request.getLanguage();
        ExecuteCodeResponse response = new ExecuteCodeResponse();

        //1. 将用户的代码保存为文件
        String fileDir = TEMP_CODE_PATH + File.separator + UUID.randomUUID();
        String fileName = fileDir + File.separator +CodeSandBoxConstant.JAVA_FILE_NAME;
        FileUtil.writeString(code,fileName, StandardCharsets.UTF_8);

        //2.编译代码，得到class文件
        String compileCommand = String.format(CodeSandBoxConstant.JAVAC_COMMAND, fileName);
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCommand);
            ExecuteMessage javacMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, CodeSandBoxConstant.JAVAC);
            if(javacMessage.getExitValue()!=0){
                // 编译失败，返回错误信息
                return ExecuteCodeResponse.builder()
                        .message(javacMessage.getErrorMessage())
                        .status(JudgeStatusEnum.FAILED.getValue())
                        .build();
            }
            //3. 执行程序，获取输出结果
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for (String inputArgs : inputList) {
                String executeCommand = String.format(CodeSandBoxConstant.JAVA_COMMAND, fileDir, inputArgs);
                    Process executeProcess = Runtime.getRuntime().exec(executeCommand);
                    ExecuteMessage javaMessage = ProcessUtils.runProcessAndGetMessage(executeProcess, CodeSandBoxConstant.JAVA);
                    executeMessageList.add(javaMessage);
                //快速失败: 最后保存的javaMessage是错误信息
                if(javaMessage.getExitValue()!=0){
                    break;
                }
            }
            //4. 处理输出结果
            JudgeInfo judgeInfo = new JudgeInfo();
            long maxTime=0L;
        List<String> outputList = new ArrayList<>();
            for (ExecuteMessage executeMessage : executeMessageList) {
                if(executeMessage.getExitValue()==0){
                    outputList.add(executeMessage.getMessage());
                    //获取时间占用
                    judgeInfo.setTime(Math.toIntExact(maxTime > executeMessage.getTime() ? maxTime : executeMessage.getTime()));
                }
            }
            //运行时正常
            if(outputList.size()==inputList.size()){
                response.setOutputList(outputList);
                response.setStatus(JudgeStatusEnum.SUCCEED.getValue());
                response.setJudgeInfo(judgeInfo);
            }else {
                //运行时异常
                response.setOutputList(outputList);
                response.setStatus(JudgeStatusEnum.FAILED.getValue());
                response.setMessage(executeMessageList.get(executeMessageList.size()-1).getErrorMessage());
            }
        }
        catch (Exception e) {
            return ExecuteCodeResponse.builder()
                    .message(e.getMessage())
                    .status(JudgeStatusEnum.FAILED.getValue())
                    .build();
        }finally{
            //5. 删除文件
            if(FileUtil.exist(fileDir)){
                boolean del = FileUtil.del(fileDir);
                System.out.println("删除临时文件 "+fileDir +(del ? "成功" : "失败"));
            }
        }
        //6. 异常处理（写在catch中）
        return response;
    }
}
