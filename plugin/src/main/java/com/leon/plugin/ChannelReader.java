package com.leon.plugin;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.android.annotations.NonNull;

import java.io.File;
import java.io.UnsupportedEncodingException;

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
        return getValueById(channelFile, ChannelConstants.CHANNEL_BLOCK_ID);
    }


    /**
     * get value by id
     *
     * @param channelFile
     * @param id
     * @return
     */
    public static String getValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        byte[] buffer = IdValueReader.getByteValueById(channelFile, id);
        try {
            return new String(buffer, ChannelConstants.CONTENT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
