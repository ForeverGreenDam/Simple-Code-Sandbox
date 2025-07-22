package com.greendam.codesandbox.constant;

/**
 * 代码沙箱常量接口
 * @author ForeverGreenDam
 */
public interface CodeSandBoxConstant {
    /**
     * 代码沙箱的临时代码存放目录名
     */
    String TEMP_CODE = "tempcode";
    /**
     * 用于获取当前用户工作目录路径
     */
    String USER_DIR= "user.dir";
    /**
     * Java临时代码文件名
     */
    String JAVA_FILE_NAME = "Main.java";
    /**
     * Java编译命令
     */
    String JAVAC_COMMAND = "javac -encoding utf-8 %s";
    /**
     * Java编译操作
     */
    String JAVAC = "编译";
    /**
     * Java执行命令
     */
    String JAVA_COMMAND = "java -Dfile.encoding=UTF-8 -cp %s Main %s";
    /**
     * Java执行操作
     */
    String JAVA = "执行";
    /**
     * 失败状态码
     */
    Integer FAIL = 3;
    /**
     * 成功状态码
     */
    Integer SUCCESS = 2;
}
