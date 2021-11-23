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

import org.gradle.api.Project
import java.io.File

open class ChannelConfigExtension(project: Project) : ConfigExtension(project) {

    companion object {
        //默认文件名模板
        const val DEFAULT_APK_NAME_FORMAT = "${'$'}{appName}-${'$'}{versionName}-${'$'}{versionCode}-${'$'}{flavorName}-${'$'}{buildType}-${'$'}{buildTime}"

        //默认时间格式
        const val DEFAULT_DATE_FORMAT = "yyyyMMdd-HHmmss"
    }

    /**
     * 渠道包输出目录
     */
    var baseOutputDir = File(project.buildDir, "channel")

    /**
     * 渠道包的命名格式
     */
    var apkNameFormat = DEFAULT_APK_NAME_FORMAT

    /**
     * buildTime的时间格式
     */
    var buildTimeDateFormat = DEFAULT_DATE_FORMAT

    init {
        if (!baseOutputDir.exists()) {
            baseOutputDir.mkdirs()
        }
    }
    /**
     * 检查channel属性参数
     */
    fun checkParams() {
        if (!baseOutputDir.exists()) {
            baseOutputDir.mkdirs()
        }
    }
}