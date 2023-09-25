package com.yupi.zlcojcodesandbox.security;

import java.security.Permission;

/**
 * Java 安全管理器 (禁止所有权限的)
 */
public class MySecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        // 默认super 即禁用所有的权限
//        throw new SecurityException("权限异常: " + perm.getActions() + " " + perm.toString());
    }


}
