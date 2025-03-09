package com.vivo.applyindepthoptimization;

interface IService {
    // 退出
    void exit();
    // 查看uid
    int getUid();
    // 执行命令
    String runCommand(String command);

    // settings system
    // put
    boolean settingsSystemPutString(String name, String value);
    boolean settingsSystemPutFloat(String name, float value);
    boolean settingsSystemPutInt(String name, int value);
    boolean settingsSystemPutLong(String name, long value);
    // get
    String settingsSystemGetString(String name);
    float settingsSystemGetFloatWithDef(String name, float def);
    float settingsSystemGetFloat(String name);
    int settingsSystemGetIntWithDef(String name, int def);
    int settingsSystemGetInt(String name);
    long settingsSystemGetLongWithDef(String name, long def);
    long settingsSystemGetLong(String name);

    // settings secure
    // put
    boolean settingsSecurePutString(String name, String value);
    boolean settingsSecurePutFloat(String name, float value);
    boolean settingsSecurePutInt(String name, int value);
    boolean settingsSecurePutLong(String name, long value);
    // get
    String settingsSecureGetString(String name);
    float settingsSecureGetFloatWithDef(String name, float def);
    float settingsSecureGetFloat(String name);
    int settingsSecureGetIntWithDef(String name, int def);
    int settingsSecureGetInt(String name);
    long settingsSecureGetLongWithDef(String name, long def);
    long settingsSecureGetLong(String name);

    // settings global
    // put
    boolean settingsGlobalPutString(String name, String value);
    boolean settingsGlobalPutFloat(String name, float value);
    boolean settingsGlobalPutInt(String name, int value);
    boolean settingsGlobalPutLong(String name, long value);
    // get
    String settingsGlobalGetString(String name);
    float settingsGlobalGetFloatWithDef(String name, float def);
    float settingsGlobalGetFloat(String name);
    int settingsGlobalGetIntWithDef(String name, int def);
    int settingsGlobalGetInt(String name);
    long settingsGlobalGetLongWithDef(String name, long def);
    long settingsGlobalGetLong(String name);
}