package com.leon.plugin.extension

import org.gradle.api.GradleException;
import org.gradle.api.Project

public class ChannelConfigurationExtension {
    static
    final String DEFAULT_APK_NAME_FORMAT = '${appName}-${versionName}-${versionCode}-${flavorName}-${buildType}'

    Project mProject;

    /**
     * 渠道包输出目录
     */
    File mOutputDir;

    /**
     * 渠道包的命名格式
     */
    String mApkNameFormat

    //String apkSignerPath


    ChannelConfigurationExtension(Project project) {
        this.mProject = project
        mOutputDir = new File(project.buildDir, "channel");
        mApkNameFormat = DEFAULT_APK_NAME_FORMAT;
    }

    public void cleanOutputDir(){
        if (mOutputDir.exists()){
            mOutputDir.deleteDir()
        }
        mOutputDir.mkdirs()
    }

    public void checkParamters() {
        if (mProject == null || mOutputDir == null || mApkNameFormat.isEmpty()) {
            throw new GradleException("ChannelConfigurationExtension params invalid , " +
                    "mProject = ${mProject} , mOutputDir = ${mOutputDir} , mApkNameFormat = ${mApkNameFormat}")
        }
    }
}