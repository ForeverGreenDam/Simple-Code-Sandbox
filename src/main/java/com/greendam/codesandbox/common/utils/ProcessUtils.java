package com.greendam.codesandbox.common.utils;

import com.greendam.codesandbox.constant.CodeSandBoxConstant;
import com.greendam.codesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 工具类：处理进程相关的操作
 * @author ForeverGreenDam
 */
public class ProcessUtils {
    /**
     * 默认超时时间，10秒
     */
    public static final long DEFAULT_TIMEOUT = 10 * 1000;
    /**
         * 执行进程并获取信息
         *
         * @param runProcess 要执行的进程
         * @param opName 操作名称，用于日志输出
         * @return ExecuteMessage 包含执行结果和输出信息
         */
        public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
            ExecuteMessage executeMessage = new ExecuteMessage();


            try {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                // 等待程序执行，获取错误码
                boolean timeout = runProcess.waitFor(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
                if (!timeout) {
                    // 超时处理, 防止进程一直占用资源
                    runProcess.destroy();
                    executeMessage.setExitValue(-1);
                    executeMessage.setErrorMessage("执行超时，已终止进程");
                    System.out.println(opName + "超时，已终止进程");
                    return executeMessage;
                }
                int exitValue = runProcess.waitFor();
                executeMessage.setExitValue(exitValue);
                // 正常退出
                if (exitValue == 0) {
                    System.out.println(opName + "成功");
                    // 分批获取进程的正常输出
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                    StringBuilder compileOutputStringBuilder = new StringBuilder();
                    // 逐行读取
                    String compileOutputLine;
                    while ((compileOutputLine = bufferedReader.readLine()) != null) {
                        compileOutputStringBuilder.append(compileOutputLine);
                    }
                    executeMessage.setMessage(compileOutputStringBuilder.toString());
                    bufferedReader.close();
                } else {
                    // 异常退出
                    System.out.println(opName + "失败，错误码： " + exitValue);
                    // 分批获取进程的正常输出
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                    StringBuilder compileOutputStringBuilder = new StringBuilder();
                    // 逐行读取
                    String compileOutputLine;
                    while ((compileOutputLine = bufferedReader.readLine()) != null) {
                        compileOutputStringBuilder.append(compileOutputLine);
                    }
                    executeMessage.setMessage(compileOutputStringBuilder.toString());
                    bufferedReader.close();
                    // 分批获取进程的错误输出
                    BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                    StringBuilder errorCompileOutputStringBuilder = new StringBuilder();

                    // 逐行读取
                    String errorCompileOutputLine;
                    while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                        errorCompileOutputStringBuilder.append(errorCompileOutputLine);
                    }
                    //中国区的编译报错信息始终是GBK编码，我们需要转为UTF-8编码
                    String errorOutput = errorCompileOutputStringBuilder.toString();
                    if(CodeSandBoxConstant.JAVAC.equals(opName)){
                        byte[] bytes = errorOutput.getBytes("CP936");
                        errorOutput = new String(bytes, StandardCharsets.UTF_8);
                    }
                    executeMessage.setErrorMessage(errorOutput);
                    errorBufferedReader.close();
                }
                stopWatch.stop();
                executeMessage.setTime(stopWatch.getTotalTimeMillis());
            } catch (Exception e) {
                return executeMessage;
            }
            return executeMessage;
        }
    }


