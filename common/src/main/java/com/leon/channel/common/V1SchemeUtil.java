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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Created by leontli on 17/2/19.
 */

public class V1SchemeUtil {

    /**
     * write channel to apk
     *
     * @param file
     * @param channel
     * @throws Exception
     */
    public static void writeChannel(File file, String channel) throws Exception {
        if (file == null || !file.exists() || !file.isFile() || channel == null || channel.isEmpty()) {
            throw new Exception("param error , file : " + file + " , channel : " + channel);
        }
        RandomAccessFile raf = null;
        byte[] comment = channel.getBytes(ChannelConstants.CONTENT_CHARSET);
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = getEocd(file);
        if (eocdAndOffsetInFile.getFirst().remaining() == ZipUtils.ZIP_EOCD_REC_MIN_SIZE) {
            System.out.println("file : " + file.getAbsolutePath() + " , has no comment");
            try {
                raf = new RandomAccessFile(file, "rw");
                //1.locate comment length field
                raf.seek(file.length() - ChannelConstants.SHORT_LENGTH);
                //2.write zip comment length (content field length + length field length + magic field length)
                writeShort(comment.length + ChannelConstants.SHORT_LENGTH + ChannelConstants.V1_MAGIC.length, raf);
                //3.write content
                raf.write(comment);
                //4.write content length
                writeShort(comment.length, raf);
                //5. write magic bytes
                raf.write(ChannelConstants.V1_MAGIC);
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
        } else {
            System.out.println("file : " + file.getAbsolutePath() + " , has comment");
            if (containV1Magic(file)) {
                try {
                    String existChannel = readChannel(file);
                    if (existChannel != null) {
                        file.delete();
                        throw new ChannelExistException("file : " + file.getAbsolutePath() + " has a channel : " + existChannel + ", only ignore");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int existCommentLength = ZipUtils.getUnsignedInt16(eocdAndOffsetInFile.getFirst(), ZipUtils.ZIP_EOCD_REC_MIN_SIZE - ChannelConstants.SHORT_LENGTH);
            int newCommentLength = existCommentLength + comment.length + ChannelConstants.SHORT_LENGTH + ChannelConstants.V1_MAGIC.length;
            try {
                raf = new RandomAccessFile(file, "rw");
                //1.locate comment length field
                raf.seek(eocdAndOffsetInFile.getSecond() + ZipUtils.ZIP_EOCD_REC_MIN_SIZE - ChannelConstants.SHORT_LENGTH);
                //2.write zip comment length (existCommentLength + content field length + length field length + magic field length)
                writeShort(newCommentLength, raf);
                //3.locate where channel should begin
                raf.seek(eocdAndOffsetInFile.getSecond() + ZipUtils.ZIP_EOCD_REC_MIN_SIZE + existCommentLength);
                //4.write content
                raf.write(comment);
                //5.write content length
                writeShort(comment.length, raf);
                //6.write magic bytes
                raf.write(ChannelConstants.V1_MAGIC);
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
        }
    }

    /**
     * remove channel from apk in the v1 signature mode
     */
    public static void removeChannelByV1(File file) throws Exception {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new Exception("param error , file : " + file);
        }
        RandomAccessFile raf = null;
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = getEocd(file);
        if (eocdAndOffsetInFile.getFirst().remaining() == ZipUtils.ZIP_EOCD_REC_MIN_SIZE) {
            System.out.println("file : " + file.getName() + " , has no comment");
        } else {
            System.out.println("file : " + file.getName() + " , has comment");
            int existCommentLength = ZipUtils.getUnsignedInt16(eocdAndOffsetInFile.getFirst(), ZipUtils.ZIP_EOCD_REC_MIN_SIZE - ChannelConstants.SHORT_LENGTH);
            try {
                raf = new RandomAccessFile(file, "rw");
                //1.locate comment length field
                raf.seek(eocdAndOffsetInFile.getSecond() + ZipUtils.ZIP_EOCD_REC_MIN_SIZE - ChannelConstants.SHORT_LENGTH);
                //2.write zip comment length (0)
                writeShort(0, raf);
                //3. modify the length of apk file
                raf.setLength(file.length() - existCommentLength);
                System.out.println("file : " + file.getName() + " , remove comment success");
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
        }
    }

    /**
     * read channel from apk
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static String readChannel(File file) throws Exception {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long index = raf.length();
            byte[] buffer = new byte[ChannelConstants.V1_MAGIC.length];
            index -= ChannelConstants.V1_MAGIC.length;
            raf.seek(index);
            raf.readFully(buffer);
            // whether magic bytes matched
            if (isV1MagicMatch(buffer)) {
                index -= ChannelConstants.SHORT_LENGTH;
                raf.seek(index);
                // read channel length field
                int length = readShort(raf);
                if (length > 0) {
                    index -= length;
                    raf.seek(index);
                    // read channel bytes
                    byte[] bytesComment = new byte[length];
                    raf.readFully(bytesComment);
                    return new String(bytesComment, ChannelConstants.CONTENT_CHARSET);
                } else {
                    throw new Exception("zip channel info not found");
                }
            } else {
                throw new Exception("zip v1 magic not found");
            }
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }

    private static void writeShort(int i, DataOutput out) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(ChannelConstants.SHORT_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) i);
        out.write(bb.array());
    }

    private static short readShort(DataInput input) throws IOException {
        byte[] buf = new byte[ChannelConstants.SHORT_LENGTH];
        input.readFully(buf);
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort(0);
    }

    /**
     * judge whether contain v1 magic int the end of file
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean containV1Magic(File file) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long index = raf.length();
            byte[] buffer = new byte[ChannelConstants.V1_MAGIC.length];
            index -= ChannelConstants.V1_MAGIC.length;
            raf.seek(index);
            raf.readFully(buffer);
            return isV1MagicMatch(buffer);
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }

    /**
     * check v1 magic
     *
     * @param buffer
     * @return
     */
    private static boolean isV1MagicMatch(byte[] buffer) {
        if (buffer.length != ChannelConstants.V1_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ChannelConstants.V1_MAGIC.length; ++i) {
            if (buffer[i] != ChannelConstants.V1_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * get eocd and offset from apk
     *
     * @param apk
     * @return
     * @throws IOException
     * @throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException
     */
    public static Pair<ByteBuffer, Long> getEocd(File apk) throws IOException, ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if (apk == null || !apk.exists() || !apk.isFile()) {
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(apk, "r");
            //find the EOCD
            Pair<ByteBuffer, Long> eocdAndOffsetInFile = ApkSignatureSchemeV2Verifier.getEocd(raf);
            if (ZipUtils.isZip64EndOfCentralDirectoryLocatorPresent(raf, eocdAndOffsetInFile.getSecond())) {
                throw new ApkSignatureSchemeV2Verifier.SignatureNotFoundException("ZIP64 APK not supported");
            }

            return eocdAndOffsetInFile;
        }finally {
            if (raf != null){
                raf.close();
            }
        }

    }

    /**
     * copy file
     *
     * @param src
     * @param dest
     * @throws IOException
     */
    public static void copyFile(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(src).getChannel();
            destination = new FileOutputStream(dest).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * judge whether apk contain v1 signature
     *
     * @param file
     * @return
     */
    public static boolean containV1Signature(File file) {
        JarFile jarFile;
        try {
            jarFile = new JarFile(file);
            JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            JarEntry sfEntry = null;
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().matches("META-INF/\\w+\\.SF")) {
                    sfEntry = jarFile.getJarEntry(entry.getName());
                    break;
                }
            }
            if (manifestEntry != null && sfEntry != null) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static class ChannelExistException extends Exception {
        static final long serialVersionUID = -3387516993124229949L;

        public ChannelExistException() {
            super();
        }

        public ChannelExistException(final String message) {
            super(message);
        }
    }

}
