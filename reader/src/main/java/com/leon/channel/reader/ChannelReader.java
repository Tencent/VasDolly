package com.leon.channel.reader;

import com.leon.channel.common.ChannelConstants;

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
        return IdValueReader.getStringValueById(channelFile, ChannelConstants.CHANNEL_BLOCK_ID);
    }


}
