package com.yupi.zlcojcodesandbox.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.yupi.zlcojcodesandbox.CodeSandbox;
import com.yupi.zlcojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.zlcojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.zlcojcodesandbox.model.ExecuteMessage;
import com.yupi.zlcojcodesandbox.model.JudeInfo;
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
public class JavaDockerCodeSandboxImpl implements CodeSandbox {
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    public static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    public static final long TIME_OUT = 10000;

    public static final String SECURITY_MANAGER_PATH = "D:\\MyRuanJian\\hm-vue\\xingqiu\\yu-hou\\zlc-oj-code-sandbox\\src\\main\\resources\\security";

    public static final String SECURITY_FILE_NAME = "MySecurityManager";

    public static final Boolean FIRST_INIT = true;


    public static void main(String[] args) {
        JavaDockerCodeSandboxImpl javaNativveCodeSandbox = new JavaDockerCodeSandboxImpl();
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
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();


        //拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像: " + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        System.out.println("下载完成");

        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userParentCodePath, new Volume("/app")));

        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .withCmd("echo", "Hello Docker")
                .exec();

        System.out.println(createContainerResponse);
        //容器id
        String containerId = createContainerResponse.getId();


        //启动容器(根据容器id)
        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(containerId);
        startContainerCmd.exec();

        for (String inputArgs : inputList) {
            String[] inputArgArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main",}, inputArgArray);
            //docker exec
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令: " + execCreateCmdResponse.toString());
            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (streamType.equals(StreamType.STDERR)) {
                        System.out.println("输出错误结果: " + new String(frame.getPayload()));
                    } else {
                        System.out.println("输出结果: " + new String(frame.getPayload()));
                    }

                    super.onNext(frame);
                }
            };
            try {
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }

        }


        //删除容器
//        dockerClient.removeContainerCmd(containerId)
//                .withForce(true)
//                .exec();
//
//        // 删除镜像
//        dockerClient.removeImageCmd(image)
//                .withForce(true)
//                .exec();


        //4. 收集整理输出结果


        //6.

//        return null;
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
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
