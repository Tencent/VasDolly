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

package com.leon.plugin.extension

import org.gradle.api.GradleException;
import org.gradle.api.Project

public class ChannelConfigurationExtension extends ConfigurationExtension {
    static
    final String DEFAULT_APK_NAME_FORMAT = '${appName}-${versionName}-${versionCode}-${flavorName}-${buildType}-${buildTime}'
    public static final String DEFAULT_DATE_FORMAT = 'yyyyMMdd-HHmmss'

    /**
     * 渠道包输出目录
     */
    File baseOutputDir

    /**
     * 渠道包的命名格式
     */
    String apkNameFormat

    /**
     * buildTime的时间格式
     */
    public String buildTimeDateFormat

    public ChannelConfigurationExtension(Project project) {
        super(project)
        baseOutputDir = new File(project.buildDir, "channel")
        apkNameFormat = DEFAULT_APK_NAME_FORMAT
        buildTimeDateFormat = DEFAULT_DATE_FORMAT
    }

    public void checkParamters() {
        if (mProject == null || baseOutputDir == null || apkNameFormat.isEmpty() || buildTimeDateFormat.isEmpty()) {
            throw new GradleException("ChannelConfigurationExtension params invalid , " +
                    "mProject = ${mProject} , baseOutputDir = ${baseOutputDir} , apkNameFormat = ${apkNameFormat} , buildTimeDateFormat = ${buildTimeDateFormat}")
        }

        if (!baseOutputDir.exists()) {
            baseOutputDir.mkdirs()
        }
    }
}