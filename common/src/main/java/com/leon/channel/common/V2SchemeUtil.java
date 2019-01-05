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

package com.leon.channel.common;

import com.leon.channel.common.verify.ApkSignatureSchemeV2Verifier;
import com.leon.channel.common.verify.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leontli on 17/1/18.
 */

public class V2SchemeUtil {

    /**
     * find all Id-Value Pair from ApkSignatureBlock
     * 参考ApkSignatureSchemeV2Verifier.findApkSignatureSchemeV2Block()方法
     *
     * @param apkSchemeBlock
     * @return
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static Map<Integer, ByteBuffer> getAllIdValue(ByteBuffer apkSchemeBlock) throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        ApkSignatureSchemeV2Verifier.checkByteOrderLittleEndian(apkSchemeBlock);
        // FORMAT:
        // OFFSET       DATA TYPE  DESCRIPTION
        // * @+0  bytes uint64:    size in bytes (excluding this field)
        // * @+8  bytes pairs
        // * @-24 bytes uint64:    size in bytes (same as the one above)
        // * @-16 bytes uint128:   magic
        ByteBuffer pairs = ApkSignatureSchemeV2Verifier.sliceFromTo(apkSchemeBlock, 8, apkSchemeBlock.capacity() - 24);
        Map<Integer, ByteBuffer> idValues = new LinkedHashMap<Integer, ByteBuffer>(); // keep order
        int entryCount = 0;
        while (pairs.hasRemaining()) {
            entryCount++;
            if (pairs.remaining() < 8) {
                throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException(
                    "Insufficient data to read size of APK Signing Block entry #" + entryCount);
            }
            long lenLong = pairs.getLong();
            if ((lenLong < 4) || (lenLong > Integer.MAX_VALUE)) {
                throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException(
                    "APK Signing Block entry #" + entryCount
                        + " size out of range: " + lenLong);
            }
            int len = (int) lenLong;
            int nextEntryPos = pairs.position() + len;
            if (len > pairs.remaining()) {
                throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException(
                    "APK Signing Block entry #" + entryCount + " size out of range: " + len
                        + ", available: " + pairs.remaining());
            }
            int id = pairs.getInt();
            idValues.put(id, ApkSignatureSchemeV2Verifier.getByteBuffer(pairs, len - 4));//4 is length of id
            if (id == ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID) {
                System.out.println("find V2 signature block Id : " + ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
            }
            pairs.position(nextEntryPos);
        }

        if (idValues.isEmpty()) {
            throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException(
                "not have Id-Value Pair in APK Signing Block entry #" + entryCount);
        }

        return idValues;
    }

    /**
     * get apk signature block from apk
     *
     * @param channelFile
     * @return
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static ByteBuffer getApkSigningBlock(File channelFile) throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException, IOException {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }
        RandomAccessFile apk = null;
        try {
            apk = new RandomAccessFile(channelFile, "r");
            //1.find the EOCD
            Pair<ByteBuffer, Long> eocdAndOffsetInFile = ApkSignatureSchemeV2Verifier.getEocd(apk);
            ByteBuffer eocd = eocdAndOffsetInFile.getFirst();
            long eocdOffset = eocdAndOffsetInFile.getSecond();

            if (ZipUtils.isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
                throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException("ZIP64 APK not supported");
            }

            //2.find the APK Signing Block. The block immediately precedes the Central Directory.
            long centralDirOffset = ApkSignatureSchemeV2Verifier.getCentralDirOffset(eocd, eocdOffset);//通过eocd找到中央目录的偏移量
            //3. find the apk V2 signature block
            Pair<ByteBuffer, Long> apkSignatureBlock = ApkSignatureSchemeV2Verifier.findApkSigningBlock(apk, centralDirOffset);//找到V2签名块的内容和偏移量
            return apkSignatureBlock.getFirst();
        } finally {
            if (apk != null) {
                apk.close();
            }
        }
    }

    /**
     * get the all Apk Section info from apk which is signatured by v2
     *
     * @param baseApk
     * @return
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException not have v2 sinature
     */
    public static ApkSectionInfo getApkSectionInfo(File baseApk, boolean lowMemory) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        RandomAccessFile apk = null;
        try {
            apk = new RandomAccessFile(baseApk, "r");
            //1.find the EOCD and offset
            Pair<ByteBuffer, Long> eocdAndOffsetInFile = ApkSignatureSchemeV2Verifier.getEocd(apk);
            ByteBuffer eocd = eocdAndOffsetInFile.getFirst();
            long eocdOffset = eocdAndOffsetInFile.getSecond();

            if (ZipUtils.isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
                throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException("ZIP64 APK not supported");
            }

            //2.find the APK Signing Block. The block immediately precedes the Central Directory.
            long centralDirOffset = ApkSignatureSchemeV2Verifier.getCentralDirOffset(eocd, eocdOffset);//通过eocd找到中央目录的偏移量
            Pair<ByteBuffer, Long> apkSchemeV2Block = ApkSignatureSchemeV2Verifier.findApkSigningBlock(apk, centralDirOffset);//找到V2签名块的内容和偏移量

            //3.find the centralDir
            Pair<ByteBuffer, Long> centralDir = findCentralDir(apk, centralDirOffset, (int) (eocdOffset - centralDirOffset));

            ApkSectionInfo apkSectionInfo = new ApkSectionInfo();
            apkSectionInfo.lowMemory = lowMemory;
            apkSectionInfo.apkSize = baseApk.length();
            if (!lowMemory) {
                //4.find the contentEntry
                apkSectionInfo.contentEntry = findContentEntry(apk, (int) apkSchemeV2Block.getSecond().longValue());
            }
            apkSectionInfo.schemeV2Block = apkSchemeV2Block;
            apkSectionInfo.centralDir = centralDir;
            apkSectionInfo.eocd = eocdAndOffsetInFile;

            //5. check Paramters
            apkSectionInfo.checkParamters();

            System.out.println("baseApk : " + baseApk.getAbsolutePath() + "\nApkSectionInfo = " + apkSectionInfo);
            return apkSectionInfo;
        } finally {
            if (apk != null) {
                apk.close();
            }
        }
    }

    /**
     * get the CentralDir of apk
     *
     * @param baseApk
     * @param centralDirOffset
     * @param length
     * @return
     * @throws IOException
     */
    public static Pair<ByteBuffer, Long> findCentralDir(RandomAccessFile baseApk, long centralDirOffset, int length) throws IOException {
        ByteBuffer byteBuffer = getByteBuffer(baseApk, centralDirOffset, length);
        return Pair.create(byteBuffer, centralDirOffset);
    }

    /**
     * get the ContentEntry of apk
     *
     * @param baseApk
     * @param length
     * @return
     * @throws IOException
     */
    public static Pair<ByteBuffer, Long> findContentEntry(RandomAccessFile baseApk, int length) throws IOException {
        ByteBuffer byteBuffer = getByteBuffer(baseApk, 0, length);
        return Pair.create(byteBuffer, 0L);
    }

    private static ByteBuffer getByteBuffer(RandomAccessFile baseApk, long offset, int length) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        baseApk.seek(offset);
        baseApk.readFully(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.capacity());
        return byteBuffer;
    }


    /**
     * generate the new ApkSigningBlock(contain v2 schema block)
     * reference ApkSignerV2.generateApkSigningBlock
     *
     * @param idValueMap
     * @return
     */
    public static ByteBuffer generateApkSigningBlock(Map<Integer, ByteBuffer> idValueMap) {
        if (idValueMap == null || idValueMap.isEmpty()) {
            throw new RuntimeException("getNewApkV2SchemeBlock , id value pair is empty");
        }

        // FORMAT:
        // uint64:  size (excluding this field)
        // repeated ID-value pairs:
        //     uint64:           size (excluding this field)
        //     uint32:           ID
        //     (size - 4) bytes: value
        // uint64:  size (same as the one above)
        // uint128: magic
//
//        final ByteBuffer dummy = ByteBuffer.allocate(100).order(ByteOrder.LITTLE_ENDIAN);
//        idValueMap.put(ApkSignatureSchemeV2Verifier.VERITY_PADDING_BLOCK_ID, dummy);

        long length = 16 + 8;//length is size (excluding this field) , 24 = 16 byte (magic) + 8 byte (length of the signing block excluding first 8 byte)
        for (Map.Entry<Integer, ByteBuffer> entry : idValueMap.entrySet()) {
            ByteBuffer byteBuffer = entry.getValue();
            length += 8 + 4 + (byteBuffer.remaining());
        }

        // If there has padding block, it needs to be update.
        final boolean needPadding = idValueMap.containsKey(ApkSignatureSchemeV2Verifier.VERITY_PADDING_BLOCK_ID);
        System.out.println("generateApkSigningBlock , needPadding = " + needPadding);
        if (needPadding) {
            int paddingBlockSize = 8 + 4 + (idValueMap.get(ApkSignatureSchemeV2Verifier.VERITY_PADDING_BLOCK_ID).remaining());
            // update length of apk signing block
            length -= paddingBlockSize;
            idValueMap.remove(ApkSignatureSchemeV2Verifier.VERITY_PADDING_BLOCK_ID);

            int remainder = (int) ((length + 8) % ApkSignatureSchemeV2Verifier.ANDROID_COMMON_PAGE_ALIGNMENT_BYTES);
            if (remainder != 0) {
                // Calculate the number of bytes that need to be filled
                int padding = ApkSignatureSchemeV2Verifier.ANDROID_COMMON_PAGE_ALIGNMENT_BYTES - remainder;
                // padding size must not be less than 8 + 4 bytes.
                if (padding < 8 + 4) {
                    padding += ApkSignatureSchemeV2Verifier.ANDROID_COMMON_PAGE_ALIGNMENT_BYTES;
                }
                // update length of apk signing block
                length += padding;
                // Calculate the buffer size of padding block
                int bufferSize = padding - 8 - 4;//8 is the size of padding bolck, 4 is the id of padding bolck.
                final ByteBuffer dummy = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN);
                idValueMap.put(ApkSignatureSchemeV2Verifier.VERITY_PADDING_BLOCK_ID, dummy);
                System.out.println("generateApkSigningBlock , final length = " + length + " padding = " + padding + " bufferSize = " + bufferSize);
            }
        }

        ByteBuffer newApkV2Scheme = ByteBuffer.allocate((int) (length + 8));
        newApkV2Scheme.order(ByteOrder.LITTLE_ENDIAN);
        newApkV2Scheme.putLong(length);//1.write size (excluding this field)

        for (Map.Entry<Integer, ByteBuffer> entry : idValueMap.entrySet()) {
            ByteBuffer byteBuffer = entry.getValue();
            //2.1 write length of id-value
            newApkV2Scheme.putLong(byteBuffer.remaining() + 4);//4 is length of id
            //2.2 write id
            newApkV2Scheme.putInt(entry.getKey());
            //2.3 write value
            newApkV2Scheme.put(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining());
        }

        newApkV2Scheme.putLong(length);//3.write size (same as the one above)
        newApkV2Scheme.putLong(ApkSignatureSchemeV2Verifier.APK_SIG_BLOCK_MAGIC_LO);//4. write magic
        newApkV2Scheme.putLong(ApkSignatureSchemeV2Verifier.APK_SIG_BLOCK_MAGIC_HI);//4. write magic
        if (newApkV2Scheme.remaining() > 0) {
            throw new RuntimeException("generateNewApkV2SchemeBlock error");
        }
        newApkV2Scheme.flip();
        return newApkV2Scheme;
    }

    /**
     * Returns {@code true} if the provided APK contains an APK Signature Scheme V2 signature.
     * <p>
     * NOTE: This method does not verify the signature.</b>
     *
     * @param apkPath
     * @return
     * @throws Exception
     */
    public static boolean verifyChannelApk(String apkPath) throws Exception {
        return ApkSignatureSchemeV2Verifier.hasSignature(apkPath);
    }

    /**
     * judge whether apk contain v2 signature block
     *
     * @param apk
     * @return
     */
    public static boolean containV2Signature(File apk) {
        try {
            ByteBuffer apkSigningBlock = getApkSigningBlock(apk);
            Map<Integer, ByteBuffer> idValueMap = getAllIdValue(apkSigningBlock);
            if (idValueMap.containsKey(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApkSignatureSchemeV2Verifier.SignatureNotFoundException e) {
            System.out.println("APK : " + apk.getAbsolutePath() + " not have apk signature block");
        }

        return false;
    }

}
