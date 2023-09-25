package com.yupi.zlcojcodesandbox.utils;

import com.yupi.zlcojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @return
     */
    public static ExecuteMessage runProcesssAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();


        try {
            StopWatch stopWatch = new StopWatch();

            stopWatch.start();
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            if (exitValue == 0) {

                //正常退出
                System.out.println(opName + "成功");
                // 分批获取输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                // 拼接字符串
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                String compilOutputLine;
                // 逐行读取
                while ((compilOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compilOutputLine);
                }
                System.out.println(compileOutputStringBuilder);
                executeMessage.setMessage(compileOutputStringBuilder.toString());

            } else {
                //异常退出
                System.out.println(opName + "失败,错误码: " + exitValue);
                // 分批获取输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                // 拼接字符串
                StringBuilder compileOutputStringBuilder = new StringBuilder();
                String compilOutputLine;
                // 逐行读取
                while ((compilOutputLine = bufferedReader.readLine()) != null) {
                    compileOutputStringBuilder.append(compilOutputLine);
                }


                // 分批获取错误信息
                BufferedReader bufferedErrorReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                String compilErrorOutputLine;
                while ((compilErrorOutputLine = bufferedErrorReader.readLine()) != null) {
                    errorCompileOutputStringBuilder.append(compilErrorOutputLine);
                }
                System.out.println(compileOutputStringBuilder);
                System.out.println("错误信息");
                System.out.println(errorCompileOutputStringBuilder);
                executeMessage.setErrorMessage(errorCompileOutputStringBuilder.toString());

                bufferedErrorReader.close();
                bufferedReader.close();

                runProcess.destroy();
            }

            stopWatch.stop();
            long totalTimeMillis = stopWatch.getLastTaskTimeMillis();
            executeMessage.setTime(totalTimeMillis);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeMessage;

    }
}
