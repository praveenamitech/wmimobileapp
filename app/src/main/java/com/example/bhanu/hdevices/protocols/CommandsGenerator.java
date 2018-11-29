package com.example.bhanu.hdevices.protocols;

import com.example.bhanu.hdevices.CustomUtils;

public class CommandsGenerator {

    final int TRUE = 1;
    final int FALSE = 0;

    final int P = 0x8408;
    int destaddLH,destaddLL,sourceadd,len,hex,dummy,i_gprs,psw_chk,Atribute,DLMS_COM,DLMS_COMH,DLMS_COML;
    int byte7flg, Billflg;
    int BillBLOCK_NO = 0;
    int dlms_temp6[] = new int[6];
    int Date_Time[] = new int[] { 0,0,1,0,0,255 };

    int[] Meter_SLNO = new int[] { 0,0,96,1,0,255 };
    int[] MFC_Name = new int[] { 0,0,96,1,1,255 };
    int[] Meter_Firm_Ver = new int[] { 1,0,0,2,0,255 };
    int[] Meter_CT = new int[] { 1,0,0,4,2,255 };
    int[] Meter_PT = new int[] { 1,0,0,4,3,255 };

    int i, fcs16;

    int[] mtrbuffer = new int[100];
    int DLMS_len;

    //    int RdestaddLH = 0x02;
//    int RdestaddLL = 0x03;
    int Rsourceadd = 0x41;

    int RdestaddLH = 0x04;
    int RdestaddLL = 0x01;

    public int bilack = 0;
    public int block_ack = 0;
    int NextBlock = 0;
    int blck_value = 0;

    final int LNT_int[] = new int[] { 0x81,0x80,0x12,0x05,0x01,0x80,0x06,0x01,0x80,0x07,0x04,0x00,0x00,0x00,0x01,0x08,0x04,0x00,0x00,0x00,0x01 };

    final int ATHENT_A[] = new int[] { 0xE6,0xE6,0x00,0x60,0x36,0xA1,0x09,0x06,0x07,0x60,0x85,0x74,0x05,0x08,0x01,0x01,0x8A,0x02,0x07,0x80,0x8B,0x07,0x60,0x85,0x74,0x05,0x08,0x02,0x01,0xAC,0x0A,0x80,0x08 };
    final int PSW_SML[] = new int[] { 0x41,0x42,0x43,0x44,0x30,0x30,0x30,0x31 };
    final int ATHENT_B[] = new int[] { 0xBE,0x10,0x04,0x0E,0x01,0x00,0x00,0x00,0x06,0x5F,0x1F,0x04,0x00,0x00,0x12,0x1A,0x27,0x0F};
    final int Main_19B[]  = new int[] { 0xE6,0xE6,0x00,0xC0,0x01,0x81,0x00 };
    final int Main_16B[]    = new int[] { 0xE6,0xE6,0x00,0xC0,0x02,0x81,0x00,0x00 };//E6 E6 00 C0 02 81 00 00
    final int Scalar_BLProfl[] = new int[] { 1,0,94,91,6,255 };//01005E5B06FF
    final int BillPerdProfil[] = new int[] { 1,0,98,1,0,255 };

    final int ATHENT_ALT[] = new int[] {0xE6,0xE6,0x00,0x60,0x32,0xA1,0x09,0x06,0x07,0x60,0x85,0x74,0x05,0x08,0x01,0x01,0x8A,0x02,0x07,0x80,0x8B,0x07,0x60,0x85,0x74,0x05,0x08,0x02,0x01,0xAC,0x06,0x80,0x04};
    final int ATHENT_BLT[] = new int[] { 0xBE,0x10,0x04,0x0E,0x01,0x00,0x00,0x00,0x06,0x5F,0x1F,0x04,0x00,0x18,0x5F,0x1F,0xFF,0xFF};

    CommandsGenerator() {
        destaddLH = RdestaddLH;
        destaddLL = RdestaddLL;
        sourceadd = Rsourceadd;
    }

    void get_fcs16() {
        int cmp_len;
        cmp_len=0;
        fcs16=0xFFFF;
        while (true)
        {
            fcs16 = (fcs16>>8) ^ fcstab((fcs16 ^ mtrbuffer[cmp_len]) & 0xff);
            cmp_len++;
            if(cmp_len==len)
            {
                fcs16 ^= 0xffff;
                break;
            }
        }
    }

