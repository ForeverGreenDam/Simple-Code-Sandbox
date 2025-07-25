package com.greendam.codesandbox;

import com.greendam.codesandbox.model.ExecuteCodeRequest;
import com.greendam.codesandbox.model.ExecuteCodeResponse;
import com.greendam.codesandbox.service.impl.JavaDockerCodeSandBox;
import com.greendam.codesandbox.service.impl.JavaNativeCodeSandBox;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class SimpleCodeSandboxApplicationTests {

    /**
     * 测试超时漏洞
     */
    @Test
    void timeOut() {
        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        JavaDockerCodeSandBox javaDockerCodeSandBox=new JavaDockerCodeSandBox();
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        //System.out.println(\"Hello, NewWorld!\");\n" +
                "      int i=1+1;\n" +
                "System.out.println(i);\n" +
                "    }\n" +
                "}";
        List<String> inputList = new ArrayList<>();
        inputList.add("test");
        inputList.add("test2");
        ExecuteCodeRequest java = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language("Java")
                .build();
        ExecuteCodeResponse response = javaDockerCodeSandBox.executeCode(java);
        System.out.println(response.toString());
        System.out.println("ok");
    }

    /**
     * 测试内存溢出漏洞
     */
    @Test
    void OOM() {
        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        JavaDockerCodeSandBox javaDockerCodeSandBox=new JavaDockerCodeSandBox();
        String code = " import java.util.ArrayList;\n" +
                "import java.util.List;\n" +
                " public class Main {\n" +

                "  public static void main(String[] args) throws InterruptedException {\n" +
                " List<byte[]> bytes = new ArrayList<>();\n" +
                "   while (true) {\n" +
                "   bytes.add(new byte[10000]);\n" +
                "   }}}";
        List<String> inputList = new ArrayList<>();
        inputList.add("test");
        ExecuteCodeRequest java = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language("Java")
                .build();
        ExecuteCodeResponse response = javaDockerCodeSandBox.executeCode(java);
        System.out.println(response.toString());
        System.out.println("ok");
    }
    /**
     * 测试读漏洞
     */
    @Test
    void readError() {
        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        JavaDockerCodeSandBox javaDockerCodeSandBox=new JavaDockerCodeSandBox();
        String code = " import java.io.File;\n" +
                "import java.io.IOException;\n" +
                "import java.nio.file.Files;\n" +
                "import java.nio.file.Paths;\n" +
                "import java.util.List;\n" +
                "      public class Main {\n" +
                "  public static void main(String[] args) throws InterruptedException, IOException {\n" +
                "    String userDir = System.getProperty(\"user.dir\");\n" +
                " String filePath = userDir + File.separator + \"src/main/resources/application.yml\";\n" +
                " List<String> allLines = Files.readAllLines(Paths.get(filePath));\n" +
                "  System.out.println(String.join(\"\\n\", allLines));\n" +
                "      }}";
        List<String> inputList = new ArrayList<>();
        inputList.add("test");
        ExecuteCodeRequest java = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language("Java")
                .build();
        ExecuteCodeResponse response = javaDockerCodeSandBox.executeCode(java);
        System.out.println(response.toString());
        System.out.println("ok");
    }
    /**
     * 测试写漏洞
     */
    @Test
    void writeError() {
        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        String code =
                "     import java.io.File;\n" +
                        "import java.io.IOException;\n" +
                        "import java.nio.file.Files;\n" +
                        "import java.nio.file.Paths;\n" +
                        "import java.util.Arrays;\n" +
                        "  public class Main {\n" +
                        " public static void main(String[] args) throws InterruptedException, IOException {\n" +
                        "      String userDir = System.getProperty(\"user.dir\");\n" +
                        "   String filePath = userDir + File.separator + \"src/main/resources/木马程序.bat\";\n" +
                        " String errorProgram = \" java - version 2 > & 1 \";\n" +
                        " Files.write(Paths.get(filePath), Arrays.asList(errorProgram));\n" +
                        " System.out.println(\" 写木马成功，你完了哈哈 \");\n" +
                        "   }}";
        List<String> inputList = new ArrayList<>();
        inputList.add("test");
        ExecuteCodeRequest java = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language("Java")
                .build();
        ExecuteCodeResponse response = javaNativeCodeSandBox.executeCode(java);
        System.out.println(response.toString());
        System.out.println("ok");
    }
    /**
     * 测试运行其他程序漏洞
     */
    @Test
    void runError() {
        JavaNativeCodeSandBox javaNativeCodeSandBox = new JavaNativeCodeSandBox();
        String code =
                " import java.io.BufferedReader;" +
                        "import java.io.File;" +
                        "import java.io.IOException;" +
                        "import java.io.InputStreamReader;" +
                        " public class Main {" +

                        " public static void main(String[] args) throws InterruptedException, IOException {" +
                        "String userDir = System.getProperty(\"user.dir\");" +
                        "String filePath = userDir + File.separator + \"src/main/resources/木马程序.bat\";" +
                        "Process process = Runtime.getRuntime().exec(filePath);" +
                        "process.waitFor();" +
                        " BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));" +
                        " String compileOutputLine;" +
                        "   while ((compileOutputLine = bufferedReader.readLine()) != null) {" +
                        "  System.out.println(compileOutputLine);" +
                        "   }" +
                        "   System.out.println(\"执行异常程序成功\");" +
                        "}}";
        List<String> inputList = new ArrayList<>();
        inputList.add("test");
        ExecuteCodeRequest java = ExecuteCodeRequest.builder()
                .code(code)
                .inputList(inputList)
                .language("Java")
                .build();
        ExecuteCodeResponse response = javaNativeCodeSandBox.executeCode(java);
        System.out.println(response.toString());
        System.out.println("ok");
    }
}
