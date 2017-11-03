package com.leon.plugin.extension

import org.gradle.api.GradleException;
import org.gradle.api.Project

public class ChannelConfigurationExtension extends ConfigurationExtension{
    static
    final String DEFAULT_APK_NAME_FORMAT = '${appName}-${versionName}-${versionCode}-${flavorName}-${buildType}'

    /**
     * 渠道包输出目录
     */
    File baseOutputDir;

    /**
     * 渠道包的命名格式
     */
    String apkNameFormat

    ChannelConfigurationExtension(Project project) {
        mProject = project
        baseOutputDir = new File(project.buildDir, "channel");
        apkNameFormat = DEFAULT_APK_NAME_FORMAT;
    }

    public void checkParamters() {
        if (mProject == null || baseOutputDir == null || apkNameFormat.isEmpty()) {
            throw new GradleException("ChannelConfigurationExtension params invalid , " +
                    "mProject = ${mProject} , baseOutputDir = ${baseOutputDir} , apkNameFormat = ${apkNameFormat}")
        }

        if (!baseOutputDir.exists()) {
            baseOutputDir.mkdirs()
        }
    }
}