    int fcstab(int fcsval16) {
        int fcstemp;

        fcstemp = fcsval16;

        for (i = 7; i >=0; i--) {
            // fcstemp = fcstemp & 1 ? (fcstemp >> 1) ^ P : fcstemp >> 1;

            if ((fcstemp & 1) != 0) {
                fcstemp = (fcstemp >> 1) ^ P;
            } else {
                fcstemp = fcstemp >> 1;
            }
        }

        fcstemp = fcstemp & 0xFFFF;

        return fcstemp;
    }

    byte[] send_7DLMS(int dlmscom) {
        mtrbuffer = new int[100];
        len=8;
        mtrbuffer[0]=0xA0;
        mtrbuffer[1]=0x0A;
        mtrbuffer[2]=0x00;
        mtrbuffer[3]=0x02;
        mtrbuffer[4]=destaddLH;
        mtrbuffer[5]=destaddLL;
        mtrbuffer[6]=sourceadd;
        mtrbuffer[7]=dlmscom;
        get_fcs16();
        mtrbuffer[8]=fcs16;
        mtrbuffer[9]=fcs16>>8;

        dummy=9;

        for(i=0;i<10;i++) {
            mtrbuffer[dummy+1]=mtrbuffer[dummy];
            dummy--;
        }

        mtrbuffer[0]=0x7E;
        mtrbuffer[11]=0x7E;
        //resp_clear();
        DLMS_len=12;

        return CustomUtils.trim(CustomUtils.int2byte(mtrbuffer));
    }

    byte[] send_Athentication() {

        mtrbuffer = new int[100];
        char dummya;

        mtrbuffer[0] = 0xA0;
        mtrbuffer[1] = 0x47;
        mtrbuffer[2] = 0x00;
        mtrbuffer[3] = 0x02;
        mtrbuffer[4] = destaddLH;
        mtrbuffer[5] = destaddLL;
        mtrbuffer[6] = sourceadd;
        mtrbuffer[7] = 0x10;

        len=8;
        get_fcs16();
        mtrbuffer[8]=fcs16;
        mtrbuffer[9]=fcs16>>8;

        dummya=10;
        for(hex=0;hex<33;hex++)
            mtrbuffer[dummya++]=ATHENT_A[hex];


        for(hex=0;hex<8;hex++)
        {
            mtrbuffer[43+hex]=PSW_SML[hex];
        }

        dummya=51;
        for(hex=0;hex<18;hex++)
            mtrbuffer[dummya++]=ATHENT_B[hex];

        len=69;
        get_fcs16();
        mtrbuffer[69]=fcs16;
        mtrbuffer[70]=fcs16>>8;

        dummya=70;
        for(i=0;i<71;i++)
        {
            mtrbuffer[dummya+1]=mtrbuffer[dummya];
            dummya--;
        }

        mtrbuffer[0]=0x7E;
        mtrbuffer[72]=0x7E;

        DLMS_len = 73;

        return CustomUtils.trim(CustomUtils.int2byte(mtrbuffer));
    }

    byte[] getDLMS_MtrDataCmd(int add) {
        mtrbuffer = new int[100];

        switch(add) {
            case 1:
                for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Date_Time[dummy];  	 psw_chk=0x08; Atribute=0x02; send_main19();	//Date_Time
                break;

            case 2:
                for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Meter_SLNO[dummy];    psw_chk=0x01; Atribute=0x02; 	    send_main19(); //Meter_SLNO
                break;

            case 3:
                for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = MFC_Name[dummy];  	 psw_chk=0x01; Atribute=0x02; send_main19();	//MFC_Name
                break;

            case 4:
                for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Meter_Firm_Ver[dummy];  	 psw_chk=0x01; Atribute=0x02; send_main19();	//Meter_Firm_Ver
                break;

            case 5:
                for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Meter_CT[dummy];  	 psw_chk=0x01; Atribute=0x02; send_main19();	//Meter_CT
                break;

            case 6:
                for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Meter_PT[dummy];  	 psw_chk=0x01; Atribute=0x02; send_main19();	//Meter_PT
                break;
        }

        return CustomUtils.trim(CustomUtils.int2byte(mtrbuffer));
    }

