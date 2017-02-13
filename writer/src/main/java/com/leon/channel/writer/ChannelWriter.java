package com.leon.channel.writer;



import com.leon.channel.common.ApkSectionInfo;
import com.leon.channel.common.ChannelConstants;
import com.leon.channel.common.verify.ApkSignatureSchemeV2Verifier;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by leontli on 17/1/17.
 */

public class ChannelWriter {
    private static final String TAG = "ChannelWriter";

    public static void addChannel(ApkSectionInfo apkSectionInfo, File destApk, String channel) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        System.out.println("destApk = " + destApk.getAbsolutePath() + " , channel = " + channel);
        if (destApk == null || channel == null || channel.length() <= 0) {
            throw new RuntimeException("addChannel param invalid, channel = " + channel + " , destApk = " + destApk);
        }
        if (destApk.exists()) {
            destApk.delete();
        }

        byte[] buffer = channel.getBytes(ChannelConstants.CONTENT_CHARSET);
        ByteBuffer channelByteBuffer = ByteBuffer.wrap(buffer);
        //apk中所有字节都是小端模式
        channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        IdValueWriter.addIdValue(apkSectionInfo, destApk, ChannelConstants.CHANNEL_BLOCK_ID, channelByteBuffer);
    }


    /**
     * @param destApk
     * @param id
     * @param buffer  please ensure utf-8 charset
     */
    public static void addIdValue(File destApk, int id, byte[] buffer) {

    }

    public static void addIdValue(File destApk, int id, String value) {

    }


}
