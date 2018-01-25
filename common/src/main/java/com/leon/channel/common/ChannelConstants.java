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

/**
 * Created by leontli on 17/1/17.
 */

public class ChannelConstants {
    public static final int CHANNEL_BLOCK_ID = 0x881155ff;
    public static final String CONTENT_CHARSET = "UTF-8";
    static final int SHORT_LENGTH = 2;
    static final byte[] V1_MAGIC = new byte[]{0x6c, 0x74, 0x6c, 0x6f, 0x76, 0x65, 0x7a, 0x68}; //ltlovezh
}