    void send_main19() {
        mtrbuffer = new int[100];
        int dummya;
        mtrbuffer[0] = 0xA0;
        mtrbuffer[1] = 0x1C;
        mtrbuffer[2] = 0x00;
        mtrbuffer[3] = 0x02;
        mtrbuffer[4] = destaddLH;
        mtrbuffer[5] = destaddLL;
        mtrbuffer[6] = sourceadd;

        ger_DLMS_COM();

        mtrbuffer[7] = DLMS_COM;

        len=8;
        get_fcs16();
        mtrbuffer[8]=fcs16;
        mtrbuffer[9]=fcs16>>8;

        dummya=10;
        for(hex=0;hex<7;hex++)
            mtrbuffer[dummya++]=Main_19B[hex];
        mtrbuffer[17]=psw_chk;
        for(hex=0;hex<6;hex++)
            mtrbuffer[++dummya]=dlms_temp6[hex];
        mtrbuffer[24]=Atribute;
        mtrbuffer[25]=0x00;

        len=26;
        get_fcs16();
        mtrbuffer[26]=fcs16;
        mtrbuffer[27]=fcs16>>8;

        dummy=27;
        for(i=0;i<28;i++)
        {
            mtrbuffer[dummy+1]=mtrbuffer[dummy];
            dummy--;
        }

        mtrbuffer[0]=0x7E;
        mtrbuffer[29]=0x7E;


        DLMS_len=30;
    }

    void ger_DLMS_COM() {
        DLMS_COMH = DLMS_COMH + 0x20;
        if(byte7flg==1)
        {
            DLMS_COM = DLMS_COMH + 0x01;
        }
        else
        {
            DLMS_COML = DLMS_COML + 0x02;
            DLMS_COML = DLMS_COML & 0x0F;
            DLMS_COM  = DLMS_COMH + DLMS_COML;
        }
    }

    byte[] Init_LNT() {
        mtrbuffer = new int[100];
        int dummya;

        destaddLH = RdestaddLH;
        destaddLL = RdestaddLL;
        sourceadd = Rsourceadd;

        DLMS_COM = 0x10;
        DLMS_COML = 0x0F & DLMS_COM;
        DLMS_COMH = 0xF0 & DLMS_COM;

        len=6;
        mtrbuffer[0] = 0xA0;
        mtrbuffer[1] = 0x21;
        mtrbuffer[2]=0x00;
        mtrbuffer[3]=0x02;
        mtrbuffer[4] = destaddLH;
        mtrbuffer[5] = destaddLL;
        mtrbuffer[6] = sourceadd;
        mtrbuffer[7] = 0x93;
        len = 8;
        get_fcs16();
        mtrbuffer[8]=fcs16;
        mtrbuffer[9]=fcs16>>8;
        dummya=10;
        for(hex=0;hex<21;hex++)
            mtrbuffer[dummya++]=LNT_int[hex];

        len=31;
        get_fcs16();
        mtrbuffer[31]=fcs16;
        mtrbuffer[32]=fcs16>>8;


        dummy=32;
        for(i=0;i<33;i++)
        {
            mtrbuffer[dummy+1]=mtrbuffer[dummy];
            dummy--;
        }

        mtrbuffer[0]=0x7E;
        mtrbuffer[34]=0x7E;
        DLMS_len=35;

        return CustomUtils.trim(CustomUtils.int2byte(mtrbuffer));
    }

    byte[] SendCmdforBilling(int cmd, int bilack, int block_ack) {
        int flag;

        mtrbuffer = new int[100];

        this.bilack = bilack;
        this.block_ack = block_ack;

        System.out.println("in func");
        if(this.bilack == 0 && this.block_ack == 0) {
            System.out.println("came here");
            switch(cmd) {
                case 1:
                    for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Scalar_BLProfl[dummy]; psw_chk=0x07; Atribute=0x03; send_main19();
                    break;

                case 2:
                    for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = Scalar_BLProfl[dummy]; psw_chk=0x07; Atribute=0x02; send_main19();
                    break;

                case 3:
                    for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = BillPerdProfil[dummy]; psw_chk=0x07; Atribute=0x03; send_main19();
                    break;

                case 4:
                    for(dummy=0;dummy<6;dummy++) dlms_temp6[dummy] = BillPerdProfil[dummy]; psw_chk=0x07; Atribute=0x02; send_main19();
                    break;
                default: break;
            }
        }

        else if((bilack == TRUE) && (block_ack == FALSE)) {
            byte7flg = 1;
            ger_DLMS_COM();
            byte7flg = 0;
            send_7DLMS(DLMS_COM);
        }

        else if((bilack == FALSE) && (block_ack == TRUE)) {
            ger_DLMS_COM();
            //byte7flg = 0;
            send_16DLMS(DLMS_COM,BillBLOCK_NO);
        }

        Billflg = TRUE;

        return CustomUtils.trim(CustomUtils.int2byte(mtrbuffer));
    }

