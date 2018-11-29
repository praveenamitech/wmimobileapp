package com.example.bhanu.hdevices.protocols;

import com.example.bhanu.hdevices.CustomUtils;

/**
 * Created by bhanu on 16/2/18.
 *
 */

public class Commands {
    // genus meter commands
    public static final byte gen_first[] = CustomUtils.hexStringToByteArray("013F4D54504CAAAAAA86");
    public static final byte gen_second[] = CustomUtils.hexStringToByteArray("014244EFEFAAAAAAAAF4");
    public static final byte gen_third[] = CustomUtils.hexStringToByteArray("01720101AAAAAAAAAA3A");
    public static final byte gen_fourth[] = CustomUtils.hexStringToByteArray("017201CBAAAAAAAAAA70");

    // dlms commands
    public static final byte dlms_first[] = CustomUtils.hexStringToByteArray("7E A0 0A 00 02 04 01 41 53 2E 16 7E"); // verified
    public static final byte dlms_second[] = CustomUtils.hexStringToByteArray("7E A0 0A 00 02 04 01 41 93 22 D0 7E"); // verified
    public static final byte dlms_third[] = CustomUtils.hexStringToByteArray("7EA047000204014110F88FE6E6006036A1090607608574050801018A0207808B0760857405080201AC0A80084142434430303031BE10040E01000000065F1F040000121A270F580B7E"); // verified
    public static final byte dlms_fourth[] = CustomUtils.hexStringToByteArray("7E A0 1C 00 02 04 014132A589E6E600C0018100080000010000FF0200 65 D7 7E"); // verified
    public static final byte dlms_fifth[] = CustomUtils.hexStringToByteArray("7EA01C000204014154958FE6E600C0018100010000600100FF0200 8C 6D 7E"); // verified
    public static final byte dlms_sixth[] = CustomUtils.hexStringToByteArray("7EA00A000204014153 2E 16 7E"); // verified
    public static final byte dlms_seventh[] = CustomUtils.hexStringToByteArray("7EA02100020401419323C5818012050180060180070400000001080400000001 53 3B 7E"); // verified
    public static final byte dlms_eighth[] = CustomUtils.hexStringToByteArray("7EA047000204014110F88FE6E6006036A1090607608574050801018A0207808B0760857405080201AC0A80084142434430303031BE10040E01000000065F1F040000121A270F580B7E");
    public static final byte dlms_ninth[] = CustomUtils.hexStringToByteArray("7EA01C000204014132A589E6E600C00181000701005E5B06FF0300D6DD7E");

    public static final byte dlms_ninth_ack[] = CustomUtils.hexStringToByteArray("7EA00A0002040141513C357E");


    //lnt meter commands
    public static final byte lnt_cmd_first[] = { 0x3a }; // send this command multiple times if no response
    public static final byte lnt_cmd_second[] = { 0x2f, 0x3f, 0x21, 0x0d, 0x0a };
    public static final byte lnt_cmd_third[] = { 0x06, 0x30, 0x34, 0x30, 0x0d, 0x0a };
    public static final byte lnt_resp_akg1[] = { 0x06 };
    public static final byte lnt_resp_akg2[] = { 0x15 };

    //l + G meter commands
    public static final byte lpg_cmd_first[] = { 0x2f, 0x3f, 0x21, 0x0d, 0x0a };
    public static final byte lpg_cmd_second[] = { 0x06, 0x30, 0x35, 0x30, 0x0d, 0x0a };
    public static final byte lpg_resp_akg1[] = { 0x06 };
    public static final byte lpg_resp_akg2[] = { 0x15 };

    // secure meter commands
    public static final byte sec_cmd_first[] = CustomUtils.hexStringToByteArray("530D");
    public static final byte sec_cmd_second[] = { (byte)0x80 };
    public static final byte sec_cmd_third[] = { (byte)0xf1 }; // this command is sent repeatedly
    public static final byte sec_cmd_fourth[] = { (byte)0xfc };

    public static final byte sec_resp_second[] = { (byte)0xfc };
    public static final byte sec_resp_fourth[] = { (byte)0xff };

    //lnt2 meter commands
    public static final byte lnt2_cmd_first[] = { 0x3a }; // send this command multiple times if no response
    public static final byte lnt2_cmd_second[] = { 0x2f, 0x3f, 0x21, 0x0d, 0x0a };
    public static final byte lnt2_cmd_third[] = { 0x06, 0x30, 0x35, 0x33, 0x0d, 0x0a, 0x42};
    public static final byte lnt2_resp_akg1[] = { 0x06 };
    public static final byte lnt2_resp_akg2[] = { 0x15 };

    //lnt_legacy meter commands
    public static final byte lntlegacy_cmd_first[] = { 0x3a }; // send this command multiple times if no response
    public static final byte lntlegacy_cmd_second[] = { 0x3f, 0x21, 0x0d, 0x0a };
    public static final byte lntlegacy_cmd_third[] = { 0x06, 0x30, 0x34, 0x33, 0x0d, 0x0a, 0x42 };
    public static final byte lntlegacy_resp_akg1[] = { 0x06 };
    public static final byte lntlegacy_resp_akg2[] = { 0x06 };
}
