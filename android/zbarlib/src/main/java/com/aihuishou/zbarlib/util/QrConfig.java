package com.aihuishou.zbarlib.util;

import android.graphics.Color;

/**
 * created by Administrator
 * on 2018/12/20
 * description
 */
public class QrConfig {
    public static final int LINE_FAST = 1000;
    public static final int LINE_MEDIUM = 3000;
    public static final int LINE_SLOW = 5000;


    public  int CORNER_COLOR = Color.parseColor("#ff5f00");
    public  int LINE_COLOR = Color.parseColor("#ff5f00");

    public  int TITLE_BACKGROUND_COLOR = Color.parseColor("#ff5f00");
    public  int TITLE_TEXT_COLOR = Color.parseColor("#ffffff");

    public static final int TYPE_QRCODE = 1;//扫描二维码
    public  static final int TYPE_BARCODE = 2;//扫描条形码（UPCA）
    public  static final int TYPE_ALL = 3;//扫描全部类型码

    public static final int SCANVIEW_TYPE_QRCODE = 1;//二维码框
    public static final int SCANVIEW_TYPE_BARCODE = 2;//条形码框
}
