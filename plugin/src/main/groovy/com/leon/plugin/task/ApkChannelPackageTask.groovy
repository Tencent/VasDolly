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

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SigningConfig
import com.leon.channel.common.ApkSectionInfo
import com.leon.channel.reader.ChannelReader
import com.leon.channel.writer.ChannelWriter
import com.leon.channel.verify.VerifyApk
import com.leon.channel.writer.IdValueWriter
import com.leon.plugin.extension.ChannelConfigurationExtension
import groovy.text.SimpleTemplateEngine
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat

public class ApkChannelPackageTask extends ChannelPackageTask {
    int mChannelPackageMode = DEFAULT_MODE

    @Input
    BaseVariant mVariant

    File mBaseApk

    File mOutputDir

    @Input
    ChannelConfigurationExtension mChannelExtension

    ApkChannelPackageTask() {
        group = 'channel'
    }

    @TaskAction
    public void channel() {
        //1.check all params
        checkParameter();
        //2.check signingConfig , determine channel package mode
        checkSigningConfig()
        //3.generate channel apk
        generateChannelApk();
    }


    void generateChannelApk() {
        println("generateChannelApk , ChannelPackageMode : ${mChannelPackageMode == ChannelPackageTask.V2_MODE ? "V2 Mode" : "V1 Mode"} , isFastMode : ${mChannelExtension.isFastMode}")
        if (mChannelPackageMode == ChannelPackageTask.V1_MODE) {
            generateV1ChannelApk()
        } else if (mChannelPackageMode == ChannelPackageTask.V2_MODE) {
            generateV2ChannelApk()
        } else {
            throw new GradleException("not have precise channel package mode");
        }
    }

