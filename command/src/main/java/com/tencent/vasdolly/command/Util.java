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

package com.tencent.vasdolly.command;

import com.android.apksig.ApkVerifier;
import com.tencent.vasdolly.verify.VerifyApk;
import com.tencent.vasdolly.common.ApkSectionInfo;
import com.tencent.vasdolly.reader.ChannelReader;
import com.tencent.vasdolly.writer.ChannelWriter;
import com.tencent.vasdolly.writer.IdValueWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by zys on 17-6-8.
 */

public class Util {

    private static final String V1 = "V1";
    private static final String V2 = "V2";
    private static final String V1_V2 = "V1_V2";

    public static final int DEFAULT_MODE = 0;
    public static final int V1_MODE = 1;
    public static final int V2_MODE = 2;
    public static final int V3_MODE = 3;

    /**
     * 获取APK的签名方式
     *
     * @param apkFile
     * @return V1, V2, V1_V2
     */
    public static String getSignMode(File apkFile) {
        try {
            int signMode = judgeChannelPackageMode(apkFile);
            return "V" + signMode;
        } catch (Exception e) {
            System.out.println("get sign exception:" + e.getMessage());
            return "Apk was not signed";
        }
    }

    /**
     * 获取已知APK的渠道信息
     *
     * @param apkFile
     * @return
     */
    public static String readChannel(File apkFile) {
        String channel = ChannelReader.getChannelByV2(apkFile);
        if (channel.isEmpty()) {
            channel = ChannelReader.getChannelByV1(apkFile);
        }
        return channel;
    }

    public static void main(String[] args) {
        File f = new File("C:/Users/caikun/Desktop/222.apk");
        writeChannel(f, Arrays.asList("10000"), f, false, false);
    }

    /**
     * 根据不同的方式写入渠道，并生成apk
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    public static void writeChannel(File baseApk, List<String> channelList, File outputDir, boolean isMultiThread, boolean isFastMode) {
        if (channelList.isEmpty()) {
            System.out.println("channel list is empty , please set channel list");
            return;
        }
        int mode = DEFAULT_MODE;
        try {
            mode = judgeChannelPackageMode(baseApk);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println("begin writing apk channel and apk signature version:V" + mode);
        System.out.println("baseApk:" + baseApk.getAbsolutePath());
        System.out.println("outputDir:" + outputDir.getAbsolutePath());
        System.out.println("isMultiThread:" + isMultiThread);
        System.out.println("isFastMode:" + isFastMode);
        if (mode == V1_MODE) {
            if (isMultiThread) {
                generateV1ChannelApkMultiThread(baseApk, channelList, outputDir, isFastMode);
            } else {
                generateV1ChannelApk(baseApk, channelList, outputDir, isFastMode);
            }
        } else if (mode == V2_MODE || mode == V3_MODE) {
            generateChannelApk(baseApk, channelList, outputDir, isFastMode);
        } else {
            throw new IllegalStateException("not support channel package mode:" + mode);
        }
    }

    /**
     * 首先判断是否有V2签名，如果有就往V2签名段中写入渠道信息，否则就判断V1，如果有V1就在apk注释添加渠道
     * 第三种情况就是没有准确的签名
     *
     * @param baseApk
     * @return
     * @link https://source.android.com/security/apksigning/v2
     */
    private static int judgeChannelPackageMode(File baseApk) throws Exception {
        if (baseApk == null || !baseApk.exists() || !baseApk.isFile()) {
            throw new IOException("not find base apk");
        }
        System.out.println("start check apk signature mode...");
        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(baseApk);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        System.out.println("Verified using v1 scheme (JAR signing): " + result.isVerifiedUsingV1Scheme());
        System.out.println("Verified using v2 scheme (APK Signature Scheme v2): " + result.isVerifiedUsingV2Scheme());
        System.out.println("Verified using v3 scheme (APK Signature Scheme v3): " + result.isVerifiedUsingV3Scheme());
        if (result.isVerifiedUsingV3Scheme()) {
            return V3_MODE;
        } else if (result.isVerifiedUsingV2Scheme()) {
            return V2_MODE;
        } else if (result.isVerifiedUsingV1Scheme()) {
            return V1_MODE;
        } else {
            return DEFAULT_MODE;
        }
    }

