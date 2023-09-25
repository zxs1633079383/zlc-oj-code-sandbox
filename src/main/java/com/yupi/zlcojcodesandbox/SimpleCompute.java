package com.yupi.zlcojcodesandbox;

// javac -encoding utf-8 SimpleCompute
// java -cp SimpleCompute 1 2
// 同一类名(Java 为Main)
public class SimpleCompute {
    public static void main(String[] args) {
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        System.out.println("结果:" + (a + b));
    }
}
