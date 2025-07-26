package com.greendam.codesandbox.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.greendam.codesandbox.constant.CodeSandBoxConstant;
import com.greendam.codesandbox.model.*;
import com.greendam.codesandbox.model.enums.JudgeStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Java原生代码沙箱
 *
 * @author ForeverGreenDam
 */
public class JavaDockerCodeSandBox extends JavaCodeSandboxTemplate {
    public static final DockerClient dockerClient= DockerClientBuilder.getInstance().build();
    @Override
    public TemplateDTO runFile(String fileDir, List<String> inputList) {
        try {
            //创建容器
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(CodeSandBoxConstant.JAVA_IMAGE);
            //容器配置
            HostConfig hostConfig = new HostConfig();
            //设置挂载，将fileDir里的内容挂载到容器的/app目录下
            hostConfig.setBinds(new Bind(fileDir, new Volume("/app")));
            //设置容器内存限制，暂定为100MB
            hostConfig.withMemory(100 * 1000 * 1000L);
            CreateContainerResponse containerResponse = containerCmd
                    .withHostConfig(hostConfig)
                    //禁用网络
                    .withNetworkDisabled(true)
                    //允许标准输入输出以及错误输出
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    //可与外界交互
                    .withTty(true)
                    .exec();
            System.out.println(containerResponse);
            String containerId = containerResponse.getId();
            //启动容器
            dockerClient.startContainerCmd(containerId).exec();
            //4. 在docker容器中执行代码，得到输出结果
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for (String inputArgs : inputList) {
                String[] args = inputArgs.split(" ");
                String[] cmdArray = ArrayUtil.append(new String[]{"java", "-Xmx50m", "-Dfile.encoding=UTF-8", "-cp", "/app", "Main"}, args);
                //构建执行命令
                ExecCreateCmdResponse execJavaResponse = dockerClient.execCreateCmd(containerId)
                        .withCmd(cmdArray)
                        .withAttachStdin(true)
                        .withAttachStderr(true)
                        .withAttachStdout(true)
                        .exec();
                System.out.println("创建执行命令：" + execJavaResponse);
                final String[] message = {null};
                //使用StringBuilder来收集错误信息，防止错误信息丢失
                final StringBuilder[] errorMessageBuilder = {new StringBuilder()};
                long time = 0L;
                ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        StreamType streamType = frame.getStreamType();
                        if (StreamType.STDERR.equals(streamType)) {
                            errorMessageBuilder[0].append(new String(frame.getPayload()));
                            System.out.println("输出错误结果：" + errorMessageBuilder[0].toString());
                        } else {
                            String tempStr = new String(frame.getPayload());
                            if (!StrUtil.isBlank(tempStr)) {
                                message[0] = tempStr;
                                System.out.println("输出结果：" + message[0]);
                            }
                        }
                        super.onNext(frame);
                    }
                };
                final long[] maxMemory = {0L};
                // 获取占用的内存
                StatsCmd statsCmd = dockerClient.statsCmd(containerId);
                ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {

                    @Override
                    public void onNext(Statistics statistics) {
                        System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                        maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public void onStart(Closeable closeable) {

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
                statsCmd.exec(statisticsResultCallback);
                //执行
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                boolean timeout = false;
                try {
                    timeout = dockerClient.execStartCmd(execJavaResponse.getId())
                            .exec(execStartResultCallback)
                            .awaitCompletion(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                timeout = !timeout;
                stopWatch.stop();
                time = stopWatch.getTotalTimeMillis();
                statsCmd.close();
                ExecuteMessage executeMessage = new ExecuteMessage();
                executeMessage.setMessage(message[0]);
                executeMessage.setErrorMessage(errorMessageBuilder[0].toString());
                executeMessage.setMemory(maxMemory[0]);
                if (!timeout) {
                    executeMessage.setTime(time);
                } else {
                    executeMessage.setErrorMessage("执行超时，已终止进程");
                }
                if ((!timeout && executeMessage.getErrorMessage().isEmpty())) {
                    executeMessage.setExitValue(0);
                    executeMessageList.add(executeMessage);
                    //清空StringBuilder，方便下次使用
                    errorMessageBuilder[0] = new StringBuilder();
                } else {
                    executeMessage.setExitValue(-1);
                    executeMessageList.add(executeMessage);
                    //快速失败
                    break;
                }
            }
            TemplateDTO result = new TemplateDTO();
            result.setExecuteMessageList(executeMessageList);
            result.setDockerId(containerId);
            return result;
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExecuteCodeResponse output(List<ExecuteMessage> executeMessageList, List<String> inputList) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        JudgeInfo judgeInfo = new JudgeInfo();
        long maxMemory=0L;
        long maxTime=0L;
        List<String> outputList = new ArrayList<>();
        for (ExecuteMessage executeMessage : executeMessageList) {
            if(executeMessage.getExitValue()==0){
                if (!StrUtil.isBlank(executeMessage.getMessage())){
                    outputList.add(executeMessage.getMessage());
                }
                //获取时间占用
                judgeInfo.setTime(Math.toIntExact(maxTime > executeMessage.getTime() ? maxTime : executeMessage.getTime()));
                maxTime=maxTime > executeMessage.getTime() ? maxTime : executeMessage.getTime();
                //获取空间占用
                judgeInfo.setMemory(Math.toIntExact(maxMemory > executeMessage.getMemory() ? maxMemory : executeMessage.getMemory()));
                maxMemory=maxMemory > executeMessage.getMemory() ? maxMemory : executeMessage.getMemory();
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
        return super.executeCode(request);
    }
}
