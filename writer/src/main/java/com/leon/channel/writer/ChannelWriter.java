package com.leon.channel.writer;


import com.leon.channel.common.ApkSectionInfo;
import com.leon.channel.common.ChannelConstants;
import com.leon.channel.common.V1SchemeUtil;
import com.leon.channel.common.V2SchemeUtil;
import com.leon.channel.common.verify.ApkSignatureSchemeV2Verifier;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by leontli on 17/1/17.
 */

public class ChannelWriter {

    /**
     * add channel to apk file
     *
     * @param apkSectionInfo
     * @param destApk
     * @param channel
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addChannel(ApkSectionInfo apkSectionInfo, File destApk, String channel) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        System.out.println("destApk = " + destApk.getAbsolutePath() + " , channel = " + channel);
        if (destApk == null || channel == null || channel.length() <= 0) {
            throw new RuntimeException("addChannel , param invalid, channel = " + channel + " , destApk = " + destApk);
        }

        if (!destApk.getParentFile().exists()) {
            destApk.getParentFile().mkdirs();
        }

        byte[] buffer = channel.getBytes(ChannelConstants.CONTENT_CHARSET);
        ByteBuffer channelByteBuffer = ByteBuffer.wrap(buffer);
        //apk中所有字节都是小端模式
        channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        IdValueWriter.addIdValue(apkSectionInfo, destApk, ChannelConstants.CHANNEL_BLOCK_ID, channelByteBuffer);
    }

    /**
     * add channel to apk file
     *
     * @param apkFile
     * @param channel
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addChannel(File apkFile, String channel) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        addChannel(apkFile, apkFile, channel);
    }

    /**
     * add channel to apk file
     *
     * @param srcApk  source apk
     * @param destApk dest apk
     * @param channel channel info
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addChannel(File srcApk, File destApk, String channel) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        ApkSectionInfo apkSectionInfo = V2SchemeUtil.getApkSectionInfo(srcApk);
        addChannel(apkSectionInfo, destApk, channel);
    }

    /**
     * add channel info to zip comment field . if you use v1 signature , not necessary to again to signature after add channel info
     *
     * @param srcApk
     * @param destApk
     * @param channel
     * @throws Exception
     */
    public static void addChannelToZipComment(File srcApk, File destApk, String channel) throws Exception {
        V1SchemeUtil.copyFile(srcApk, destApk);
        V1SchemeUtil.writeChannel(destApk, channel);
    }

    /**
     * add channel info to zip comment field . if you use v1 signature , not necessary to again to signature after add channel info
     *
     * @param apkFile
     * @param channel
     * @throws Exception
     */
    public static void addChannelToZipComment(File apkFile, String channel) throws Exception {
        V1SchemeUtil.writeChannel(apkFile, channel);
    }

}
