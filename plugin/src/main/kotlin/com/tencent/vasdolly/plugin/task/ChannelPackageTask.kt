package com.tencent.vasdolly.plugin.task

import com.tencent.vasdolly.reader.ChannelReader
import com.tencent.vasdolly.verify.VerifyApk
import com.tencent.vasdolly.writer.ChannelWriter
import com.tencent.vasdolly.writer.IdValueWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File

abstract class ChannelPackageTask : DefaultTask() {
    @Input
    var mergeExtChannelList: Boolean = true

    @Input
    var channelList: MutableList<String> = mutableListOf()

    init {
        group = "com.tencent.vasdolly"
    }

    /***
     * 合并渠道列表
     */
    @Internal
    fun mergeChannelList() {
        val extensionChannelList = getExtensionChannelList()
        if (extensionChannelList.isNotEmpty()) {
            channelList.addAll(extensionChannelList)
        }
    }

    /**
     * 生成V2渠道包
     */
    @Internal
    fun generateV2ChannelApk(baseApk: File, outputDir: File, lowMemory: Boolean, isFastMode: Boolean) {
        println("------ $project.name:$name generate v2 channel apk  , begin ------")
        val apkSectionInfo = IdValueWriter.getApkSectionInfo(baseApk, lowMemory)
        channelList.forEach { channel ->
            val apkChannelName = getChannelApkName(baseApk.name, channel)
            println("++++++++++++++++++++++++++++++  channel($channel)  ++++++++++++++++++++++++++++++")
            println("generateV2ChannelApk,channel=$channel,apkChannelName=$apkChannelName")
            val destFile = File(outputDir, apkChannelName)
            if (apkSectionInfo.lowMemory) {
                baseApk.copyTo(destFile)
            }
            ChannelWriter.addChannelByV2(apkSectionInfo, destFile, channel)
            if (!isFastMode) {
                //1. verify channel info
                if (ChannelReader.verifyChannelByV2(destFile, channel)) {
                    println("generateV2ChannelApk, $destFile add channel success")
                } else {
                    throw GradleException("generateV2ChannelApk, $destFile add channel failure")
                }
                //2. verify v2 signature
                if (VerifyApk.verifyV2Signature(destFile)) {
                    println("generateV2ChannelApk,after add channel,apk $destFile v2 verify success")
                } else {
                    throw GradleException("generateV2ChannelApk,after add channel, apk $destFile v2 verify failure")
                }
            }
            apkSectionInfo.rewind()

            if (!isFastMode) {
                apkSectionInfo.checkEocdCentralDirOffset()
            }
        }
        println("------ $project.name:$name generate v2 channel apk , end ------")
    }

    /**
     * 生成V1渠道包
     */
    @Internal
    fun generateV1ChannelApk(baseApk: File, outputDir: File,isFastMode: Boolean) {
        //check v1 signature , if not have v1 signature , you can't install Apk below 7.0
        println("------$project.name:$name generate v1 channel apk, begin------")

        if (!ChannelReader.containV1Signature(baseApk)) {
            val msg = "$name get signing config apk ${baseApk.absolutePath} not signed by v1,you can't install Apk below Android7.0"
            throw GradleException(msg)
        }

        //检查是否已经有渠道信息
        val apkChannel = ChannelReader.getChannelByV1(baseApk)
        if (apkChannel.isNotEmpty()) {
            throw GradleException("baseApk $baseApk.getAbsolutePath() has channel")
        }

        channelList.forEach { channel ->
            val apkChannelName = getChannelApkName(baseApk.name, channel)
            println("++++++++++++++++++++++++++++++  channel($channel)  ++++++++++++++++++++++++++++++")
            println("generateV1ChannelApk,channel=$channel,apkChannelName=$apkChannelName")
            val destFile = File(outputDir, apkChannelName)
            baseApk.copyTo(destFile)
            ChannelWriter.addChannelByV1(destFile, channel)
            if (isFastMode) {
                //1. verify channel info
                if (ChannelReader.verifyChannelByV1(destFile, channel)) {
                    println("generateV1ChannelApk,apk $destFile add channel success")
                } else {
                    throw GradleException("generateV1ChannelApk,apk $destFile add channel failure")
                }
                //2. verify v1 signature
                if (VerifyApk.verifyV1Signature(destFile)) {
                    println("generateV1ChannelApk,after add channel,apk $destFile v1 verify success")
                } else {
                    throw GradleException("generateV1ChannelApk , after add channel , apk $destFile v1 verify failure")
                }
            }
        }
        println("------$project.name:$name generate v1 channel apk , end------")
    }

    @Internal
    abstract fun getChannelApkName(baseApkName: String, channel: String): String

    @Internal
    abstract fun getExtensionChannelList(): List<String>
}