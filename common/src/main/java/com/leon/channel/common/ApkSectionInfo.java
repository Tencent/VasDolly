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
