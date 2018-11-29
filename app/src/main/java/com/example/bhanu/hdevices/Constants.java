package com.example.bhanu.hdevices;

import java.util.HashMap;

/**
 * Created by bhanu on 15/2/18.
 */

public class Constants {
    public static final String LICENSE_SERVER_URL = "http://183.82.97.160:3010";
    //public static final String LICENSE_SERVER_URL = "http://34.213.64.241:3010";

    public static final int ASCII = 100;
    public static final int HEX = 101;

    // status
    public static final String NA = "NA";
    public static final String PROGRESS = "PROGRESS";
    public static final String DONE = "DONE";
    public static final String FAILED = "FAILED";

    public static final int SECURE = 1;
    public static final int LNT = 2;
    public static final int GENUS = 3;
    public static final int SECURE_DLMS = 50;
    public static final int LPG = 6;
    public static final int LNT_DLMS = 51;
    public static final int LNT2 = 5;
    public static final int LNT_LEGACY = 7;


    public static final String SECURE_STR = "SECURE"; //1ph
    public static final String SECURE_DLMS_STR = "SECURE_DLMS"; //3ph
    public static final String GENUS_STR = "GENUS"; //1ph
    public static final String LNT_STR = "LNT";  //1ph  b.rate 4800
    public static final String LPG_STR = "L+G"; //1ph
    public static final String LNT_DLMS_STR = "LNT_DLMS"; //3ph
    public static final String LNT2_STR = "LNT2";  //1ph b.rate 9600
    public static final String LNT_lEGACY_STR = "LNT_LEGACY"; //3ph


    public static final int MIN_DELAY = 500;
    public static final int NORM_DELAY = 1000;
    public static final int MAX_DELAY = 2000;

    public static final String DATETIME_FORMAT = "dd-MM-yy HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";

    public static final int UNDEFINED_TABLE_ROWS_LIMIT = 100;
}
