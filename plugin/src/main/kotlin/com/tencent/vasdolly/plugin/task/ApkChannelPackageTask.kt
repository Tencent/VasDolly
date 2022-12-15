package com.tencent.vasdolly.plugin.task

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.tencent.vasdolly.plugin.extension.ChannelConfigExtension
import com.tencent.vasdolly.plugin.util.SimpleAGPVersion
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class ApkChannelPackageTask : ChannelPackageTask() {
    // 当前基础apk
    @Internal
    var baseApk: File? = null

    @get:Input
    var variant: ApplicationVariant? = null

    @get:Input
    var channelExtension: ChannelConfigExtension? = null

    @TaskAction
    fun taskAction() {
        //1.check all params
        checkParameter();
        //2.generate channel apk
        generateChannelApk();
    }

    private fun getOutputDir(): File? {
        if (variant?.flavorName?.isNotEmpty() == true) {
            return File(channelExtension?.outputDir, variant!!.flavorName!!)
        } else {
            return channelExtension?.outputDir
        }
    }

    /***
     * check channel plugin params
     */
    private fun checkParameter() {
        if (mergeExtChannelList) {
            mergeChannelList()
        }

        //1.check channel List
        if (channelList.isEmpty()) {
            throw InvalidUserDataException("Task $name channel list is empty,please check it")
        }
        println("Task $name, channelList: $channelList")


        //2.check base apk
        if (variant == null) {
            throw InvalidUserDataException("Task $name variant is null")
        }
        baseApk = getVariantBaseApk() ?: throw RuntimeException("can't find base apk")
        println("Task $name, baseApk: ${baseApk?.absolutePath}")


        //3.check ChannelExtension
        if (channelExtension == null) {
            throw InvalidUserDataException("Task $name channel is null")
        }
        channelExtension?.checkParams()
        println("Task $name, channel files outputDir:${getOutputDir()?.absolutePath}")
    }

    @Suppress("PrivateApi")
    private fun getVariantBaseApk(): File? {
        return variant?.let { variant ->
            val currentAGPVersion = SimpleAGPVersion.ANDROID_GRADLE_PLUGIN_VERSION
            val agpVersion7 = SimpleAGPVersion(7, 0)
            val apkFolder = if (currentAGPVersion < agpVersion7) {
                //AGP4.2
                val artifactCls = Class.forName("com.android.build.api.artifact.ArtifactType")
                val apkClass =
                    Class.forName("com.android.build.api.artifact.ArtifactType${'$'}APK").kotlin
                val provider = variant.artifacts.javaClass.getMethod("get", artifactCls)
                    .invoke(variant.artifacts, apkClass.objectInstance) as Provider<Directory>
                provider.get()
            } else {
                //AGP7.0
                variant.artifacts.get(SingleArtifact.APK).get()
            }
            variant.artifacts.getBuiltArtifactsLoader().load(apkFolder)?.let {
                File(it.elements.first().outputFile)
            }
        }
    }

    /***
     * 根据签名类型生成不同的渠道包
     */
    private fun generateChannelApk() {
        val outputDir = getOutputDir()
        println("generateChannelApk baseApk:${baseApk?.absolutePath},outputDir:${outputDir?.path}")
        val signingConfig = variant?.signingConfig!!
        val lowMemory = channelExtension?.lowMemory ?: false
        val isFastMode = channelExtension?.fastMode ?: false
        when {
            signingConfig.enableV2Signing.get() -> {
                generateV2ChannelApk(baseApk!!, outputDir!!, lowMemory, isFastMode)
            }
            signingConfig.enableV1Signing.get() -> {
                generateV1ChannelApk(baseApk!!, outputDir!!, isFastMode)
            }
            else -> {
                throw GradleException("not have precise channel package mode");
            }
        }
    }

    /***
     * 获取渠道文件名
     */
    override fun getChannelApkName(baseApkName: String, channel: String): String {
        var timeFormat = ChannelConfigExtension.DEFAULT_DATE_FORMAT
        if (channelExtension?.buildTimeDateFormat!!.isNotEmpty()) {
            timeFormat = channelExtension?.buildTimeDateFormat!!
        }
        val buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(timeFormat))
        val outInfo = variant?.outputs?.first()

        val keyValue: MutableMap<String, String> = mutableMapOf()
        keyValue["appName"] = project.name
        keyValue["productFlavorName"] = variant?.flavorName ?: ""
        keyValue["channelName"] = channel
        keyValue["flavorName"] = channel
        keyValue["buildType"] = variant?.buildType ?: ""
        keyValue["versionName"] = outInfo?.versionName?.get() ?: ""
        keyValue["versionCode"] = outInfo?.versionCode?.get().toString()
        keyValue["appId"] = variant?.applicationId?.get() ?: ""
        keyValue["buildTime"] = buildTime

        //默认文件名
        var apkNamePrefix = ChannelConfigExtension.DEFAULT_APK_NAME_FORMAT
        if (channelExtension?.apkNameFormat!!.isNotEmpty()) {
            apkNamePrefix = channelExtension?.apkNameFormat!!
        }
        keyValue.forEach { (k, v) ->
            apkNamePrefix = apkNamePrefix.replace("${'$'}{" + k + "}", v)
        }
        return "$apkNamePrefix.apk"
    }

    /***
     * 获取渠道列表
     */
    override fun getExtensionChannelList(): List<String> {
        return channelExtension?.getExtensionChannelList() ?: listOf()
    }
}