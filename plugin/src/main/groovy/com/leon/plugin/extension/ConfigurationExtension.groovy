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

import org.gradle.api.Project

public class ConfigurationExtension {

    public Project mProject

    public File channelFile

    //fast mode : generate channel apk without checking(speed can be increased by up to 10 times)
    public boolean isFastMode

    //only fit v2 signature
    public boolean lowMemory

    public ConfigurationExtension(Project project){
        this.mProject = project
        isFastMode = false
        lowMemory = false
    }

    public List<String> getExtensionChannelList() {
        if (channelFile != null && channelFile.isFile() && channelFile.exists()) {
            List<String> channelList = []
            channelFile.eachLine { line, num ->
                String[] array = line.split('#')
                if (array && array[0]) {
                    channelList.add(array[0].trim())
                } else {
                    println("Extension skip invalid channel line , line num is ${num} , content is ${line}")
                }
            }
            println("${mProject.name} extension channel list is ${channelList}")
            return channelList
        }
        return null
    }
}