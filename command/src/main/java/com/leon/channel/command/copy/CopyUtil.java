package com.leon.channel.command.copy;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zys on 17-6-13.
 */
public class CopyUtil {

    private static final int THREAD_NUMBER = 4;

    public interface CopyFileListener{
        void onSuccess();
        void onFailed(Exception e);
    }

    public static void copyFile(File sourceFile, File destFile,CopyFileListener listener) {
        if (sourceFile == null) {
            return;
        }
        if (!sourceFile.exists()) {
            System.out.println("Source File is not exists");
            return;
        }
        if (sourceFile.isDirectory()) {
            System.out.println("Source File is a directory");
            return;
        }
        if (destFile.exists()) {
            destFile.delete();
        }
        CountDownLatch sCountDOwnLatch = new CountDownLatch(THREAD_NUMBER);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBER);
        for (int i = 0; i < THREAD_NUMBER; i++) {
            executor.execute(new CopyRunnable(sourceFile, destFile, i, THREAD_NUMBER, sCountDOwnLatch));
        }
        try {
            sCountDOwnLatch.await();
        } catch (InterruptedException e) {
            if (listener!=null) {
                listener.onFailed(e);
            }
        }
        //结束线程池
        executor.shutdown();
        //回调copy完成
        if (listener!=null) {
            listener.onSuccess();
        }
    }

}
