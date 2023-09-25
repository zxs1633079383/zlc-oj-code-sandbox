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
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Java原始代码 (利用模板方法)
 * Java进程执行管理类 Process
 */
// todo  为每一个测试用例都有一个独立的内存,时间占用的统计
@Component
public class JavaNativveCodeSandbox extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }
}
