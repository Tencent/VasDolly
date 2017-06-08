package com.leon.channel.command;

import com.leon.channel.common.V1SchemeUtil;
import com.leon.channel.common.V2SchemeUtil;

import java.io.File;

/**
 * Created by zys on 17-6-8.
 */

public class Main {

    public static final String V1 = "V1";
    public static final String V2 = "V2";
    public static final String V1_V2 = "V1_V2";

    public static void main(String[] args) {
        String cmdFilePath = "-path";
        String cmdHelp = "-help";

        String help = "用法：java -jar ApkChannelPackage.jar [" + cmdFilePath + "] [arg]"
                + "\n" + cmdFilePath + "        APK的文件路径"
                + "\n例如："
                + "\n java -jar ApkChannelPackage.jar -path /home/user/test.apk";

        if (args.length == 0 || args[0] == null || args[0].trim().length() == 0) {
            System.out.print(help);
        } else if (args.length > 0) {
            if (args.length == 1 && args[0].trim().equals(cmdHelp)) {    //帮助
                System.out.print(help);
            } else if (args[0].trim().equals(cmdFilePath)) {//获取签名方式
                String filePath = args[1].trim();
                File file = new File(filePath);
                if (!file.exists()) {
                    System.out.print("Apk file does not exist!");
                    return;
                } else if (file.isDirectory()) {
                    System.out.print("The file path cannot be a directory1");
                    return;
                }
                System.out.print((getSignScheme(file)));
            }
        }
    }


    /**
     * 获取APK的签名方式
     *
     * @param file
     * @return V1, V2, V1_V2
     */
    private static String getSignScheme(File file) {

        if (V2SchemeUtil.containV2Signature(file,true)) {
            //如果有V2签名段，并且没有CERT.SF，那么一定是仅仅V2签名,否则就是V1和V2一起签名的
            if (!V1SchemeUtil.containV1Signature(file)) {
                return V2;
            } else {
                return V1_V2;
            }
        } else if (V1SchemeUtil.containV1Signature(file)) {//如果没有V2签名段，并且有CERT.SF，那么一定是仅仅V1签名
            return V1;
        }else {
            throw new IllegalStateException("Apk was not signed");
        }
    }
}

