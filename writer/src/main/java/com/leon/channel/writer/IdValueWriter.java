package com.leon.channel.writer;

import com.leon.channel.common.ApkSectionInfo;
import com.leon.channel.common.V2SchemeUtil;
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
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addIdValue(ApkSectionInfo apkSectionInfo, File destApk, int id, ByteBuffer valueBuffer) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if (id == ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID) {
            throw new RuntimeException("addIdValue , id can not is " + String.valueOf(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID) + " , v2 signature block use it");
        }

        Map<Integer, ByteBuffer> idValueMap = new LinkedHashMap<>();
        idValueMap.put(id, valueBuffer);
        addIdValueByteBufferMap(apkSectionInfo, destApk, idValueMap);
    }

    /**
     * add id-value pairs to apk
     *
     * @param apkSectionInfo
     * @param destApk
     * @param idValueMap
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addIdValueByteBufferMap(ApkSectionInfo apkSectionInfo, File destApk, Map<Integer, ByteBuffer> idValueMap) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if (idValueMap == null || idValueMap.isEmpty()) {
            throw new RuntimeException("addIdValueByteBufferMap , id value pair is empty");
        }
        if (idValueMap.containsKey(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) { //不能和系统V2签名块的ID冲突
            idValueMap.remove(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
        }
        System.out.println("addIdValueByteBufferMap , idValueMap = " + idValueMap);

        Map<Integer, ByteBuffer> existentIdValueMap = V2SchemeUtil.getAllIdValue(apkSectionInfo.mSchemeV2Block.getFirst());
        if (!existentIdValueMap.containsKey(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) {
            throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException(
                    "No APK V2 Signature Scheme block in APK Signing Block");
        }
        System.out.println("addIdValueByteBufferMap , existentIdValueMap = " + existentIdValueMap);

        existentIdValueMap.putAll(idValueMap);
        System.out.println("addIdValueByteBufferMap , finalIdValueMap = " + existentIdValueMap);

        ByteBuffer newApkSigningBlock = V2SchemeUtil.generateApkSigningBlock(existentIdValueMap);
        System.out.println("addIdValueByteBufferMap , oldApkSigningBlock size = " + apkSectionInfo.mSchemeV2Block.getFirst().remaining()
                + " , newApkSigningBlock size = " + newApkSigningBlock.remaining());

        ByteBuffer contentEntry = apkSectionInfo.mContentEntry.getFirst();
        ByteBuffer centralDir = apkSectionInfo.mCentralDir.getFirst();
        ByteBuffer eocd = apkSectionInfo.mEocd.getFirst();
        long centralDirOffset = apkSectionInfo.mCentralDir.getSecond();
        //update the offset of centralDir
        centralDirOffset += (newApkSigningBlock.remaining() - apkSectionInfo.mSchemeV2Block.getFirst().remaining());
        ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset);//修改了apkSectionInfo中eocd的原始数据

        RandomAccessFile fIn = new RandomAccessFile(destApk, "rw");
        long apkLength = contentEntry.remaining() + newApkSigningBlock.remaining() + centralDir.remaining() + eocd.remaining();
        fIn.seek(0l);
        //1. write real content Entry block
        fIn.write(contentEntry.array(), contentEntry.arrayOffset() + contentEntry.position(), contentEntry.remaining());
        //2. write new apk v2 scheme block
        fIn.write(newApkSigningBlock.array(), newApkSigningBlock.arrayOffset() + newApkSigningBlock.position(), newApkSigningBlock.remaining());
        //3. write central dir block
        fIn.write(centralDir.array(), centralDir.arrayOffset() + centralDir.position(), centralDir.remaining());
        //4. write eocd block
        fIn.write(eocd.array(), eocd.arrayOffset() + eocd.position(), eocd.remaining());
        fIn.setLength(apkLength);
        System.out.println("addIdValueByteBufferMap , after add channel , new apk is " + destApk.getAbsolutePath() + " , length = " + apkLength);
    }


    /**
     * add id-value(byte[]) to apk
     *
     * @param destApk
     * @param id
     * @param buffer  please ensure utf-8 charset
     */
    public static void addIdValue(File srcApk, File destApk, int id, byte[] buffer) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        ApkSectionInfo apkSectionInfo = V2SchemeUtil.getApkSectionInfo(srcApk);
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
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addIdValue(File apkFile, int id, byte[] buffer) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        addIdValue(apkFile, apkFile, id, buffer);
    }


    /**
     * add id-value(byte[]) pairs to apk
     *
     * @param srcApk
     * @param destApk
     * @param idValueByteMap
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addIdValueByteMap(File srcApk, File destApk, Map<Integer, byte[]> idValueByteMap) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if (idValueByteMap == null || idValueByteMap.isEmpty()) {
            throw new RuntimeException("addIdValueByteMap , idValueByteMap is empty");
        }
        ApkSectionInfo apkSectionInfo = V2SchemeUtil.getApkSectionInfo(srcApk);
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
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static void addIdValueByteMap(File apkFile, Map<Integer, byte[]> idValueByteMap) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        addIdValueByteMap(apkFile, apkFile, idValueByteMap);
    }
}
