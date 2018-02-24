/*
 * Tencent is pleased to support the open source community by making VasDolly available.
 *
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS,WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leon.plugin.task

import com.leon.channel.common.ApkSectionInfo
import com.leon.channel.common.V1SchemeUtil
import com.leon.channel.common.V2SchemeUtil
import com.leon.channel.reader.ChannelReader
import com.leon.channel.writer.ChannelWriter;
import com.com.leon.channel.verify.VerifyApk;
import com.leon.plugin.extension.RebuildChannelConfigurationExtension
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
        checkParameter()

        if (mRebuildChannelExtension.isNeedRebuildDebugApk()) {
            generateChannelApk(mRebuildChannelExtension.baseDebugApk, mRebuildChannelExtension.debugOutputDir)
        }

        if (mRebuildChannelExtension.isNeedRebuildReleaseApk()) {
            generateChannelApk(mRebuildChannelExtension.baseReleaseApk, mRebuildChannelExtension.releaseOutputDir)
        }
    }

    void generateChannelApk(File baseApk, File outputDir) {
        int mode = judgeChannelPackageMode(baseApk)
        println("generateChannelApk , ChannelPackageMode : ${mode == ChannelPackageTask.V2_MODE ? "V2 Mode" : "V1 Mode"} , isFastMode : ${mRebuildChannelExtension.isFastMode}")
        if (mode == ChannelPackageTask.V1_MODE) {
            println("${baseApk} , ChannelPackageMode = V1 Mode");
            generateV1ChannelApk(baseApk, outputDir)
        } else if (mode == ChannelPackageTask.V2_MODE) {
            println("${baseApk} , ChannelPackageMode = V2 Mode");
            generateV2ChannelApk(baseApk, outputDir)
        } else {
            throw new GradleException("not have precise channel package mode");
        }
    }

    int judgeChannelPackageMode(File baseApk) {
        if (V2SchemeUtil.containV2Signature(baseApk, false)) {
            return ChannelPackageTask.V2_MODE
        } else if (V1SchemeUtil.containV1Signature(baseApk)) {
            return ChannelPackageTask.V1_MODE
        } else {
            return ChannelPackageTask.DEFAULT_MODE
        }
    }

    void generateV1ChannelApk(File baseApk, File outputDir) {
        //check v1 signature , if not have v1 signature , you can't install Apk below 7.0
        if (!V1SchemeUtil.containV1Signature(baseApk)) {
            throw new GradleException("Task ${name} " +
                    "apk ${apkPath} not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0")
        }
        try {
            //判断基础包是否已经包含渠道信息
            String testChannel = V1SchemeUtil.readChannel(baseApk);
            if (testChannel != null) {
                println("baseApk : " + baseApk.getAbsolutePath() + " has a channel : " + testChannel + ", only ignore");
                return;
            }
        } catch (Exception e) {
            //e.printStackTrace()
            println("baseApk : " + baseApk.getAbsolutePath() + " not have channel info , so can add a channel info")
        }

        println("------ Task ${name} generate v1 channel apk  , begin ------")

        String baseReleaseApkName = baseApk.name;
        mChannelList.each { channel ->
            String apkChannelName = getChannelApkName(baseReleaseApkName, channel)
            println "generateV1ChannelApk , channel = ${channel} , apkChannelName = ${apkChannelName}"
            File destFile = new File(outputDir, apkChannelName)
            copyTo(baseApk, destFile)
            V1SchemeUtil.writeChannel(destFile, channel)
            if (!mRebuildChannelExtension.isFastMode){
                //1. verify channel info
                if (V1SchemeUtil.verifyChannel(destFile, channel)) {
                    println("generateV1ChannelApk , ${destFile} add channel success")
                } else {
                    throw new GradleException("generateV1ChannelApk , ${destFile} add channel failure")
                }
                //2. verify v1 signature
                if (VerifyApk.verifyV1Signature(destFile)) {
                    println "generateV1ChannelApk , after add channel , apk : ${destFile} v1 verify success"
                } else {
                    throw new GradleException("generateV1ChannelApk , after add channel , apk : ${destFile} v1 verify failure")
                }
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
            if (!mRebuildChannelExtension.isFastMode){
                //1. verify channel info
                if (ChannelReader.verifyChannel(destFile, channel)) {
                    println("generateV2ChannelApk , ${destFile} add channel success")
                } else {
                    throw new GradleException("generateV2ChannelApk , ${destFile} add channel failure")
                }
                //2. verify v2 signature
                //boolean success = V2SchemeUtil.verifyChannelApk(destFile.getAbsolutePath())
                boolean success = VerifyApk.verifyV2Signature(destFile)
                if (success) {
                    println "generateV2ChannelApk , after add channel , apk : ${destFile} v2 verify success"
                } else {
                    throw new GradleException("generateV2ChannelApk , after add channel , apk : ${destFile} v2 verify failure")
                }
//            if (!verifyV2Signature(destFile.getAbsolutePath())) {
//                throw new GradleException("verify error")
//            }
            }
        }

        println("------ Task ${name} generate v2 channel apk , end ------")
    }

    /**
     * check necessary parameters
     */
    void checkParameter() {
        //merge channel list
        mergeExtensionChannelList()
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
        if (baseApkName.contains("base")){
            return baseApkName.replace("base", channel)
        }else {
            return channel + "-" + baseApkName;
        }
    }

    @Override
    List<String> getExtensionChannelList(){
        if (mRebuildChannelExtension != null){
            return mRebuildChannelExtension.getExtensionChannelList()
        }
        return null
    }
}
