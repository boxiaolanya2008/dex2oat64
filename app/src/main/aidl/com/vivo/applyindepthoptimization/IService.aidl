package com.vivo.applyindepthoptimization;

interface IService {
    // 退出
    void exit();
    // 查看uid
    int getUid();
    // 执行命令
    String runCommand(String command);
}