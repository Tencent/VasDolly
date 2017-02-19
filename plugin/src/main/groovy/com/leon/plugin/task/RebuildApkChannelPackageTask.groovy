package com.leon.plugin.task

import com.leon.channel.common.ApkSectionInfo
import com.leon.channel.common.V1SchemeUtil
import com.leon.channel.common.V2SchemeUtil
import com.leon.channel.reader.ChannelReader
import com.leon.channel.writer.ChannelWriter;
import com.leon.plugin.extension.RebuildChannelConfigurationExtension
import com.leon.plugin.verifier.VerifyApk;
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Created by leontli on 17/2/18.
 */

public class RebuildApkChannelPackageTask extends ChannelPackageTask {

    @Input
    RebuildChannelConfigurationExtension mRebuildChannelExtension;

    RebuildApkChannelPackageTask() {
        group = 'channel'
    }

    @TaskAction
    public void channel() {
        //1.check all params
        checkParameter();

        if (mRebuildChannelExtension.isNeedRebuildDebugApk()) {
            generateChannelApk(mRebuildChannelExtension.baseDebugApk, mRebuildChannelExtension.debugOutputDir)
        }

        if (mRebuildChannelExtension.isNeedRebuildReleaseApk()) {
            generateChannelApk(mRebuildChannelExtension.baseReleaseApk, mRebuildChannelExtension.releaseOutputDir)
        }
    }

    void generateChannelApk(File baseApk, File outputDir) {
        int mode = judgeChannelPackageMode(baseApk)
        println("${baseApk} , ChannelPackageMode = ${mode == ChannelPackageTask.V2_MODE ? "V2 Mode" : "V1 Mode"}")

        if (mode == ChannelPackageTask.V1_MODE) {
            generateV1ChannelApk(baseApk, outputDir)
        } else if (mode == ChannelPackageTask.V2_MODE) {
            generateV2ChannelApk(baseApk, outputDir)
        } else {
            throw new GradleException("not have precise channel package mode");
        }
    }

    int judgeChannelPackageMode(File baseApk) {
        if (V2SchemeUtil.containV2Signature(baseApk)) {
            return ChannelPackageTask.V2_MODE
        } else if (V1SchemeUtil.containV1Signature(baseApk)) {
            return ChannelPackageTask.V1_MODE
        } else {
            return ChannelPackageTask.DEFAULT_MODE
        }
    }

    void generateV1ChannelApk(File baseApk, File outputDir) {
        //check v1 signature , if not have v1 signature , you can't install Apk below 7.0
        println("------ Task ${name} generate v1 channel apk  , begin ------")

        if (!V1SchemeUtil.containV1Signature(baseApk)) {
            throw new GradleException("Task ${name} " +
                    "apk ${apkPath} not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0")
        }
        String baseReleaseApkName = baseApk.name;
        mChannelList.each { channel ->
            String apkChannelName = getChannelApkName(baseReleaseApkName, channel)
            println "generateV1ChannelApk , channel = ${channel} , apkChannelName = ${apkChannelName}"
            File destFile = new File(outputDir, apkChannelName)
            copyTo(baseApk, destFile)
            V1SchemeUtil.writeChannel(destFile, channel)
            //verify channel info
            if (V1SchemeUtil.verifyChannel(destFile, channel)) {
                println("generateV1ChannelApk , ${destFile} add channel success")
            } else {
                throw new GradleException("generateV1ChannelApk , ${destFile} add channel failure")
            }
            //verify v1 signature
            if (VerifyApk.verifyV1Signature(destFile)) {
                println "generateV1ChannelApk , after add channel , apk ${destFile} v1 verify success"
            } else {
                throw new GradleException("generateV1ChannelApk , after add channel , apk ${destFile} v1 verify failure")
            }
        }

        println("------ Task ${name} generate v1 channel apk , end ------")
    }

    void generateV2ChannelApk(File baseApk, File outputDir) {
        println("------ Task ${name} generate v2 channel apk  , begin ------")

        String baseReleaseApkName = baseApk.name;
        ApkSectionInfo apkSectionInfo = V2SchemeUtil.getApkSectionInfo(baseApk)
        mChannelList.each { channel ->
            String apkChannelName = getChannelApkName(baseReleaseApkName, channel)
            println "generateV2ChannelApk , channel = ${channel} , apkChannelName = ${apkChannelName}"
            File destFile = new File(outputDir, apkChannelName)
            ChannelWriter.addChannel(apkSectionInfo, destFile, channel)
            //verify channel info
            if (ChannelReader.verifyChannel(destFile, channel)) {
                println("generateV2ChannelApk , ${destFile} add channel success")
            } else {
                throw new GradleException("generateV2ChannelApk , ${destFile} add channel failure")
            }
            //verify v2 signature
            //boolean success = V2SchemeUtil.verifyChannelApk(destFile.getAbsolutePath())
            boolean success = VerifyApk.verifyV2Signature(destFile)
            if (success) {
                println "generateV2ChannelApk , after add channel , apk ${destFile} v2 verify success"
            } else {
                throw new GradleException("generateV2ChannelApk , after add channel , apk ${destFile} v2 verify failure")
            }
//            if (!verifyV2Signature(destFile.getAbsolutePath())) {
//                throw new GradleException("verify error")
//            }
        }

        println("------ Task ${name} generate v2 channel apk , end ------")
    }

    /**
     * check necessary parameters
     */
    void checkParameter() {
        //1.check channel List
        if (mChannelList == null || mChannelList.isEmpty()) {
            throw new InvalidUserDataException("Task ${name} channel list is empty , please check it")
        }

        //4.check ChannelExtension
        if (mRebuildChannelExtension == null) {
            throw new InvalidUserDataException("Task ${name} mRebuildChannelExtension is null , you are joke!")
        }
    }

    String getChannelApkName(String baseApkName, String channel) {
        return baseApkName.replace("base", channel)
    }
}
