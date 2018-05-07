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

import java.nio.ByteBuffer;

/**
 * Created by leontli on 17/1/18.
 * APK info
 */

public class ApkSectionInfo {
    public boolean lowMemory = false;
    public long apkSize;
    public Pair<ByteBuffer, Long> contentEntry;
    public Pair<ByteBuffer, Long> schemeV2Block;
    public Pair<ByteBuffer, Long> centralDir;
    public Pair<ByteBuffer, Long> eocd;

    public void checkParamters() throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        if ((!lowMemory && contentEntry == null) || schemeV2Block == null || centralDir == null || eocd == null) {
            throw new RuntimeException("ApkSectionInfo paramters is not valid : " + toString());
        }

        boolean result = (lowMemory ? true : (contentEntry.getSecond() == 0l && contentEntry.getFirst().remaining() + contentEntry.getSecond() == schemeV2Block.getSecond()))
                && (schemeV2Block.getFirst().remaining() + schemeV2Block.getSecond() == centralDir.getSecond())
                && (centralDir.getFirst().remaining() + centralDir.getSecond() == eocd.getSecond())
                && (eocd.getFirst().remaining() + eocd.getSecond() == apkSize);

        if (!result) {
            throw new RuntimeException("ApkSectionInfo paramters is not valid : " + toString());
        }
        checkEocdCentralDirOffset();
    }

    public void rewind() {
        if (contentEntry != null) {
            contentEntry.getFirst().rewind();
        }
        if (schemeV2Block != null) {
            schemeV2Block.getFirst().rewind();
        }
        if (centralDir != null) {
            centralDir.getFirst().rewind();
        }
        if (eocd != null) {
            eocd.getFirst().rewind();
        }
    }

    public void checkEocdCentralDirOffset() throws ApkSignatureSchemeV2Verifier.SignatureNotFoundException {
        //通过eocd找到中央目录的偏移量
        long centralDirOffset = ApkSignatureSchemeV2Verifier.getCentralDirOffset(eocd.getFirst(), eocd.getSecond());
        if (centralDirOffset != centralDir.getSecond()) {
            throw new RuntimeException("CentralDirOffset mismatch , EocdCentralDirOffset : " + centralDirOffset + ", centralDirOffset : " + centralDir.getSecond());
        }
    }

    @Override
    public String toString() {
        return "lowMemory : " + lowMemory + "\n apkSize : " + apkSize + "\n contentEntry : " + contentEntry + "\n schemeV2Block : " + schemeV2Block + "\n centralDir : " + centralDir + "\n eocd : " + eocd;
    }
}
