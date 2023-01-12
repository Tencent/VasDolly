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

package com.tencent.vasdolly.writer;

import com.tencent.vasdolly.common.ApkSectionInfo;
import com.tencent.vasdolly.common.V2SchemeUtil;
import com.tencent.vasdolly.common.apk.SignatureNotFoundException;
import com.tencent.vasdolly.common.apk.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leontli on 17/1/17.
 */

public class IdValueWriter {

    /**
     * add id-value to apk
     *
     * @param apkSectionInfo
     * @param destApk
     * @param id
     * @param valueBuffer
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addIdValue(ApkSectionInfo apkSectionInfo, File destApk, int id, ByteBuffer valueBuffer) throws IOException, SignatureNotFoundException {
        Map<Integer, ByteBuffer> idValueMap = new LinkedHashMap<>();
        idValueMap.put(id, valueBuffer);
        addIdValueByteBufferMap(apkSectionInfo, destApk, idValueMap);
    }

    public static void removeIdValue(ApkSectionInfo apkSectionInfo, File destApk, List<Integer> idList) throws SignatureNotFoundException, IOException {
        if (apkSectionInfo == null || destApk == null || !destApk.isFile() || !destApk.exists() || idList == null || idList.isEmpty()) {
            return;
        }
        Map<Integer, ByteBuffer> existentIdValueMap = V2SchemeUtil.getAllIdValue(apkSectionInfo.apkSigningBlock.getFirst());
        int existentIdValueSize = existentIdValueMap.size();
        System.out.println("removeIdValue , existed IdValueMap = " + existentIdValueMap);

        for (Integer id : idList) {
            existentIdValueMap.remove(id);
        }
        int remainderIdValueSize = existentIdValueMap.size();
        if (existentIdValueSize == remainderIdValueSize) {
            System.out.println("removeIdValue , No idValue was deleted");
        } else {
            System.out.println("removeIdValue , final IdValueMap = " + existentIdValueMap);
            ByteBuffer newApkSigningBlock = V2SchemeUtil.generateApkSigningBlock(existentIdValueMap);
            System.out.println("removeIdValue , oldApkSigningBlock size = " + apkSectionInfo.apkSigningBlock.getFirst().remaining() + " , newApkSigningBlock size = " + newApkSigningBlock.remaining());

            ByteBuffer centralDir = apkSectionInfo.centralDir.getFirst();
            ByteBuffer eocd = apkSectionInfo.eocd.getFirst();
            long centralDirOffset = apkSectionInfo.centralDir.getSecond();
            int apkChangeSize = newApkSigningBlock.remaining() - apkSectionInfo.apkSigningBlock.getFirst().remaining();
            //update the offset of centralDir
            ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset + apkChangeSize);//修改了EOCD中保存的中央目录偏移量

            long apkLength = apkSectionInfo.apkSize + apkChangeSize;
            RandomAccessFile fIn = null;
            try {
                fIn = new RandomAccessFile(destApk, "rw");
                if (apkSectionInfo.lowMemory) {
                    long signBlockPos = apkSectionInfo.apkSigningBlock.getSecond();
                    System.out.println("seek to apk signing block pos:" + signBlockPos);
                    fIn.seek(signBlockPos);
                } else {
                    ByteBuffer contentEntry = apkSectionInfo.contentEntry.getFirst();
                    fIn.seek(apkSectionInfo.contentEntry.getSecond());
                    //1. write real content Entry block
                    fIn.write(contentEntry.array(), contentEntry.arrayOffset() + contentEntry.position(), contentEntry.remaining());
                }

                //2. write new apk v2 scheme block
                fIn.write(newApkSigningBlock.array(), newApkSigningBlock.arrayOffset() + newApkSigningBlock.position(), newApkSigningBlock.remaining());
                //3. write central dir block
                fIn.write(centralDir.array(), centralDir.arrayOffset() + centralDir.position(), centralDir.remaining());
                //4. write eocd block
                fIn.write(eocd.array(), eocd.arrayOffset() + eocd.position(), eocd.remaining());
                //5. modify the length of apk file
                if (fIn.getFilePointer() != apkLength) {
                    throw new RuntimeException("after removeIdValue , file size wrong , FilePointer : " + fIn.getFilePointer() + ", apkLength : " + apkLength);
                }
                fIn.setLength(apkLength);
                System.out.println("removeIdValue , after remove channel , apk is " + destApk.getAbsolutePath() + " , length = " + destApk.length());
            } finally {
                //恢复EOCD中保存的中央目录偏移量，满足基础包的APK结构
                ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset);
                if (fIn != null) {
                    fIn.close();
                }
            }
        }
    }

    /**
     * add id-value pairs to apk
     *
     * @param apkSectionInfo
     * @param destApk
     * @param idValueMap
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addIdValueByteBufferMap(ApkSectionInfo apkSectionInfo, File destApk, Map<Integer, ByteBuffer> idValueMap) throws IOException, SignatureNotFoundException {
        if (idValueMap == null || idValueMap.isEmpty()) {
            throw new RuntimeException("addIdValueByteBufferMap , id value pair is empty");
        }

        Map<Integer, ByteBuffer> existentIdValueMap = V2SchemeUtil.getAllIdValue(apkSectionInfo.apkSigningBlock.getFirst());
        existentIdValueMap.putAll(idValueMap);

        ByteBuffer newApkSigningBlock = V2SchemeUtil.generateApkSigningBlock(existentIdValueMap);
        System.out.println("addIdValueByteBufferMap , oldApkSigningBlock size = " + apkSectionInfo.apkSigningBlock.getFirst().remaining() + " , newApkSigningBlock size = " + newApkSigningBlock.remaining());

        ByteBuffer centralDir = apkSectionInfo.centralDir.getFirst();
        ByteBuffer eocd = apkSectionInfo.eocd.getFirst();
        long centralDirOffset = apkSectionInfo.centralDir.getSecond();
        int apkChangeSize = newApkSigningBlock.remaining() - apkSectionInfo.apkSigningBlock.getFirst().remaining();
        //update the offset of centralDir
        ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset + apkChangeSize);//修改了EOCD中保存的中央目录偏移量

        long apkLength = apkSectionInfo.apkSize + apkChangeSize;
        RandomAccessFile fIn = null;
        try {
            fIn = new RandomAccessFile(destApk, "rw");
            if (apkSectionInfo.lowMemory) {
                long signBlockPos = apkSectionInfo.apkSigningBlock.getSecond();
                System.out.println("seek to apk signing block pos:" + signBlockPos + ",dest apk file len:" + fIn.length());
                fIn.seek(signBlockPos);
            } else {
                ByteBuffer contentEntry = apkSectionInfo.contentEntry.getFirst();
                fIn.seek(apkSectionInfo.contentEntry.getSecond());
                //1. write real content Entry block
                fIn.write(contentEntry.array(), contentEntry.arrayOffset() + contentEntry.position(), contentEntry.remaining());
            }

            //2. write new apk v2 scheme block
            fIn.write(newApkSigningBlock.array(), newApkSigningBlock.arrayOffset() + newApkSigningBlock.position(), newApkSigningBlock.remaining());
            //3. write central dir block
            fIn.write(centralDir.array(), centralDir.arrayOffset() + centralDir.position(), centralDir.remaining());
            //4. write eocd block
            fIn.write(eocd.array(), eocd.arrayOffset() + eocd.position(), eocd.remaining());
            //5. modify the length of apk file
            if (fIn.getFilePointer() != apkLength) {
                throw new RuntimeException("after addIdValueByteBufferMap , file size wrong , FilePointer : " + fIn.getFilePointer() + ", apkLength : " + apkLength);
            }
            fIn.setLength(apkLength);
            System.out.println("addIdValueByteBufferMap , after add channel , new apk is " + destApk.getAbsolutePath() + " , length = " + destApk.length());
        } finally {
            //恢复EOCD中保存的中央目录偏移量，满足基础包的APK结构
            ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset);
            if (fIn != null) {
                fIn.close();
            }
        }
    }


    /**
     * add id-value(byte[]) to apk
     *
     * @param destApk
     * @param id
     * @param buffer  please ensure utf-8 charset
     */
    public static void addIdValue(File srcApk, File destApk, int id, byte[] buffer, boolean lowMemory) throws IOException, SignatureNotFoundException {
        ApkSectionInfo apkSectionInfo = getApkSectionInfo(srcApk, lowMemory);
        ByteBuffer channelByteBuffer = ByteBuffer.wrap(buffer);
        //apk中所有字节都是小端模式
        channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        addIdValue(apkSectionInfo, destApk, id, channelByteBuffer);
    }


