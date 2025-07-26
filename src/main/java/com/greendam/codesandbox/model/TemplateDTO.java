package com.greendam.codesandbox.model;

import lombok.Data;

import java.util.List;

/**
 * 模版方法数据传输对象
 */
@Data
public class TemplateDTO {
    private String dockerId;
    private List<ExecuteMessage> executeMessageList;
}
