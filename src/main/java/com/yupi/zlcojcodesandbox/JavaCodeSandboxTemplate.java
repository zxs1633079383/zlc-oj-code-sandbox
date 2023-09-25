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
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Java原始模板方法
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 10000;

//    public static final String SECURITY_MANAGER_PATH = "D:\\MyRuanJian\\hm-vue\\xingqiu\\yu-hou\\zlc-oj-code-sandbox\\src\\main\\resources\\security";

//    public static final String SECURITY_FILE_NAME = "MySecurityManager";

//    private static final List<String> blackList = Arrays.asList("Files", "exec");


    /**
     * 1. 用户代码保存为文件
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCoedPathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        if (!FileUtil.exist(globalCoedPathName)) {
            FileUtil.mkdir(globalCoedPathName);
        }

        // 用户的代码 隔离存放
        String userParentCodePath = globalCoedPathName + File.separator + UUID.randomUUID();
        String userCodePah = userParentCodePath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePah, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2.编译用户代码文件
     *
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process process = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtils.runProcesssAndGetMessage(process, "编译");
            System.out.println(executeMessage);
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;

        } catch (IOException e) {
            //
//            return getResponse(e);
            throw new RuntimeException(e);

        }
    }

    // 3. 执行程序 返回结果列表
    public List<ExecuteMessage> runFile(File compiledFile, List<String> inputList) {
        String userParentCodePath = compiledFile.getParentFile().getAbsolutePath();

        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();

            String runCmd = String.format("java -Xmx256m -Dfile.encoding=utf-8 -cp %s Main %s", userParentCodePath, inputArgs);
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=utf-8 -cp %s   -Djava.security.manager=%s Main %s", userParentCodePath,SECURITY_MANAGER_PATH,SECURITY_FILE_NAME, inputArgs);

            try {

                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 类似于守护线程,超时了就杀掉(超时控制)
                // todo 如果任务真的需要超时? 直接杀掉好吗. (自我感觉可以查询提交id的状态, 然后走逻辑)
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(TIME_OUT);
//                        System.out.println("超时了, 中断");
//                        runProcess.destroy();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcesssAndGetMessage(runProcess, "运行");
                executeMessageList.add(executeMessage);
                System.out.println(executeMessage);
            } catch (IOException e) {
//                return getResponse(e);
                throw new RuntimeException("程序执行异常", e);
            }
        }
        return executeMessageList;
    }

    /**
     * //4. 收集整理输出结果
     *
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOut(List<ExecuteMessage> executeMessageList) {
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

        return executeCodeResponse;
    }

    //5. 删除所属文件
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userParentCodePath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userParentCodePath);
            System.out.println("是否临时文件清理成功: " + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {

        //设置指定的Java 安全管理器
//        System.setSecurityManager(new DenySecurityManager());

        //todo  judeinfo的Message,memory未设置.响应的总信息未设置
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        String userDir = System.getProperty("user.dir");
        String globalCoedPathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        String userParentCodePath = globalCoedPathName + File.separator + UUID.randomUUID();


        //校验黑名单代码..


        //1. 查询文件存放位置是否存在 不存在就新创建
        File file = saveCodeToFile(code);

        //2. 编译代码,得到class文件
        ExecuteMessage executeMessage1 = compileFile(file);
        System.out.println(executeMessage1);


        //3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(file, inputList);

        //4. 收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = getOut(executeMessageList);


        //5. 文件清理
        boolean flag = deleteFile(file);
        if (!flag) {
            log.error("deleteFile error,userCodeFilePath = {}", file.getAbsolutePath());
        }


        //6. 运行到最后 他的state也是1
        log.info("沙箱返回响应信息:{}", executeCodeResponse);
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
