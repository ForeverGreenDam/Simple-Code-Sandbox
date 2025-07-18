package com.greendam.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 执行代码请求
 * @author ForeverGreenDam
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 代码内容
     */
    private String code;
    /**
     * 输入列表
     */
    private List<String> inputList;
    /**
     * 语言类型
     */
    private String language;
}