    /**
     * 如果目标是以.apk结尾则使用该路径，否则使用默认命名的路径
     *
     * @param dirOrApkPath 文件目录或指定的APK位置
     * @param apkName      原始apk名
     * @param channel      渠道号
     * @return
     */
    private static File getDestinationFile(File dirOrApkPath, String apkName, String channel) {
        if (dirOrApkPath.getAbsolutePath().endsWith(".apk")) {
            return dirOrApkPath;
        } else {
            return new File(dirOrApkPath, getChannelApkName(apkName, channel));
        }
    }

    /**
     * V1方式写入渠道
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV1ChannelApk(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        if (!ChannelReader.containV1Signature(baseApk)) {
            System.out.println("File " + baseApk.getName() + " not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0");
            return;
        }

        String apkName = baseApk.getName();
        //判断基础包是否已经包含渠道信息

        String testChannel = ChannelReader.getChannelByV1(baseApk);
        if (!testChannel.isEmpty()) {
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + " has a channel : " + testChannel + ", only ignore");
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("------ File " + apkName + " generate v1 channel apk  , begin ------");

        try {
            for (String channel : channelList) {
                File destFile = getDestinationFile(outputDir, apkName, channel);
                System.out.println("generatedV1ChannelApk , channel = " + channel + " , apkChannelName = " + destFile.getName());
                copyFileUsingNio(baseApk, destFile);
                ChannelWriter.addChannelByV1(destFile, channel);
                if (!isFastMode) {
                    //1. verify channel info
                    if (ChannelReader.verifyChannelByV1(destFile, channel)) {
                        System.out.println("generateV1ChannelApk , " + destFile + " add channel success");
                    } else {
                        throw new RuntimeException("generateV1ChannelApk , " + destFile + " add channel failure");
                    }
                    //2. verify signature
                    if (VerifyApk.verifySignature(destFile)) {
                        System.out.println("generateV1ChannelApk , after add channel , " + destFile + " verify success");
                    } else {
                        throw new RuntimeException("generateV1ChannelApk , after add channel , " + destFile + " verify failure");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("generateV1ChannelApk error , please check it and fix it ，and that you should generate all V1 Channel Apk again!");
            e.printStackTrace();
        }

        System.out.println("------ File " + apkName + " generate v1 channel apk , end ------");
        long cost = System.currentTimeMillis() - startTime;
        System.out.println("------ total " + channelList.size() + " channel apk , cost : " + cost + " ------");
    }

    /**
     * V1方式写入渠道 (多线程)
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateV1ChannelApkMultiThread(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        if (!ChannelReader.containV1Signature(baseApk)) {
            System.out.println("File " + baseApk.getName() + " not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0");
            return;
        }

        String apkName = baseApk.getName();
        //判断基础包是否已经包含渠道信息
        String testChannel = ChannelReader.getChannelByV1(baseApk);
        if (testChannel != null) {
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + " has a channel : " + testChannel + ", only ignore");
            return;
        }

        long startTime = System.currentTimeMillis();
        System.out.println("------ File " + apkName + " generate v1 channel apk  , begin ------");
        //多线程生成渠道包
        ThreadManager.getInstance().generateV1Channel(baseApk, channelList, outputDir, isFastMode);
        ThreadManager.getInstance().destory();
        System.out.println("------ File " + apkName + " generate v1 channel apk , end ------");
        long cost = System.currentTimeMillis() - startTime;
        System.out.println("------ total " + channelList.size() + " channel apk , cost : " + cost + " ------");
    }

    /**
     * V2/V3签名方式写入渠道
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    private static void generateChannelApk(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        String apkName = baseApk.getName();
        long startTime = System.currentTimeMillis();
        System.out.println("------ File " + apkName + " generate channel apk  , begin ------");

        try {
            // 获取Apk的各部分片段信息
            ApkSectionInfo apkSectionInfo = IdValueWriter.getApkSectionInfo(baseApk, false);
            for (String channel : channelList) {
                File destFile = getDestinationFile(outputDir, apkName, channel);
                System.out.println("generatedChannelApk , channel = " + channel + " , apkChannelName = " + destFile.getName());
                if (apkSectionInfo.lowMemory) {
                    copyFileUsingNio(baseApk, destFile);
                }
                ChannelWriter.addChannelByV2(apkSectionInfo, destFile, channel);
                if (!isFastMode) {
                    //1. verify channel info
                    if (ChannelReader.verifyChannelByV2(destFile, channel)) {
                        System.out.println("generatedChannelApk destFile（" + destFile + "）add channel success");
                    } else {
                        throw new RuntimeException("generatedChannelApk destFile（ " + destFile + "） add channel failure");
                    }

                    //2. 检查生成的渠道包是否是合法的APK文件
                    if (VerifyApk.verifySignature(destFile)) {
                        System.out.println("generatedChannelApk , after add channel ,  " + destFile + " verify success");
                    } else {
                        throw new RuntimeException("generatedChannelApk , after add channel , " + destFile + " verify failure");
                    }
                }
                apkSectionInfo.rewind();
                if (!isFastMode) {
                    apkSectionInfo.checkEocdCentralDirOffset();
                }
            }
        } catch (Exception e) {
            System.out.println("generatedChannelApk error , please check it and fix it ，and that you should generate all  Channel Apk again!");
            e.printStackTrace();
        }

        System.out.println("------ File " + apkName + " generate channel apk , end ------");
        long cost = System.currentTimeMillis() - startTime;
        System.out.println("------ total " + channelList.size() + " channel apk , cost : " + cost + " ------");
    }


    public static boolean removeChannel(File channelApk) {
        try {
            int mode = judgeChannelPackageMode(channelApk);
            if (mode == V1_MODE) {
                ChannelWriter.removeChannelByV1(channelApk);
                return true;
            } else if (mode == V2_MODE || mode == V3_MODE) {
                ChannelWriter.removeChannelByV2(channelApk, true);
                return true;
            } else {
                System.out.println("not have precise channel package mode");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        return false;
    }

    /**
     * 配置添加渠道信息之后的apk名称
     *
     * @param baseApkName
     * @param channel
     * @return
     */
    public static String getChannelApkName(String baseApkName, String channel) {
        if (baseApkName.contains("base")) {
            return baseApkName.replace("base", channel);
        }
        return channel + "-" + baseApkName;
    }