    /**
     * check necessary parameters
     */
    void checkParameter() {
        //merge channel list
        if (isMergeExtensionChannelList){
            mergeExtensionChannelList()
        }

        //1.check channel List
        if (channelList == null || channelList.isEmpty()) {
            throw new InvalidUserDataException("Task ${name} channel list is empty , please check it")
        }

        println("Task ${name} , channelList : ${channelList}")

        //2.check output dir
        if (mOutputDir == null) {
            throw new InvalidUserDataException("Task ${name} mOutputDir == null")
        }
        if (!mOutputDir.exists()) {
            mOutputDir.mkdirs()
        } else {
            // delete old apks
            mOutputDir.eachFile { file ->
                if (file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        }

        //3.check base apk
        if (mVariant == null) {
            throw new InvalidUserDataException("Task ${name} mVariant is null , you are joke!")
        }
        mBaseApk = mVariant.outputs.first().outputFile
        if (mBaseApk == null || !mBaseApk.exists() || !mBaseApk.isFile()) {
            throw new InvalidUserDataException("Task ${name} Base Apk is invalid , please check it")
        }

        //4.check ChannelExtension
        if (mChannelExtension == null) {
            throw new InvalidUserDataException("Task ${name} mChannelExtension is null , you are joke!")
        }
        mChannelExtension.checkParamters()
    }

    void checkSigningConfig() {
        SigningConfig signingConfig = getSigningConfig()
        if (signingConfig == null) {
            throw new GradleException("SigningConfig is null , please check it")
        }

        if (signingConfig.hasProperty("v2SigningEnabled") && signingConfig.v2SigningEnabled) {
            if (signingConfig.hasProperty("v1SigningEnabled") && !signingConfig.v1SigningEnabled) {
                throw new GradleException("you only assign V2 Mode , but not assign V1 Mode , you can't install Apk below 7.0")
            }
            mChannelPackageMode = ChannelPackageTask.V2_MODE;
        } else if ((signingConfig.hasProperty("v1SigningEnabled") && signingConfig.v1SigningEnabled) || !signingConfig.hasProperty("v1SigningEnabled")) {
            mChannelPackageMode = ChannelPackageTask.V1_MODE;
        } else {
            throw new GradleException("you must assign V1 or V2 Mode")
        }
        println("ChannelPackageMode = ${mChannelPackageMode == ChannelPackageTask.V2_MODE ? "V2 Mode" : "V1 Mode"}")
    }

    /**
     * get the SigningConfig
     * @return
     */
    SigningConfig getSigningConfig() {
        //return mVariant.buildType.signingConfig == null ? mVariant.mergedFlavor.signingConfig : mVariant.buildType.signingConfig
        SigningConfig config = null
        if (mVariant.hasProperty("signingConfig") && mVariant.signingConfig != null) {
            config = mVariant.signingConfig
        } else if (mVariant.hasProperty("variantData") &&
                mVariant.variantData.hasProperty("variantConfiguration") &&
                mVariant.variantData.variantConfiguration.hasProperty("signingConfig") &&
                mVariant.variantData.variantConfiguration.signingConfig != null) {
            config = mVariant.variantData.variantConfiguration.signingConfig
        } else if (mVariant.hasProperty("apkVariantData") &&
                mVariant.apkVariantData.hasProperty("variantConfiguration") &&
                mVariant.apkVariantData.variantConfiguration.hasProperty("signingConfig") &&
                mVariant.apkVariantData.variantConfiguration.signingConfig != null) {
            config = mVariant.apkVariantData.variantConfiguration.signingConfig
        }
        return config
    }

    @Override
    List<String> getExtensionChannelList(){
        if (mChannelExtension != null){
            return mChannelExtension.getExtensionChannelList()
        }
        return null
    }

    /**
     * get the channel apk name
     * @param channel
     * @return
     */
    String getChannelApkName(String channel) {
        def buildTime
        try {
            buildTime = new SimpleDateFormat(mChannelExtension.buildTimeDateFormat).format(new Date())
        }catch (Exception e){
            println("Task ${name} , getChannelApkName Exception : ${e.toString()}")
            buildTime = new SimpleDateFormat(ChannelConfigurationExtension.DEFAULT_DATE_FORMAT).format(new Date())
        }

        def keyValue = [
                'appName'    : project.name,
                'flavorName' : channel,
                'buildType'  : mVariant.buildType.name,
                'versionName': mVariant.versionName,
                'versionCode': mVariant.versionCode,
                'appId'      : mVariant.applicationId,
                'buildTime'  : buildTime
        ]

        def templateEngine = new SimpleTemplateEngine()
        def apkNamePrefix = templateEngine.createTemplate(mChannelExtension.apkNameFormat).make(keyValue).toString()
        return apkNamePrefix + '.apk'
    }

    void generateV1ChannelApk() {
        //check v1 signature , if not have v1 signature , you can't install Apk below 7.0
        println("------ ${project.name}:${name} generate v1 channel apk  , begin ------")

        if (!ChannelReader.containV1Signature(mBaseApk)) {
            throw new GradleException(":${name} " +
                    "apk ${apkPath} not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0")
        }

        channelList.each { channel ->
            String apkChannelName = getChannelApkName(channel)
            println "generateV1ChannelApk , channel = ${channel} , apkChannelName = ${apkChannelName}"
            File destFile = new File(mOutputDir, apkChannelName)
            copyTo(mBaseApk, destFile)
            ChannelWriter.addChannelByV1(destFile, channel)
            if (!mChannelExtension.isFastMode){
                //1. verify channel info
                if (ChannelReader.verifyChannelByV1(destFile, channel)) {
                    println("generateV1ChannelApk , ${destFile} add channel success")
                } else {
                    throw new GradleException("generateV1ChannelApk , ${destFile} add channel failure")
                }
                //2. verify v1 signature
                if (VerifyApk.verifyV1Signature(destFile)) {
                    println "generateV1ChannelApk , after add channel , apk ${destFile} v1 verify success"
                } else {
                    throw new GradleException("generateV1ChannelApk , after add channel , apk ${destFile} v1 verify failure")
                }
            }
        }

        println("------ ${project.name}:${name} generate v1 channel apk , end ------")
    }

    void generateV2ChannelApk() {
        println("------ ${project.name}:${name} generate v2 channel apk  , begin ------")

        ApkSectionInfo apkSectionInfo = IdValueWriter.getApkSectionInfo(mBaseApk, mChannelExtension.lowMemory)
        channelList.each { channel ->
            String apkChannelName = getChannelApkName(channel)
            println "generateV2ChannelApk , channel = ${channel} , apkChannelName = ${apkChannelName}"
            File destFile = new File(mOutputDir, apkChannelName)
            if (apkSectionInfo.lowMemory){
                copyTo(mBaseApk, destFile)
            }
            ChannelWriter.addChannelByV2(apkSectionInfo, destFile, channel)
            if (!mChannelExtension.isFastMode){
                //1. verify channel info
                if (ChannelReader.verifyChannelByV2(destFile, channel)) {
                    println("generateV2ChannelApk , ${destFile} add channel success")
                } else {
                    throw new GradleException("generateV2ChannelApk , ${destFile} add channel failure")
                }
                //2. verify v2 signature
                boolean success = VerifyApk.verifyV2Signature(destFile)
                if (success) {
                    println "generateV2ChannelApk , after add channel , apk ${destFile} v2 verify success"
                } else {
                    throw new GradleException("generateV2ChannelApk , after add channel , apk ${destFile} v2 verify failure")
                }
            }
            apkSectionInfo.rewind()
            if (!mChannelExtension.isFastMode){
                apkSectionInfo.checkEocdCentralDirOffset()
            }
        }

        println("------ ${project.name}:${name} generate v2 channel apk , end ------")
    }


}