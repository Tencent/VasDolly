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

package com.leon.plugin

import com.leon.plugin.extension.RebuildChannelConfigurationExtension
import com.leon.plugin.task.RebuildApkChannelPackageTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import com.leon.plugin.extension.ChannelConfigurationExtension
import com.leon.plugin.task.ApkChannelPackageTask
import org.gradle.api.Task

class ApkChannelPackagePlugin implements org.gradle.api.Plugin<Project> {
    static final String CHANNEL_FILE = "channel_file"
    static final String PROPERTY_CHANNELS = "channels"
    Project mProject
    ChannelConfigurationExtension mChannelConfigurationExtension
    RebuildChannelConfigurationExtension mRebuildChannelConfigurationExtension
    private List<String> mChanneInfolList


    @Override
    void apply(Project project) {
        this.mProject = project
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw new ProjectConfigurationException("plugin 'com.android.application' must be apply", null)
        }

        //@todo 这里要根据打包模式，进行gradle版本和配置的校验
        mChannelConfigurationExtension = project.extensions.create('channel', ChannelConfigurationExtension, project)
        mRebuildChannelConfigurationExtension = project.extensions.create('rebuildChannel', RebuildChannelConfigurationExtension, project)

        if (mProject.hasProperty(PROPERTY_CHANNELS)){
            mChanneInfolList = []
            def tempChannelsProperty = mProject.getProperties().get(PROPERTY_CHANNELS)
            if (tempChannelsProperty != null && tempChannelsProperty.trim().length() > 0) {
                tempChannelsProperty.split(",").each {
                    mChanneInfolList.add(it.trim())
                }
            }
            if (mChanneInfolList.isEmpty()){
                throw new InvalidUserDataException("Property(${PROPERTY_CHANNELS}) channel list is empty , please fix it")
            }
        }else {
            //get the channel list
            mChanneInfolList = getChannelListInfo()
        }

        project.afterEvaluate {
            project.android.applicationVariants.all { variant ->
                def variantOutput = variant.outputs.first();
                def dirName = variant.dirName;
                def variantName = variant.name.capitalize();
                Task channelTask = project.task("channel${variantName}", type: ApkChannelPackageTask) {
                    mVariant = variant;
                    mChannelExtension = mChannelConfigurationExtension;
                    mOutputDir = new File(mChannelConfigurationExtension.baseOutputDir, dirName)
                    isMergeExtensionChannelList = !mProject.hasProperty(PROPERTY_CHANNELS)
                    channelList = mChanneInfolList
                    dependsOn variant.assemble
                }
            }
        }

        project.task("reBuildChannel", type: RebuildApkChannelPackageTask) {
            isMergeExtensionChannelList = !mProject.hasProperty(PROPERTY_CHANNELS)
            channelList = mChanneInfolList
            mRebuildChannelExtension = mRebuildChannelConfigurationExtension
        }
    }

    /**
     * get the channel list
     * @return
     */
    List<String> getChannelListInfo() {
        List<String> channelList = []
        if (mProject.hasProperty(CHANNEL_FILE)){
            def channelFilePath = mProject.property(CHANNEL_FILE).toString()
            if (channelFilePath) {
                File channelFile = mProject.rootProject.file(channelFilePath)
                if (channelFile.exists() && channelFile.isFile()){
                    channelFile.eachLine { line, num ->
                        String[] array = line.split('#')
                        if (array && array[0]) {
                            channelList.add(array[0].trim())
                        } else {
                            println("skip invalid channel line , line num is ${num} , content is ${line}")
                        }
                    }
                }
            }
        }

        println("${mProject.name} channel list is ${channelList}")
        return channelList
    }
}