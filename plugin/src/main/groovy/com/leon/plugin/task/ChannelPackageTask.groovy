package com.leon.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.process.ExecResult

/**
 * Created by leontli on 17/2/19.
 */
class ChannelPackageTask extends DefaultTask {
    public static final int DEFAULT_MODE = -1;
    public static final int V1_MODE = 1;
    public static final int V2_MODE = 2;

    @Input
    public List<String> mChannelList;

    /**
     * verify apk by apksigner.jar
     * @param apkPath
     * @return
     */
    boolean verifyV2Signature(String apkPath) {
        def apktoolCmd = []
        apktoolCmd.add("java")
        apktoolCmd.add("-jar")
        apktoolCmd.add("$project.projectDir/tools/apksigner.jar")
        apktoolCmd.add("verify")
        apktoolCmd.add("-v")
        apktoolCmd.add("$apkPath")
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        println "apk cmd verify : " + apktoolCmd
        ExecResult result = project.exec {
            standardOutput = arrayOutputStream
            commandLine apktoolCmd
        }

        String str = arrayOutputStream.toString("utf-8");
        boolean v2VerifySuccess = false
        str.eachLine { String value ->
            println "value = ${value}"
            if (value.contains("APK Signature Scheme v2") && value.trim().endsWith("true")) {
                v2VerifySuccess = true
            }
        }

        return result.exitValue == 0 && v2VerifySuccess
    }

    void copyTo(File src, File dest) {
        def input = src.newInputStream()
        def output = dest.newOutputStream()
        output << input
        input.close()
        output.close()
    }
}
