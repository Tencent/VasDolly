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
 * <p>
 * 针对V2签名块的Id-Value序列进行读取操作
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
            //e.printStackTrace();
            System.out.println("APK : " + channelFile.getAbsolutePath() + " not have apk signature block");
        }

        return null;
    }

}
