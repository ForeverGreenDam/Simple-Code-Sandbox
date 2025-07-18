package com.greendam.codesandbox.service;


import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口
 * @author ForeverGreenDam
 */
public interface CodeSandBox {
    /**
     * 执行代码
     * @param request 执行代码请求
     * @return 执行代码响应
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest request) ;
}
