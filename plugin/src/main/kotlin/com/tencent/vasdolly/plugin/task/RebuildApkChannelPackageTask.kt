package com.tencent.vasdolly.plugin.task

import com.tencent.vasdolly.plugin.extension.RebuildChannelConfigExtension
import com.tencent.vasdolly.reader.ChannelReader
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

/***
 * 根据已有基础包重新生成多渠道包
 */
open class RebuildApkChannelPackageTask : ChannelPackageTask() {
    //根据已有基础包重新生成多渠道包
    @get:Input
    var rebuildExt: RebuildChannelConfigExtension? = null

    @TaskAction
    fun taskAction() {
        //1.check all params
        checkParameter();
        //2.generate channel apk
        generateChannelApk(rebuildExt?.baseApk, rebuildExt?.outputDir)
    }

    private fun checkParameter() {
        //merge channel list
        if (mergeExtChannelList) {
            mergeChannelList()
        }

        //1.check channel List
        if (channelList.isEmpty()) {
            throw InvalidUserDataException("Task $name channel list is empty , please check it")
        }
        println("Task $name , channelList : $channelList")

        //4.check ChannelExtension
        if (rebuildExt == null) {
            throw InvalidUserDataException("Task $name rebuildExt is null , you are joke!")
        }
    }

    /***
     * 生成渠道包
     */
    private fun generateChannelApk(baseApk: File?, outputDir: File?) {
        println("generateChannelApk baseApk:${baseApk?.absolutePath},outputDir:${outputDir?.path}")
        val lowMemory = rebuildExt?.lowMemory ?: false
        val isFastMode = rebuildExt?.fastMode ?: false
        //检要baseApk
        if (baseApk == null || !baseApk.exists() || !baseApk.isFile) {
            println("baseApk:$baseApk, it is not a valid file , so can not rebuild channel apk")
            return
        }
        //检查是否有输出目录
        outputDir?.let { outDir ->
            if (!outDir.exists()) {
                outDir.mkdirs()
            }
            //清空输出目录下已经存在的apk
            outDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
            //开始生成渠道包
            if (ChannelReader.containV2Signature(baseApk)) {
                generateV2ChannelApk(baseApk, outputDir, lowMemory, isFastMode)
            } else if (ChannelReader.containV1Signature(baseApk)) {
                generateV1ChannelApk(baseApk, outputDir, isFastMode)
            }
        } ?: throw GradleException("rebuild apk channel outputDir is empty")
    }

    /**
     * 获取Apk文件名
     */
    override fun getChannelApkName(baseApkName: String, channel: String): String {
        return if (baseApkName.contains("base")) {
            baseApkName.replace("base", channel)
        } else {
            "$channel-$baseApkName";
        }
    }

    override fun getExtensionChannelList(): List<String> {
        return rebuildExt?.getExtensionChannelList() ?: listOf()
    }
}