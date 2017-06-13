package com.leon.channel.command.copy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zys on 17-6-13.
 */
public class CopyRunnable implements Runnable {

    private File mSourceFile;
    private File mDestFile;
    private int mThreadNo;
    private int mThreadNum;

    private int bufferSize = 4096;

    private CountDownLatch mCountDownLatch;

    public CopyRunnable(File mSourceFile, File mDestFile, int mThreadNo,
                        int mThreadNum,CountDownLatch countDownLatch) {
        this.mSourceFile = mSourceFile;
        this.mDestFile = mDestFile;
        this.mThreadNo = mThreadNo;
        this.mThreadNum = mThreadNum;
        this.mCountDownLatch=countDownLatch;
    }



    @Override
    public void run() {
        FileInputStream fis = null;
        RandomAccessFile raf = null;
        try {
            fis = new FileInputStream(mSourceFile);
            raf = new RandomAccessFile(mDestFile, "rw");

            //每次写入的块大小
            long blockSize = mSourceFile.length() / mThreadNum;
            //开始读取的指针
            long startPoint = blockSize * mThreadNo;
            byte[] buffer = new byte[bufferSize];
            //从startPoint的地方开始读取
            fis.skip(startPoint);
            raf.seek(startPoint);

            int currentSize = 0;
            int totalSize = 0;

            while ((currentSize = fis.read(buffer)) >= 0 && totalSize <= blockSize) {
                totalSize += currentSize;
                raf.write(buffer, 0, currentSize);
            }
            fis.close();
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }finally {
            mCountDownLatch.countDown();
        }
    }
}
