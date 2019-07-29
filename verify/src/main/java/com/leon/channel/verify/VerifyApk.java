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

package com.leon.channel.verify;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.android.apksig.zip.ZipFormatException;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by leontli on 17/6/11.
 */

public class VerifyApk {
    /**
     * verify V2 signature
     *
     * @param inputApk
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws ZipFormatException
     * @throws ApkFormatException
     */
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

    /**
     * verify V1 signature
     *
     * @param inputApk
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws ZipFormatException
     * @throws ApkFormatException
     */
    public static boolean verifyV1Signature(File inputApk) throws NoSuchAlgorithmException, IOException, ZipFormatException, ApkFormatException {
        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(inputApk);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        boolean verified = result.isVerified();
        System.out.println("verified : " + verified);
        if (verified) {
            System.out.println("Verified using v1 scheme (JAR signing): " + result.isVerifiedUsingV1Scheme());
            System.out.println("Verified using v2 scheme (APK Signature Scheme v2): " + result.isVerifiedUsingV2Scheme());
            if (result.isVerifiedUsingV1Scheme()) {
                return true;
            }
        }
        return false;
    }


}

