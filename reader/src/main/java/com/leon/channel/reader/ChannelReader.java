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

package com.leon.channel.reader;

import com.leon.channel.common.ChannelConstants;
import com.leon.channel.common.V1SchemeUtil;
import com.leon.channel.common.V2SchemeUtil;

import java.io.File;

/**
 * Created by leontli on 17/1/19.
 */

public class ChannelReader {

    /**
     * get channel value from apk in the v2 signature mode
     *
     * @param channelFile
     * @return
     */
    public static String getChannelByV2(File channelFile) {
        System.out.println("try to read channel info from apk : " + channelFile.getAbsolutePath());
        return IdValueReader.getStringValueById(channelFile, ChannelConstants.CHANNEL_BLOCK_ID);
    }

    /**
     * get channel info from apk in the v1 signature mode
     *
     * @param channelFile
     * @return
     * @throws Exception
     */
    public static String getChannelByV1(File channelFile) {
        try {
            return V1SchemeUtil.readChannel(channelFile);
        } catch (Exception e) {
            System.out.println("APK : " + channelFile.getAbsolutePath() + " not have channel info from Zip Comment");
        }
        return null;
    }

    /**
     * verify channel info in the v2 signature mode
     *
     * @param file
     * @param channel
     * @return
     */
    public static boolean verifyChannelByV2(File file, String channel) {
        if (channel != null) {
            return channel.equals(getChannelByV2(file));
        }
        return false;
    }

    /**
     * verify channel info in the v1 signature mode
     *
     * @param file
     * @param channel
     * @return
     */
    public static boolean verifyChannelByV1(File file, String channel) {
        if (channel != null) {
            return channel.equals(getChannelByV1(file));
        }
        return false;
    }

    /**
     * judge whether apk contain v1 signature
     *
     * @param file
     * @return
     */
    public static boolean containV1Signature(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        return V1SchemeUtil.containV1Signature(file);
    }

    /**
     * judge whether apk contain v2 signature block
     *
     * @param file
     * @return
     */
    public static boolean containV2Signature(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        return V2SchemeUtil.containV2Signature(file);
    }

}
