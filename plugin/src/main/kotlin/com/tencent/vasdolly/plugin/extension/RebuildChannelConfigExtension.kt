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

package com.tencent.vasdolly.plugin.extension

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

open class RebuildChannelConfigExtension(project: Project) : ConfigExtension(project) {
    /**
     * debug base APK path
     */
    var baseDebugApk: File? = null

    /**
     * release base APK path
     */
    var baseReleaseApk: File? = null

    /**
     * debug channel apk output dir
     */
    var debugOutputDir: File? = null

    /**
     * release channel apk output dir
     */
    var releaseOutputDir: File? = null

    init {
        debugOutputDir = File(project.buildDir, "rebuildChannel/debug")
        releaseOutputDir = File(project.buildDir, "rebuildChannel/release")
    }

    fun isNeedRebuildDebugApk(): Boolean {
        if (baseDebugApk == null || !baseDebugApk?.exists()!! || !baseDebugApk?.isFile!!) {
            println("baseDebugApk:$baseDebugApk, it is not a valid file , so can not rebuild debug channel apk")
            return false
        } else {
            if (debugOutputDir == null) {
                throw GradleException("baseDebugApk:$baseDebugApk is ok , so you should set a valid debugOutputDir , or you can set baseDebugApk = null")
            }

            if (!debugOutputDir?.exists()!!) {
                debugOutputDir?.mkdirs()
            } else {
                // delete old apks
                debugOutputDir?.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }
            return true
        }
    }

    fun isNeedRebuildReleaseApk(): Boolean {
        if (baseReleaseApk == null || !baseReleaseApk?.exists()!! || !baseReleaseApk?.isFile!!) {
            println("baseReleaseApk:$baseReleaseApk, it is not a valid file , so can not rebuild release channel apk")
            return false
        } else {
            if (releaseOutputDir == null) {
                throw GradleException("baseReleaseApk:$baseReleaseApk is ok , so you should set a valid releaseOutputDir , or you can set baseReleaseApk = null")
            }

            if (!releaseOutputDir?.exists()!!) {
                releaseOutputDir?.mkdirs()
            } else {
                // delete old apks
                releaseOutputDir?.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk")) {
                        file.delete()
                    }
                }
            }
            return true
        }
    }
}