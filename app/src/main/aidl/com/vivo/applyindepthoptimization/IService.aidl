package com.vivo.applyindepthoptimization;

interface IService {
    // Shizuku必须实现方法
    void destroy() = 16777114;
    void exit() = 1;

    // 查看uid
    int getUid();
    // 执行命令
    String runCommand(String command);
}