    void send_16DLMS(int dlmscom, int BLOCK_NO) {
        mtrbuffer = new int[100];
        int dummya;

        len=8;	//5+3
        mtrbuffer[0]=0xA0;
        mtrbuffer[1]=0x16;
        mtrbuffer[2]=0x00;
        mtrbuffer[3]=0x02;
        mtrbuffer[4]=destaddLH;//0x04;
        mtrbuffer[5]=destaddLL;//0x01;
        mtrbuffer[6]=sourceadd;//0x41;
        mtrbuffer[7]=dlmscom;
        get_fcs16();
        mtrbuffer[8]=fcs16;	//5+3
        mtrbuffer[9]=fcs16>>8;

        dummya=10;	//7+3
        for(hex=0;hex<8;hex++)
            mtrbuffer[dummya++] = Main_16B[hex];

        //	byte7flg=1;
        //	ger_DLMS_COM();
        //	byte7flg=0;

        mtrbuffer[18] = BLOCK_NO >> 8;	//15+3
        mtrbuffer[19] = BLOCK_NO;

        len=20;		//17+3
        get_fcs16();
        mtrbuffer[20]=fcs16;
        mtrbuffer[21]=fcs16>>8;

        dummy=22;	//19+3
        for(i=0;i<23;i++)	//20+3
        {
            mtrbuffer[dummy+1]=mtrbuffer[dummy];
            dummy--;
        }

        mtrbuffer[0]=0x7E;
        mtrbuffer[23]=0x7E;		//20+3

        DLMS_len=24;	//21+3
    }

    public void checkResponse(byte[] liedbuf) {
        Billflg=FALSE;
        if(liedbuf[1] != (byte)0xa0)
            this.bilack = TRUE;
        else
            this.bilack = FALSE;
        if(liedbuf[14] == (byte)0xc4  && liedbuf[15] == (byte)0x02 && liedbuf[16] == (byte)0x81)
        {
            blck_value = liedbuf[20];
            blck_value <<= 8;
            blck_value = blck_value+liedbuf[21];
            if(BillBLOCK_NO == blck_value)
            {
                //printf("$$$$$$$$$tamper return $$$$$$$$$$$$\n");
                //exit(1);
                //DLMS_RespFlag = 0;/now commented
            }

            NextBlock = liedbuf[17];
            this.block_ack = FALSE;
        }
        if(liedbuf[1] != (byte)0xa0)
            this.bilack = TRUE;
        else if((NextBlock == 0) && (BillBLOCK_NO != blck_value))
        {
            BillBLOCK_NO += 1;
            this.block_ack = TRUE;
            this.bilack = FALSE;
        }
        else
        {
            this.bilack= FALSE;
            this.block_ack = FALSE;
        }
    }

    byte[] send_AthenticationLT() {
        mtrbuffer = new int[100];
        int dummya;

        mtrbuffer[0] = 0xA0;
        mtrbuffer[1] = 0x43;
        mtrbuffer[2] = 0x00;
        mtrbuffer[3] = 0x02;

        mtrbuffer[4] = destaddLH;
        mtrbuffer[5] = destaddLL;
        mtrbuffer[6] = sourceadd;
        mtrbuffer[7] = 0x10;

        len=8;
        get_fcs16();
        mtrbuffer[8]=fcs16;
        mtrbuffer[9]=fcs16>>8;

        dummya=10;
        for(hex=0;hex<33;hex++)
            mtrbuffer[dummya++]=ATHENT_ALT[hex];

        mtrbuffer[43]='l';
        mtrbuffer[44]='n';
        mtrbuffer[45]='t';
        mtrbuffer[46]='1';

        dummya=47;
        for(hex=0;hex<18;hex++)
            mtrbuffer[dummya++]=ATHENT_BLT[hex];
        len=65;
        get_fcs16();
        mtrbuffer[65]=fcs16;
        mtrbuffer[66]=fcs16>>8;

/*	ser_out1(0x7E);
	for(dummya=0;dummya<69;dummya++)
		ser_out1(mtrbuffer[dummya]);
	ser_out1(0x7E);
	del(20);
*/
        dummya=66;
        for(i=0;i<67;i++)
        {
            mtrbuffer[dummya+1]=mtrbuffer[dummya];
            dummya--;
        }

        mtrbuffer[0]=0x7E;
        mtrbuffer[68]=0x7E;

        DLMS_len = 69;

//		printf("\n send_Athentication :\n ==> ");

//		for(i=0;i<DLMS_len;i++)
//			printf("%x ",mtrbuffer[i]);
//		printf("\n");

//		SendData(1,DLMS_FD,mtrbuffer,DLMS_len,DLMS_ADD);

        return CustomUtils.trim(CustomUtils.int2byte(mtrbuffer));
    }
}

