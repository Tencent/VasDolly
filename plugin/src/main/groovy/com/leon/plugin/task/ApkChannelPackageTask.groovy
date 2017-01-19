package com.leon.plugin.task

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SigningConfig
import com.leon.plugin.ApkSectionInfo
import com.leon.plugin.ChannelReader
import com.leon.plugin.ChannelWriter
import com.leon.plugin.V2SchemeUtil
import com.leon.plugin.extension.ChannelConfigurationExtension
import groovy.text.SimpleTemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction


public class ApkChannelPackageTask extends DefaultTask {
    private static final String TAG = "ApkChannelPackageTask";

    static final int DEFAULT_MODE = -1;
    static final int V1_MODE = 1;
    static final int V2_MODE = 2;

    int mChannelPackageMode = DEFAULT_MODE;
    @Input
    BaseVariant mVariant;

    File mBaseApk

    @Input
    ChannelConfigurationExtension mChannelExtension;

    @Input
    List<String> mChannelList;


    ApkChannelPackageTask() {
        group = 'channel'
    }

    @TaskAction
    public void channel() {
        //1.check all params
        checkParameter();
        //2.check signingConfig , determine channel package mode
        checkSigningConfig()

        mChannelExtension.cleanOutputDir()

        ApkSectionInfo apkSectionInfo = V2SchemeUtil.getApkSectionInfo(mBaseApk)
        mChannelList.each { channel ->
            String apkChannelName = getChannelApkName(channel)
            File destFile = new File(mChannelExtension.mOutputDir, apkChannelName)
            println "apkChannelName = ${apkChannelName} , channel = ${apkChannelName}"
            ChannelWriter.addChannel(apkSectionInfo, destFile, channel)
//            boolean success = V2SchemeUtil.verifyChannelApk(destFile.getAbsolutePath())
//            if (success) {
//                println "after add channel , channel apk verify success"
//            }
            def tempChannel = ChannelReader.getChannel(destFile)
            println "tempChannel = ${tempChannel}"
        }
    }

    /**
     * check necessary parameters
     */
    void checkParameter() {
        if (mChannelList == null || mChannelList.isEmpty()) {
            throw new InvalidUserDataException("channel list is empty , please check it")
        }

        if (mVariant == null) {
            throw new GradleException("mVariant is null , you are joke!")
        }

        mBaseApk = mVariant.outputs.first().outputFile
        if (mBaseApk == null || !mBaseApk.exists() || !mBaseApk.isFile()) {
            throw new GradleException("Base Apk is invalid , please check it")
        }

        if (mChannelExtension == null) {
            throw new GradleException("mChannelExtension is null , you are joke!")
        }

        mChannelExtension.checkParamters()


    }

    void checkSigningConfig() {
        SigningConfig signingConfig = getSigningConfig()
        if (signingConfig == null) {
            throw new GradleException("SigningConfig is null , please check it")
        }

        if (signingConfig.hasProperty("v2SigningEnabled") && signingConfig.v2SigningEnabled) {
            mChannelPackageMode = V2_MODE;
        } else if ((signingConfig.hasProperty("v1SigningEnabled") && signingConfig.v1SigningEnabled) || !signingConfig.hasProperty("v1SigningEnabled")) {
            mChannelPackageMode = V1_MODE;
        } else {
            throw new GradleException("you must assign V1 or V2 Mode")
        }

    }

    /**
     * get the SigningConfig
     * @return
     */
    SigningConfig getSigningConfig() {
        //return mVariant.buildType.signingConfig == null ? mVariant.mergedFlavor.signingConfig : mVariant.buildType.signingConfig
        return mVariant.apkVariantData.variantConfiguration.signingConfig
    }

    /**
     * get the channel apk name
     * @param channel
     * @return
     */
    String getChannelApkName(String channel) {
        def keyValue = [
                'appName'    : project.name,
                'flavorName' : channel,
                'buildType'  : mVariant.buildType.name,
                'versionName': mVariant.versionName,
                'versionCode': mVariant.versionCode,
                'appId'      : mVariant.applicationId
        ]

        def templateEngine = new SimpleTemplateEngine()
        def apkNamePrefix = templateEngine.createTemplate(mChannelExtension.mApkNameFormat).make(keyValue).toString()
        return apkNamePrefix + '.apk'
    }

    boolean generateChannelApk() {
        if (mChannelPackageMode == V1_MODE) {
            generateV1ChannelApk()
        } else if (mChannelPackageMode == V2_MODE) {
            generateV2ChannelApk()
        } else {
            throw new GradleException("not have precise channel package mode");
        }
    }

    boolean generateV1ChannelApk() {

    }

    boolean generateV2ChannelApk() {

    }


}