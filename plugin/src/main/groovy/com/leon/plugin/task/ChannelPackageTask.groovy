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

package com.leon.plugin.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.process.ExecResult

/**
 * Created by leontli on 17/2/19.
 */
abstract class ChannelPackageTask extends DefaultTask {
    public static final int DEFAULT_MODE = -1
    public static final int V1_MODE = 1
    public static final int V2_MODE = 2


    @Input
    public boolean isMergeExtensionChannelList = true

    @Input
    public List<String> channelList

    protected mergeExtensionChannelList(){
        List<String> extensionChannelList = getExtensionChannelList()
        if (extensionChannelList != null && !extensionChannelList.isEmpty()){
            if (channelList == null){
                channelList = extensionChannelList
            }else {
                channelList.addAll(extensionChannelList)
            }
        }
    }

    abstract List<String> getExtensionChannelList()

    /**
     * verify apk by apksigner.jar
     * @param apkPath
     * @return
     */
    boolean verifyV2Signature(String apkPath) {
        def apktoolCmd = []
        apktoolCmd.add("java")
        apktoolCmd.add("-jar")
        apktoolCmd.add("$project.projectDir/tools/apksigner.jar")
        apktoolCmd.add("verify")
        apktoolCmd.add("-v")
        apktoolCmd.add("$apkPath")
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        println "apk cmd verify : " + apktoolCmd
        ExecResult result = project.exec {
            standardOutput = arrayOutputStream
            commandLine apktoolCmd
        }

        String str = arrayOutputStream.toString("utf-8");
        boolean v2VerifySuccess = false
        str.eachLine { String value ->
            println "value = ${value}"
            if (value.contains("APK Signature Scheme v2") && value.trim().endsWith("true")) {
                v2VerifySuccess = true
            }
        }

        return result.exitValue == 0 && v2VerifySuccess
    }

    void copyTo(File src, File dest) {
        def input = src.newInputStream()
        def output = dest.newOutputStream()
        output << input
        input.close()
        output.close()
    }
}
