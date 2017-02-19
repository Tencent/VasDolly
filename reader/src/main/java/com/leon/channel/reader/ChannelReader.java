package com.leon.channel.reader;

import com.leon.channel.common.ChannelConstants;
import com.leon.channel.common.V1SchemeUtil;

import java.io.File;

/**
 * Created by leontli on 17/1/19.
 */

public class ChannelReader {

    /**
     * get channel value
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
            e.printStackTrace();
        }
        return null;
    }


}
