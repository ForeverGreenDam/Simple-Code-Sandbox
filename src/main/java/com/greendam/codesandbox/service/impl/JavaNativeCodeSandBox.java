package com.greendam.codesandbox.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.dfa.WordTree;
import com.greendam.codesandbox.common.utils.ProcessUtils;
import com.greendam.codesandbox.constant.CodeSandBoxConstant;
import com.greendam.codesandbox.model.*;
import com.greendam.codesandbox.model.enums.JudgeMessageEnum;
import com.greendam.codesandbox.model.enums.JudgeStatusEnum;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Java原生代码沙箱
 *
 * @author ForeverGreenDam
 */
@Component
public class JavaNativeCodeSandBox extends JavaCodeSandboxTemplate {

    private static final List<String> BLACK_LIST = Arrays.asList("Files","try","catch","Process","Tread","Runtime","exec");
    private static final WordTree WORD_TREE ;
    //初始化工作路径以及临时代码路径，初始化黑名单字典树
    static{
        if(!FileUtil.exist(System.getProperty(CodeSandBoxConstant.USER_DIR) + File.separator + CodeSandBoxConstant.TEMP_CODE)){
            FileUtil.mkdir(System.getProperty(CodeSandBoxConstant.USER_DIR) + File.separator + CodeSandBoxConstant.TEMP_CODE);
        }
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(BLACK_LIST);
    }
    @Override
    public TemplateDTO runFile (String fileDir, List<String> inputList) {
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String executeCommand = String.format(CodeSandBoxConstant.JAVA_COMMAND, fileDir, inputArgs);
            Process executeProcess = null;
            try {
                executeProcess = Runtime.getRuntime().exec(executeCommand);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ExecuteMessage javaMessage = ProcessUtils.runProcessAndGetMessage(executeProcess, CodeSandBoxConstant.JAVA);
            executeMessageList.add(javaMessage);
            //快速失败: 最后保存的javaMessage是错误信息
            if(javaMessage.getExitValue()!=0){
                break;
            }
        }
        TemplateDTO result=new TemplateDTO();
        result.setExecuteMessageList(executeMessageList);
        return result;
    }

    @Override
    public ExecuteCodeResponse output(List<ExecuteMessage> executeMessageList,List<String> inputList) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        JudgeInfo judgeInfo = new JudgeInfo();
        long maxTime=0L;
        List<String> outputList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            if(executeMessage.getExitValue()==0){
                outputList.add(executeMessage.getMessage());
                //获取时间占用
                judgeInfo.setTime(Math.toIntExact(maxTime > executeMessage.getTime() ? maxTime : executeMessage.getTime()));
                maxTime=maxTime > executeMessage.getTime() ? maxTime : executeMessage.getTime();
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
        return response;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest request) {
        String code = request.getCode();
        //首先判断用户的代码是否含有敏感信息
        if(WORD_TREE.isMatch(code)){
            return ExecuteCodeResponse.builder()
                    .message(JudgeMessageEnum.DANGEROUS_OPERATION.getValue())
                    .status(JudgeStatusEnum.FAILED.getValue())
                    .build();
        }
        return super.executeCode(request);
    }
}
