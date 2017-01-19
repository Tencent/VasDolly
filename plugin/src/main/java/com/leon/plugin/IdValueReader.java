package com.leon.plugin;

import com.leon.plugin.verifier.ApkSignatureSchemeV2Verifier;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by leontli on 17/1/19.
 */

public class IdValueReader {


    public static byte[] getByteValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        ByteBuffer value = getByteBufferValueById(channelFile, id);
        if (value != null) {
            return Arrays.copyOfRange(value.array(), value.arrayOffset() + value.position(), value.arrayOffset() + value.limit());
        } else {
            System.out.println("id = " + id + " , value = " + value);
        }
        return null;
    }


    public static ByteBuffer getByteBufferValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        Map<Integer, ByteBuffer> idValueMap = getAllIdValueMap(channelFile);
        System.out.println("idValueMap = " + idValueMap);
        if (idValueMap != null) {
            return idValueMap.get(id);
        }

        return null;
    }

    public static Map<Integer, ByteBuffer> getAllIdValueMap(File channelFile) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        try {
            ByteBuffer apkV2SigningBlock = V2SchemeUtil.getApkV2SigningBlock(channelFile);
            return V2SchemeUtil.getAllIdValue(apkV2SigningBlock);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApkSignatureSchemeV2Verifier.SignatureNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
