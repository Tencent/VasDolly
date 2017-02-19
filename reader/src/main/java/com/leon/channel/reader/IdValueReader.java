package com.leon.channel.reader;

import com.leon.channel.common.ChannelConstants;
import com.leon.channel.common.V2SchemeUtil;
import com.leon.channel.common.verify.ApkSignatureSchemeV2Verifier;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by leontli on 17/1/19.
 */

public class IdValueReader {

    /**
     * get string value by id
     *
     * @param channelFile
     * @param id
     * @return
     */
    public static String getStringValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        byte[] buffer = getByteValueById(channelFile, id);
        try {
            if (buffer != null && buffer.length > 0) {
                return new String(buffer, ChannelConstants.CONTENT_CHARSET);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * get byte[] value from apk by id
     *
     * @param channelFile
     * @param id
     * @return
     */
    public static byte[] getByteValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        ByteBuffer value = getByteBufferValueById(channelFile, id);
        System.out.println("getByteValueById , id = " + id + " , value = " + value);
        if (value != null) {
            return Arrays.copyOfRange(value.array(), value.arrayOffset() + value.position(), value.arrayOffset() + value.limit());
        }
        return null;
    }


    /**
     * get ByteBuffer value from apk by id
     *
     * @param channelFile
     * @param id
     * @return
     */
    public static ByteBuffer getByteBufferValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        Map<Integer, ByteBuffer> idValueMap = getAllIdValueMap(channelFile);
        System.out.println("getByteBufferValueById , destApk " + channelFile.getAbsolutePath() + " IdValueMap = " + idValueMap);

        if (idValueMap != null) {
            return idValueMap.get(id);
        }

        return null;
    }

    /**
     * find all Id-Value Pair from Apk
     *
     * @param channelFile
     * @return
     */
    public static Map<Integer, ByteBuffer> getAllIdValueMap(File channelFile) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        try {
            ByteBuffer apkSigningBlock = V2SchemeUtil.getApkSigningBlock(channelFile);
            return V2SchemeUtil.getAllIdValue(apkSigningBlock);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApkSignatureSchemeV2Verifier.SignatureNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
