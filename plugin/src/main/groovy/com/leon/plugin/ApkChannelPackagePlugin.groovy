package com.leon.plugin

import com.leon.plugin.extension.RebuildChannelConfigurationExtension
import com.leon.plugin.task.RebuildApkChannelPackageTask
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import com.leon.plugin.extension.ChannelConfigurationExtension
import com.leon.plugin.task.ApkChannelPackageTask
import org.gradle.api.Task

class ApkChannelPackagePlugin implements org.gradle.api.Plugin<Project> {
    static final TAG = "ApkChannelPackagePlugin"
    static final String CHANNEL_FILE = "channel_file"
    Project mProject;
    ChannelConfigurationExtension mChannelConfigurationExtension;
    RebuildChannelConfigurationExtension mRebuildChannelConfigurationExtension;
    List<String> mChanneInfolList;


    @Override
    void apply(Project project) {
        this.mProject = project;
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw new ProjectConfigurationException("plugin 'com.android.application' must be apply", null);
        }

        //@todo 这里要根据打包模式，进行gradle版本和配置的校验

        mChannelConfigurationExtension = project.extensions.create('channel', ChannelConfigurationExtension, project)
        mRebuildChannelConfigurationExtension = project.extensions.create('rebuildChannel', RebuildChannelConfigurationExtension, project)
        //get the channel list
        mChanneInfolList = getChannelListInfo()

        project.afterEvaluate {
            project.android.applicationVariants.all { variant ->
                def variantOutput = variant.outputs.first();
                def dirName = variant.dirName;
                def variantName = variant.name.capitalize();
                Task channelTask = project.task("channel${variantName}", type: ApkChannelPackageTask) {
                    mVariant = variant;
                    mChannelExtension = mChannelConfigurationExtension;
                    mOutputDir = new File(mChannelConfigurationExtension.baseOutputDir, dirName)
                    mChannelList = mChanneInfolList
                    dependsOn variant.assemble
                }
            }
        }

        project.task("reBuildChannel", type: RebuildApkChannelPackageTask) {
            mChannelList = mChanneInfolList
            mRebuildChannelExtension = mRebuildChannelConfigurationExtension
        }
    }

    /**
     * get the channel list
     * @return
     */
    List<String> getChannelListInfo() {
        if (!mProject.hasProperty(CHANNEL_FILE)) {
            println "not assign channel file Property"
            return null
        }

        def channelFilePath = mProject.property(CHANNEL_FILE).toString()
        if (!channelFilePath) {
            println("the channel file Property is invalid")
            return null
        }

        File channelFile = mProject.rootProject.file(channelFilePath)
        if (!channelFile.exists() || !channelFile.isFile()) {
            throw new IllegalArgumentException("the Channel file is invalid : ${channelFile.absolutePath}")
        }

        List<String> channelList = []
        channelFile.eachLine { line, num ->
            String[] array = line.split('#')
            if (array && array[0]) {
                channelList.add(array[0].trim())
            } else {
                println("skip invalid channel line , line num is ${num} , content is ${line}")
            }
        }

        println("${mProject.name} channel list is ${channelList}")
        return channelList
    }
}