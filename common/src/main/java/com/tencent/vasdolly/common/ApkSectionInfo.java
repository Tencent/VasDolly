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

package com.tencent.vasdolly.common;

import com.tencent.vasdolly.common.apk.ApkSigningBlockUtils;
import com.tencent.vasdolly.common.apk.SignatureNotFoundException;

import java.nio.ByteBuffer;

/**
 * Created by leontli on 17/1/18.
 * APK info
 */

public class ApkSectionInfo {
    // 当设备需要复制Apk内容到内存Buffer时支持的最大大小，由于ByteBuffer仅支持int，因此最大不可以大于int的长度
    static int COPY_CONTENT_MAX_SIZE = 512 * 1024 * 1024; // 最大申请 512MB Buffer
    // 是否复制Apk内容到内存中（主要是为了提升写入渠道包的速度）lowMemory=false表示复制apk
    public boolean lowMemory = false;
    public long apkSize;
    public Pair<ByteBuffer, Long> contentEntry;
    // apk签名块
    public Pair<ByteBuffer, Long> apkSigningBlock;
    public Pair<ByteBuffer, Long> centralDir;
    public Pair<ByteBuffer, Long> eocd;

    public void checkParamters() throws SignatureNotFoundException {
        if ((!lowMemory && contentEntry == null) || apkSigningBlock == null || centralDir == null || eocd == null) {
            throw new RuntimeException("ApkSectionInfo paramters is not valid : " + toString());
        }

        boolean result = (lowMemory || (contentEntry.getSecond() == 0L && contentEntry.getFirst().remaining() == apkSigningBlock.getSecond()))
                && (apkSigningBlock.getFirst().remaining() + apkSigningBlock.getSecond() == centralDir.getSecond())
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
        if (apkSigningBlock != null) {
            apkSigningBlock.getFirst().rewind();
        }
        if (centralDir != null) {
            centralDir.getFirst().rewind();
        }
        if (eocd != null) {
            eocd.getFirst().rewind();
        }
    }

    public void checkEocdCentralDirOffset() throws SignatureNotFoundException {
        //通过eocd找到中央目录的偏移量
        long centralDirOffset = ApkSigningBlockUtils.getCentralDirOffset(eocd.getFirst(), eocd.getSecond());
        if (centralDirOffset != centralDir.getSecond()) {
            throw new RuntimeException("CentralDirOffset mismatch , EocdCentralDirOffset : " + centralDirOffset + ", centralDirOffset : " + centralDir.getSecond());
        }
    }

    @Override
    public String toString() {
        return "lowMemory : " + lowMemory + "\n apkSize : " + apkSize + "\n contentEntry : " + contentEntry + "\n schemeV2Block : " + apkSigningBlock + "\n centralDir : " + centralDir + "\n eocd : " + eocd;
    }
}
