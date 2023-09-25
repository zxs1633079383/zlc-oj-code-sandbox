package com.yupi.zlcojcodesandbox;


import com.yupi.zlcojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.zlcojcodesandbox.model.ExecuteCodeResponse;

public interface CodeSandbox {

    //todo 提供可提供查看代码沙箱状态的接口。。

    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);

}
