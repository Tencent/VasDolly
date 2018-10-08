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

package com.leon.channel.command;

import com.leon.channel.verify.VerifyApk;
import com.leon.channel.reader.ChannelReader;
import com.leon.channel.writer.ChannelWriter;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by leontli on 2017/7/2.
 */

public class ThreadManager {
    private static final int CPU_CORE;
    private ExecutorService mExecutorService;
    private AtomicInteger mChannelSuccessNum;
    private Vector<String> mChannelSuccessList = new Vector<>();
    private int mChannelNum;
    CountDownLatch mChannelountDownLatch;
    private volatile static ThreadManager mInstance;

    static {
        int core = Runtime.getRuntime().availableProcessors();
        if (core <= 2) {
            core = 2;
        }
        CPU_CORE = core;
        System.out.println("CPU_CORE = " + CPU_CORE);
    }

    private ThreadManager() {
        mExecutorService = Executors.newFixedThreadPool(CPU_CORE, new ChannelThreadFactory("channel"));
    }

    public static ThreadManager getInstance() {
        if (mInstance == null) {
            synchronized (ThreadManager.class) {
                if (mInstance == null) {
                    mInstance = new ThreadManager();
                }
            }
        }
        return mInstance;
    }


    public void destory() {
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mInstance = null;
        }
    }

    /**
     * 多线程生成渠道包
     *
     * @param baseApk
     * @param channelList
     * @param outputDir
     */
    public void generateV1Channel(File baseApk, List<String> channelList, File outputDir, boolean isFastMode) {
        String apkName = baseApk.getName();
        setChannelNum(channelList.size());
        for (String channel : channelList) {
            String apkChannelName = Util.getChannelApkName(apkName, channel);
            System.out.println("generateV1Channel , channel = " + channel + " , apkChannelName = " + apkChannelName);
            File destFile = new File(outputDir, apkChannelName);
            mExecutorService.execute(new ChanndelRunnable(baseApk, destFile, channel, isFastMode));
        }
        try {
            mChannelountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mChannelSuccessNum.get() != mChannelNum) {
            System.out.println("Fail , need generate channel num : " + mChannelNum + " , but success only num : " + mChannelSuccessNum.get() + " , success apk list : " + mChannelSuccessList);
        } else {
            System.out.println("Success , total generate channel num : " + mChannelNum + " , APK list : " + mChannelSuccessList);
        }
    }

    private void setChannelNum(int channelNum) {
        this.mChannelSuccessList.clear();
        this.mChannelNum = channelNum;
        this.mChannelSuccessNum = new AtomicInteger(0);
        this.mChannelountDownLatch = new CountDownLatch(channelNum);
    }


    class ChanndelRunnable implements Runnable {
        File mBaseApk;
        File mDestFile;
        String mChannel;
        boolean mIsFastMode;

        public ChanndelRunnable(File baseFile, File destfile, String channel, boolean isFastMode) {
            this.mBaseApk = baseFile;
            this.mDestFile = destfile;
            this.mChannel = channel;
            this.mIsFastMode = isFastMode;
        }

        @Override
        public void run() {
            String threadName = Thread.currentThread().getName();
            try {
                Util.copyFileUsingNio(mBaseApk, mDestFile);
                ChannelWriter.addChannelByV1(mDestFile, mChannel);
                if (mIsFastMode) {
                    mChannelSuccessNum.incrementAndGet();//表示生成渠道包成功
                    mChannelSuccessList.add(mDestFile.getName());
                    System.out.println("Thread : " + threadName + " , generateV1Channel , " + mDestFile + " fastMode success");
                } else {
                    //1. verify channel info
                    if (ChannelReader.verifyChannelByV1(mDestFile, mChannel)) {
                        System.out.println("Thread : " + threadName + " , generateV1Channel , " + mDestFile + " add channel success");
                    } else {
                        throw new RuntimeException("Thread : " + threadName + " , generateV1Channel , " + mDestFile + " add channel failure");
                    }
                    //2. verify v1 signature
                    if (VerifyApk.verifyV1Signature(mDestFile)) {
                        mChannelSuccessNum.incrementAndGet();//表示生成渠道包成功
                        mChannelSuccessList.add(mDestFile.getName());
                        System.out.println("Thread : " + threadName + " , generateV1Channel , after add channel , " + mDestFile + " v1 verify success");
                    } else {
                        throw new RuntimeException("Thread : " + threadName + " , generateV1Channel , after add channel , " + mDestFile + " v1 verify failure");
                    }
                }
            } catch (Exception e) {
                System.out.println("Thread : " + threadName + " , generateV1Channel , error , please check it");
                e.printStackTrace();
            } finally {
                mChannelountDownLatch.countDown();
            }
        }
    }

    static class ChannelThreadFactory implements ThreadFactory {
        static int count = 0;
        public String name;

        public ChannelThreadFactory(String name) {
            super();
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name + "_" + (++count));
        }
    }


}
