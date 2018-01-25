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

import java.io.File;

/**
 * Created by leontli on 17/1/19.
 */

public class ChannelReader {

    /**
     * get channel value from apk signature block
     *
     * @param channelFile
     * @return
     */
    public static String getChannel(File channelFile) {
        System.out.println("try to read channel info from apk : " + channelFile.getAbsolutePath());
        return IdValueReader.getStringValueById(channelFile, ChannelConstants.CHANNEL_BLOCK_ID);
    }

    /**
     * verify channel info
     *
     * @param file
     * @param channel
     * @return
     */
    public static boolean verifyChannel(File file, String channel) {
        if (channel != null) {
            return channel.equals(getChannel(file));
        }
        return false;
    }

    /**
     * get channel info from apk comment field
     *
     * @param channelFile
     * @return
     * @throws Exception
     */
    public static String getChannelByZipComment(File channelFile) {
        try {
            return V1SchemeUtil.readChannel(channelFile);
        } catch (Exception e) {
            //e.printStackTrace();
            System.out.println("APK : " + channelFile.getAbsolutePath() + " not have channel info from Zip Comment");
        }
        return null;
    }


}
