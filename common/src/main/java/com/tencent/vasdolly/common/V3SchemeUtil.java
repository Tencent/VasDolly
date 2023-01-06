package com.tencent.vasdolly.common;

import com.tencent.vasdolly.common.apk.SignatureNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class V3SchemeUtil {
    public static final int APK_SIGNATURE_SCHEME_V3_BLOCK_ID = 0xf05368c0;

    /**
     * judge whether apk contain v3 signature block
     *
     * @param apk
     * @return
     */
    public static boolean containV3Signature(File apk) {
        try {
            ByteBuffer apkSigningBlock = V2SchemeUtil.getApkSigningBlock(apk);
            Map<Integer, ByteBuffer> idValueMap = V2SchemeUtil.getAllIdValue(apkSigningBlock);
            if (idValueMap.containsKey(APK_SIGNATURE_SCHEME_V3_BLOCK_ID)) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SignatureNotFoundException e) {
            System.out.println("APK : " + apk.getAbsolutePath() + " not have apk v3 signature block");
        }

        return false;
    }
}
