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

import java.nio.ByteBuffer;

/**
 * Created by leontli on 17/1/18.
 * APK info
 */

public class ApkSectionInfo {
    public Pair<ByteBuffer, Long> mContentEntry;
    public Pair<ByteBuffer, Long> mSchemeV2Block;
    public Pair<ByteBuffer, Long> mCentralDir;
    public Pair<ByteBuffer, Long> mEocd;


    @Override
    public String toString() {
        return "mContentEntry : " + mContentEntry + " , mSchemeV2Block : " + mSchemeV2Block + " , mCentralDir : " + mCentralDir + " , mEocd : " + mEocd;
    }
}
