# Simple-Code-Sandbox
一个简单的代码沙箱实现，目前暂时支持java，后续语言支持更新中。。。
## 使用方式
使用Post请求[forevergreendam.cn/runcode](http://forevergreendam.cn/runcode)传入JSON数据，数据格式如下
```json
{
  "inputList": ["1","2"],
  "language": "java",
  "code": "public class Main {\n public static void main(String[] args) {\n    System.out.println(\"Hello,NewWorld!\");\n  }\n};"
}
```
- language目前必须填java；
- inputList填写传入参数，每个元素代表一组测试用例，如果程序运行需要两个参数（比如A+B问题）,则需要在两个参数之间加入空格（eg:"inputList": ["1 2","3 4"]代表执行这个程序两次，第一次传入参数1和2，第二次传入参数3和4）
- code需要传入完整的java代码，包括import语句；类名必须是Main，接收参数的形式必须是args[i]，输出的形式必须是 System.out.println();
## 返回格式
```json
{
    "outputList": [
        "Hello,NewWorld!",
        "Hello,NewWorld!"
    ],
    "message": null,
    "status": 2,
    "judgeInfo": {
        "memory": 1789952,
        "message": null,
        "time": 303
    }
}
```
- outputList返回System.out.println();的输出结果，长度和inputList相同（没有错误的前提下），如果程序运行过程中出错，则仅包含出错用例之前的所有执行结果
- message返回错误信息，如果正常运行会返回null，编译或运行报错会返回错误信息
- status返回执行状态，2代表成功执行，3代表执行失败
- judgeinfo返回执行内存和时间消耗，memory返回最大消耗内存，单位为b，time返回最长执行时间，单位为ms，message暂不返回任何消息，为null
  
