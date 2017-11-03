package com.leon.plugin.extension

import org.gradle.api.Project

public class ConfigurationExtension {

    public Project mProject;

    public File channelFile;

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
            println("${mProject.name} ExtensionChannelList is ${channelList}")
            return channelList
        }
        return null
    }
}