package com.greendam.codesandbox.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 执行代码响应
 * @author ForeverGreenDam
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse implements Serializable {
    /**
     * 输出列表
     */
    private List<String> outputList;
    /**
     * 接口信息
     */
    private String message;
    /**
     * 执行状态
     */
    private Integer status;
    /**
     * 执行信息
     */
    private JudgeInfo judgeInfo;
}