    /**
     * add id-value(byte[]) to apk
     *
     * @param apkFile
     * @param id
     * @param buffer
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addIdValue(File apkFile, int id, byte[] buffer, boolean lowMemory) throws IOException, SignatureNotFoundException {
        addIdValue(apkFile, apkFile, id, buffer, lowMemory);
    }


    /**
     * add id-value(byte[]) pairs to apk
     *
     * @param srcApk
     * @param destApk
     * @param idValueByteMap
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addIdValueByteMap(File srcApk, File destApk, Map<Integer, byte[]> idValueByteMap, boolean lowMemory) throws IOException, SignatureNotFoundException {
        if (idValueByteMap == null || idValueByteMap.isEmpty()) {
            throw new RuntimeException("addIdValueByteMap , idValueByteMap is empty");
        }
        ApkSectionInfo apkSectionInfo = getApkSectionInfo(srcApk, lowMemory);
        Map<Integer, ByteBuffer> idValues = new LinkedHashMap<Integer, ByteBuffer>(); // keep order

        for (Integer integer : idValueByteMap.keySet()) {
            ByteBuffer channelByteBuffer = ByteBuffer.wrap(idValueByteMap.get(integer));
            //apk中所有字节都是小端模式
            channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            idValues.put(integer, channelByteBuffer);
        }

        addIdValueByteBufferMap(apkSectionInfo, destApk, idValues);
    }

    /**
     * add id-value(byte[]) pairs to apk
     *
     * @param apkFile
     * @param idValueByteMap
     * @throws IOException
     * @throws SignatureNotFoundException
     */
    public static void addIdValueByteMap(File apkFile, Map<Integer, byte[]> idValueByteMap, boolean lowMemory) throws IOException, SignatureNotFoundException {
        addIdValueByteMap(apkFile, apkFile, idValueByteMap, lowMemory);
    }

    public static void removeIdValue(File apk, int id) {

    }

    public static ApkSectionInfo getApkSectionInfo(File baseApk, boolean lowMemory) throws IOException, SignatureNotFoundException {
        if (baseApk == null || !baseApk.exists() || !baseApk.isFile()) {
            return null;
        }
        return V2SchemeUtil.getApkSectionInfo(baseApk, lowMemory);
    }
}
