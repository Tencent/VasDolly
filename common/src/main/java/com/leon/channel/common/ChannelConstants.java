package com.leon.channel.common;

/**
 * Created by leontli on 17/1/17.
 */

public class ChannelConstants {
    public static final int CHANNEL_BLOCK_ID = 0x881155ff;
    public static final String CONTENT_CHARSET = "UTF-8";
    static final int SHORT_LENGTH = 2;
    static final byte[] V1_MAGIC = new byte[]{0x21, 0x5a, 0x58, 0x4b, 0x21}; //!ZXK!
}
