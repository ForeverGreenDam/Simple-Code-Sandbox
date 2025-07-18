package com.greendam.codesandbox.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 判题信息
 *
 * @author ForeverGreenDam
 */
@Data
public class JudgeInfo implements Serializable {
    /**
     * 内存消耗
     */
    private Integer memory;
    /**
     * 判题结果
     */
    private String message;
    /**
     * 时间消耗
     */
    private Integer time;
}
