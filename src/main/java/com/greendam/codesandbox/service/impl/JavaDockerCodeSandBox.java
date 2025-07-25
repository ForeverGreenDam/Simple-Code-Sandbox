package com.greendam.codesandbox.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
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
import com.greendam.codesandbox.common.utils.ProcessUtils;
import com.greendam.codesandbox.constant.CodeSandBoxConstant;
import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;
import com.greendam.codesandbox.model.ExecuteMessage;
import com.greendam.codesandbox.model.JudgeInfo;
import com.greendam.codesandbox.model.enums.JudgeStatusEnum;
import com.greendam.codesandbox.service.CodeSandBox;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Java原生代码沙箱
 *
 * @author ForeverGreenDam
 */
public class JavaDockerCodeSandBox implements CodeSandBox {
    private static final String ROOT_PATH;
    private static final String TEMP_CODE_PATH;
    public static final  DockerClient dockerClient = DockerClientBuilder.getInstance().build();
    //初始化工作路径以及临时代码路径，初始化黑名单字典树
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
        String containerId = null;
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
            //3. 将编译好的文件上传到docker环境中
            //创建容器
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(CodeSandBoxConstant.JAVA_IMAGE);
            //容器配置
            HostConfig hostConfig=new HostConfig();
            //设置挂载，将fileDir里的内容挂载到容器的/app目录下
            hostConfig.setBinds(new Bind(fileDir,new Volume("/app")));
            //设置容器内存限制，暂定为100MB
            hostConfig.withMemory(100*1000*1000L);
            CreateContainerResponse containerResponse =containerCmd
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
            containerId = containerResponse.getId();
            //启动容器
            dockerClient.startContainerCmd(containerId).exec();
            //4. 在docker容器中执行代码，得到输出结果
            List<ExecuteMessage> executeMessageList = new ArrayList<>();
            for (String inputArgs : inputList) {
                String[] args = inputArgs.split(" ");
                String[] cmdArray= ArrayUtil.append(new String[]{"java","-Xmx50m","-Dfile.encoding=UTF-8","-cp","/app","Main"},args);
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
                final StringBuilder[] errorMessageBuilder={new StringBuilder()};
                long time = 0L;
                String execId = execJavaResponse.getId();
                ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                    @Override
                    public void onNext(Frame frame) {
                        StreamType streamType = frame.getStreamType();
                        if (StreamType.STDERR.equals(streamType)) {
                            errorMessageBuilder[0].append(new String(frame.getPayload()));
                            System.out.println("输出错误结果：" + errorMessageBuilder[0].toString());
                        } else {
                            String tempStr= new String(frame.getPayload());
                            if(!StrUtil.isBlank(tempStr)){
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
                boolean timeout = dockerClient.execStartCmd(execJavaResponse.getId())
                        .exec(execStartResultCallback)
                        .awaitCompletion(15, TimeUnit.SECONDS);
                timeout=!timeout;
                stopWatch.stop();
                time = stopWatch.getTotalTimeMillis();
                statsCmd.close();
                ExecuteMessage executeMessage = new ExecuteMessage();
                executeMessage.setMessage(message[0]);
                executeMessage.setErrorMessage(errorMessageBuilder[0].toString());
                executeMessage.setMemory(maxMemory[0]);
                if(!timeout){
                    executeMessage.setTime(time);
                }else{
                    executeMessage.setErrorMessage("执行超时，已终止进程");
                }
                if((!timeout&& executeMessage.getErrorMessage().isEmpty())){
                    executeMessage.setExitValue(0);
                    executeMessageList.add(executeMessage);
                    //清空StringBuilder，方便下次使用
                    errorMessageBuilder[0]=new StringBuilder();
                }else{
                    executeMessage.setExitValue(-1);
                    executeMessageList.add(executeMessage);
                    //快速失败
                    break;
                }
            }
            //5. 收集整理输出结果返回
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
                    //获取空间占用
                    judgeInfo.setMemory(Math.toIntExact(maxMemory > executeMessage.getMemory() ? maxMemory : executeMessage.getMemory()));
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
            //6. 删除文件以及容器
            if(FileUtil.exist(fileDir)){
                boolean del = FileUtil.del(fileDir);
                System.out.println("删除临时文件 "+fileDir +(del ? "成功" : "失败"));
            }
            if (containerId != null) {
                    dockerClient.killContainerCmd(containerId).exec();
                }
        }
        //7. 异常处理（写在catch中）
        return response;
    }
}
