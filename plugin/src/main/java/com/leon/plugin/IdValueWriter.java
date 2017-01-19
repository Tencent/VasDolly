package com.leon.plugin;

import com.leon.plugin.verifier.ApkSignatureSchemeV2Verifier;
import com.leon.plugin.verifier.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leontli on 17/1/17.
 */

public class IdValueWriter {

    public static void addIdValue(ApkSectionInfo apkSectionInfo, File destApk, int id, ByteBuffer valueBuffer) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if (id == ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID) {
            throw new RuntimeException("addIdValue , id can not is 0x7109871a , v2 signature block use it");
        }

        Map<Integer, ByteBuffer> idValueMap = new LinkedHashMap<>();
        idValueMap.put(id, valueBuffer);
        addIdValueMap(apkSectionInfo, destApk, idValueMap);
    }

    public static void addIdValueMap(ApkSectionInfo apkSectionInfo, File destApk, Map<Integer, ByteBuffer> idValueMap) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if (idValueMap == null || idValueMap.isEmpty()) {
            throw new RuntimeException("addIdValue , id value pair is empty");
        }
        if (idValueMap.containsKey(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID)) { //不能和系统V2签名块的ID冲突
            idValueMap.remove(ApkSignatureSchemeV2Verifier.APK_SIGNATURE_SCHEME_V2_BLOCK_ID);
        }

        Map<Integer, ByteBuffer> existentIdValueMap = V2SchemeUtil.getAllIdValue(apkSectionInfo.mSchemeV2Block.getFirst());
        System.out.println("existentIdValueMap = " + existentIdValueMap);
        idValueMap.putAll(existentIdValueMap);
        System.out.println("idValueMap = " + idValueMap);
        ByteBuffer newApkV2SchemeBlock = V2SchemeUtil.generateNewApkV2SchemeBlock(idValueMap);
        System.out.println("newApkV2SchemeBlock size = " + newApkV2SchemeBlock.remaining() + " , oldApkV2SchemeBlock size = " + apkSectionInfo.mSchemeV2Block.getFirst().remaining());

        ByteBuffer contentEntry = apkSectionInfo.mContentEntry.getFirst();
        ByteBuffer centralDir = apkSectionInfo.mCentralDir.getFirst();
        ByteBuffer eocd = apkSectionInfo.mEocd.getFirst();
        long centralDirOffset = apkSectionInfo.mCentralDir.getSecond();
        //update the offset of centralDir
        centralDirOffset += (newApkV2SchemeBlock.remaining() - apkSectionInfo.mSchemeV2Block.getFirst().remaining());
        ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset);

        RandomAccessFile fIn = new RandomAccessFile(destApk, "rw");
        long apkLength = contentEntry.remaining() + newApkV2SchemeBlock.remaining() + centralDir.remaining() + eocd.remaining();
        fIn.seek(0l);
        //1. write real content Entry block
        fIn.write(contentEntry.array(), contentEntry.arrayOffset() + contentEntry.position(), contentEntry.remaining());
        //2. write new apk v2 scheme block
        fIn.write(newApkV2SchemeBlock.array(), newApkV2SchemeBlock.arrayOffset() + newApkV2SchemeBlock.position(), newApkV2SchemeBlock.remaining());
        //3. write central dir block
        fIn.write(centralDir.array(), centralDir.arrayOffset() + centralDir.position(), centralDir.remaining());
        //4. write eocd block
        fIn.write(eocd.array(), eocd.arrayOffset() + eocd.position(), eocd.remaining());
        fIn.setLength(apkLength);
        System.out.println("after add channel , new apk is " + destApk.getAbsolutePath() + " , length = " + apkLength);
    }
}
