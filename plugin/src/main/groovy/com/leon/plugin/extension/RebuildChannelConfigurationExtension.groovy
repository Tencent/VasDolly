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
import org.gradle.api.Project;

/**
 * Created by leontli on 17/2/18.
 */

public class RebuildChannelConfigurationExtension extends ConfigurationExtension{
    /**
     * debug base APK path
     */
    File baseDebugApk;
    /**
     * release base APK path
     */
    File baseReleaseApk;
    /**
     * debug channel apk output dir
     */
    File debugOutputDir;
    /**
     * release channel apk output dir
     */
    File releaseOutputDir;

    public RebuildChannelConfigurationExtension(Project project) {
        super(project)
        debugOutputDir = new File(project.buildDir, "rebuildChannel/debug")
        releaseOutputDir = new File(project.buildDir, "rebuildChannel/release")
    }


    public boolean isNeedRebuildDebugApk() {
        if (baseDebugApk == null || !baseDebugApk.exists() || !baseDebugApk.isFile()) {
            println("baseDebugApk : ${baseDebugApk} , it is not a valid file , so can not rebuild debug channel apk");
            return false
        } else {
            if (debugOutputDir == null) {
                throw new GradleException("baseDebugApk : ${baseDebugApk} is ok , so you should set a valid debugOutputDir , or you can set baseDebugApk = null")
            }

            if (!debugOutputDir.exists()) {
                debugOutputDir.mkdirs()
            } else {
                // delete old apks
                debugOutputDir.eachFile { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }

            return true
        }
    }

    public boolean isNeedRebuildReleaseApk() {
        if (baseReleaseApk == null || !baseReleaseApk.exists() || !baseReleaseApk.isFile()) {
            println("baseReleaseApk : ${baseReleaseApk} , it is not a valid file , so can not rebuild release channel apk");
            return false
        } else {
            if (releaseOutputDir == null) {
                throw new GradleException("baseReleaseApk : ${baseReleaseApk} is ok , so you should set a valid releaseOutputDir , or you can set baseReleaseApk = null")
            }

            if (!releaseOutputDir.exists()) {
                releaseOutputDir.mkdirs()
            } else {
                // delete old apks
                releaseOutputDir.eachFile { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }
            return true
        }
    }
}