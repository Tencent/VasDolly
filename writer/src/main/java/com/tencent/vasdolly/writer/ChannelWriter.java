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

package com.tencent.vasdolly.writer;


import com.tencent.vasdolly.common.ApkSectionInfo;
import com.tencent.vasdolly.common.ChannelConstants;
import com.tencent.vasdolly.common.V1SchemeUtil;
import com.tencent.vasdolly.common.apk.SignatureNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leontli on 17/1/17.
 */

public class ChannelWriter {

    /**
     * add channel to apk in the v2 signature mode
     *
     * @param apkSectionInfo
     * @param destApk
     * @param channel
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addChannelByV2(ApkSectionInfo apkSectionInfo, File destApk, String channel) throws IOException, SignatureNotFoundException {
        if (destApk == null || channel == null || channel.length() <= 0) {
            throw new RuntimeException("addChannelByV2 , param invalid, channel = " + channel + " , destApk = " + destApk);
        }

        if (apkSectionInfo.lowMemory) {
            if (!destApk.exists() || !destApk.isFile() || destApk.length() <= 0) {
                throw new RuntimeException("addChannelByV2 , destApk invalid in the lowMemory mode");
            }
        } else {
            if (!destApk.getParentFile().exists()) {
                destApk.getParentFile().mkdirs();
            }
        }


        byte[] buffer = channel.getBytes(ChannelConstants.CONTENT_CHARSET);
        ByteBuffer channelByteBuffer = ByteBuffer.wrap(buffer);
        //apk中所有字节都是小端模式
        channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        IdValueWriter.addIdValue(apkSectionInfo, destApk, ChannelConstants.CHANNEL_BLOCK_ID, channelByteBuffer);
    }

    /**
     * add channel to apk in the v2 signature mode
     *
     * @param apkFile
     * @param channel
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addChannelByV2(File apkFile, String channel, boolean lowMemory) throws IOException, SignatureNotFoundException {
        addChannelByV2(apkFile, apkFile, channel, lowMemory);
    }

    /**
     * add channel to apk in the v2 signature mode
     *
     * @param srcApk  source apk
     * @param destApk dest apk
     * @param channel channel info
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addChannelByV2(File srcApk, File destApk, String channel, boolean lowMemory) throws IOException, SignatureNotFoundException {
        ApkSectionInfo apkSectionInfo = IdValueWriter.getApkSectionInfo(srcApk, lowMemory);
        addChannelByV2(apkSectionInfo, destApk, channel);
    }

    /**
     * add channel to apk in the v1 signature mode . if you use v1 signature , not necessary to again to signature after add channel info
     *
     * @param srcApk
     * @param destApk
     * @param channel
     * @throws Exception
     */
    public static void addChannelByV1(File srcApk, File destApk, String channel) throws Exception {
        V1SchemeUtil.copyFile(srcApk, destApk);
        addChannelByV1(destApk, channel);
    }

    /**
     * add channel to apk in the v1 signature mode . if you use v1 signature , not necessary to again to signature after add channel info
     *
     * @param apkFile
     * @param channel
     * @throws Exception
     */
    public static void addChannelByV1(File apkFile, String channel) throws Exception {
        V1SchemeUtil.writeChannel(apkFile, channel);
    }

    /**
     * remove channel from apk in the v2 signature mode
     *
     * @param destApk
     * @param lowMemory
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void removeChannelByV2(File destApk, boolean lowMemory) throws IOException, SignatureNotFoundException {
        if (destApk == null || !destApk.isFile() || !destApk.exists()) {
            return;
        }
        ApkSectionInfo apkSectionInfo = IdValueWriter.getApkSectionInfo(destApk, lowMemory);
        List<Integer> idList = new ArrayList<>();
        idList.add(ChannelConstants.CHANNEL_BLOCK_ID);
        IdValueWriter.removeIdValue(apkSectionInfo, destApk, idList);
        apkSectionInfo.checkParamters();
    }


    /**
     * remove channel from apk in the v1 signature mode
     */
    public static void removeChannelByV1(File destApk) throws Exception {
        V1SchemeUtil.removeChannelByV1(destApk);
    }

}