    /**
     * 通过调用cp命令来加快文件复制
     *
     * @param baseApk
     * @param destApk
     */
    private static void copyFileByCp(File baseApk, File destApk) {
        try {
            String command = "cp " + baseApk.getAbsolutePath() + " " + destApk.getAbsolutePath();
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 使用文件流copy文件
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyFileUsingStream(File source, File dest) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    public static void copyFileUsingNio(File source, File dest) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        if (source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            System.out.println("No copying induces same absolute path, dest: " + dest.getAbsolutePath());
            return;
        }
        try {
            inStream = new FileInputStream(source);
            outStream = new FileOutputStream(dest, false);
            in = inStream.getChannel();
            out = outStream.getChannel();
            long pos = 0;
            long count = in.size();
            long copied = 0;
            while (copied != count) {
                // 特别注意，使用transferTo最多只能复制int.MAX_VALUE个字节(约2GB)，因此需要多次操作
                copied += in.transferTo(pos + copied, count - copied, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * 读取渠道文件
     *
     * @param channelFile
     * @return
     */
    public static List<String> readChannelFile(File channelFile) {
        ArrayList<String> channelList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(channelFile));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    String[] array = line.split("#");
                    if (array != null && array.length > 0 && array[0] != null && array[0].trim().length() > 0) {
                        channelList.add(array[0].trim());
                    } else {
                        System.out.println("skip invalid channel line : " + line);
                    }
                }
                return channelList;
            } finally {
                br.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
