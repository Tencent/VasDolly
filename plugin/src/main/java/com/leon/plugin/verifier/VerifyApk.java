package com.leon.plugin.verifier;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.android.apksig.zip.ZipFormatException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class VerifyApk {

    public static boolean verifyV2Signature(File inputApk) throws NoSuchAlgorithmException, IOException, ZipFormatException, ApkFormatException {
        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(inputApk);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        boolean verified = result.isVerified();
        System.out.println("verified : " + verified);
        if (verified) {
            System.out.println("Verified using v1 scheme (JAR signing): " + result.isVerifiedUsingV1Scheme());
            System.out.println("Verified using v2 scheme (APK Signature Scheme v2): " + result.isVerifiedUsingV2Scheme());
            if (result.isVerifiedUsingV2Scheme()) {
                return true;
            }
        }
        return false;
    }
}