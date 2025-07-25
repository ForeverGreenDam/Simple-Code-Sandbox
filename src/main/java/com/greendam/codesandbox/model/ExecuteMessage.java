package com.greendam.codesandbox.model;

import lombok.Data;

/**
 * 执行信息
 * @author ForeverGreenDam
 */
@Data
public class ExecuteMessage {
    private int exitValue;
    private String message;
    private String errorMessage;
    private Long time;
    private Long memory;
}
