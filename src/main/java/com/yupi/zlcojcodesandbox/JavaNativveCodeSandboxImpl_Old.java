package com.yupi.zlcojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.yupi.zlcojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.zlcojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.zlcojcodesandbox.model.ExecuteMessage;
import com.yupi.zlcojcodesandbox.model.JudeInfo;
import com.yupi.zlcojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Java原始代码
 * Java进程执行管理类 Process
 */
// todo  为每一个测试用例都有一个独立的内存,时间占用的统计
public class JavaNativveCodeSandboxImpl_Old implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 10000;

    public static final String SECURITY_MANAGER_PATH = "D:\\MyRuanJian\\hm-vue\\xingqiu\\yu-hou\\zlc-oj-code-sandbox\\src\\main\\resources\\security";

    public static final String SECURITY_FILE_NAME = "MySecurityManager";

    // hutool的字典树 用以检索 目的字符串中,是否存有指定字符串.
    // todo 无法遍历所有的黑名单. 且不同的编程语言,对应的领域,关键词不一样
    private static final WordTree wordTree;

    private static final List<String> blackList = Arrays.asList("Files", "exec");

    static {
        wordTree = new WordTree();
        wordTree.addWords(blackList);
    }


    public static void main(String[] args) {
        JavaNativveCodeSandboxImpl_Old javaNativveCodeSandbox = new JavaNativveCodeSandboxImpl_Old();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        String code = ResourceUtil.readStr("testCode.simpleComputeArgs/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("java");
        //执行测试
        ExecuteCodeResponse executeCodeResponse = javaNativveCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        //设置指定的Java 安全管理器
//        System.setSecurityManager(new DenySecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //校验黑名单代码..

//        FoundWord foundWord = wordTree.matchWord(code);
//        if (foundWord != null) {
//            // 找到了 敏感词汇, 跳出.
//            System.out.println("敏感词:" + foundWord.getFoundWord());
//            return null;
//        }


        //查询文件存放位置是否存在 不存在就新创建
        String userDir = System.getProperty("user.dir");
        String globalCoedPathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        if (!FileUtil.exist(globalCoedPathName)) {
            FileUtil.mkdir(globalCoedPathName);


        }

        // 用户的代码 隔离存放
        String userParentCodePath = globalCoedPathName + File.separator + UUID.randomUUID();
        String userCodePah = userParentCodePath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePah, StandardCharsets.UTF_8);

        //2. 编译代码,得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process process = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcesssAndGetMessage(process, "编译");
            System.out.println(executeMessage);

        } catch (IOException e) {
            return getResponse(e);
//            throw new RuntimeException(e);

        }

        //3. 执行代码，得到输出结果

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();

            String runCmd = String.format("java -Xmx256m -Dfile.encoding=utf-8 -cp %s Main %s", userParentCodePath, inputArgs);
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=utf-8 -cp %s;%s  -Djava.security.manager=%s Main %s", userParentCodePath,SECURITY_MANAGER_PATH,SECURITY_FILE_NAME, inputArgs);

            try {

                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 类似于守护线程,超时了就杀掉(超时控制)
                // todo 如果任务真的需要超时? 直接杀掉好吗. (自我感觉可以查询提交id的状态, 然后走逻辑)
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了, 中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcesssAndGetMessage(runProcess, "运行");
                executeMessageList.add(executeMessage);
                System.out.println(executeMessage);

            } catch (IOException e) {
                return getResponse(e);
//                throw new RuntimeException(e);
            }
        }

        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (executeMessage.getTime() != null) {
                maxTime = Math.max(maxTime, executeMessage.getTime());
            }
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);

                //todo 是否add

                break;
            }
            outputList.add(executeMessage.getMessage());

        }
        //设置返回信息
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);

        JudeInfo judeInfo = new JudeInfo();
        //从process 的java进程管理类拿到内存消耗要借助第三方库
//        judeInfo.setMemory();
        judeInfo.setTime(maxTime);

        executeCodeResponse.setJudeInfo(judeInfo);

        //5. 文件清理
        if (userCodeFile.getParentFile() != null) {
            boolean del = FileUtil.del(userParentCodePath);
            System.out.println("是否临时文件清理成功: " + (del ? "成功" : "失败"));
        }


        //6.

        return executeCodeResponse;
    }

    //6. 获取错误响应
    private ExecuteCodeResponse getResponse(Throwable e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 2.表示代码沙箱错误,3表示用户代码错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudeInfo(new JudeInfo());
        return executeCodeResponse;
    }
}
