package com.example.bhanu.hdevices;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by bhanu on 13/2/18.
 * connect device data holder
 */

public class EnergyMeter {
    private int index;
    private String ipAddr;
    private String mtrNo;
    private int make;
    private String readStatus;
    private String uploadStatus;
    private String makeString;
    private boolean readAgain;
    private boolean isSavedMeter;
    private String timestamp;
    private ArrayList<File> relatedFiles;

    public EnergyMeter() {
        readStatus = Constants.NA;
        uploadStatus = Constants.NA;
        relatedFiles = new ArrayList<>();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getMtrNo() {
        return mtrNo;
    }

    public void setMtrNo(String mrtNo) {
        this.mtrNo = mrtNo;
    }

    public String getMtrNoDisplayText() {
        String serialNoText = getMtrNo();
        int noOfRelatedFiles = getRelatedFiles().size();

        if (noOfRelatedFiles > 0) {
            serialNoText += " (" + noOfRelatedFiles + ")";
            setUploadStatus(Constants.FAILED);
        }

        return serialNoText;
    }

    public int getMake() {
        return make;
    }

    public void setMake(int make) {
        this.make = make;

        switch (make) {
            case Constants.GENUS:
                this.makeString = Constants.GENUS_STR;
                break;
            case Constants.LNT:
                this.makeString = Constants.LNT_STR;
                break;
            case Constants.SECURE:
                this.makeString = Constants.SECURE_STR;
                break;
            case Constants.SECURE_DLMS:
                this.makeString = Constants.SECURE_DLMS_STR;
                break;
            case Constants.LPG:
                this.makeString = Constants.LPG_STR;
                break;
            case Constants.LNT_LEGACY:
                this.makeString = Constants.LNT_lEGACY_STR;
                break;
            case Constants.LNT2:
                this.makeString = Constants.LNT2_STR;
                break;
        }
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        if (readStatus.equals(Constants.DONE)
                || readStatus.equals(Constants.NA)
                || readStatus.equals(Constants.FAILED)
                || readStatus.equals(Constants.PROGRESS)) {
            this.readStatus = readStatus;
        } else {
            try {
                throw new Exception("Invalid status value: " + readStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getMakeString() {
        return makeString;
    }

    public void setMakeString(String makeString) {
        this.makeString = makeString;
    }

    public boolean isReadAgain() {
        return readAgain;
    }

    public void setReadAgain(boolean readAgain) {
        this.readAgain = readAgain;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSavedMeter() {
        return isSavedMeter;
    }

    public void setSavedMeter(boolean savedMeter) {
        isSavedMeter = savedMeter;
    }

    public String getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(String uploadStatus) {
        if (uploadStatus.equals(Constants.DONE)
                || uploadStatus.equals(Constants.NA)
                || uploadStatus.equals(Constants.FAILED)
                || uploadStatus.equals(Constants.PROGRESS)) {
            this.uploadStatus = uploadStatus;
        } else {
            try {
                throw new Exception("Invalid status value: " + uploadStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ArrayList<File> getRelatedFiles() {
        return relatedFiles;
    }

    public void setRelatedFiles(ArrayList<File> relatedFiles) {
        this.relatedFiles = relatedFiles;
    }

    public String getFileNamePrefix() {
        String fileNamePrefix = getMakeString() + "_" + getMtrNo();
        return fileNamePrefix;
    }

    @Override
    public String toString() {
        return "EnergyMeter{" +
                "index=" + index +
                ", ipAddr='" + ipAddr + '\'' +
                ", mtrNo='" + mtrNo + '\'' +
                ", make=" + make +
                ", readStatus='" + readStatus + '\'' +
                ", uploadStatus='" + uploadStatus + '\'' +
                ", makeString='" + makeString + '\'' +
                ", readAgain=" + readAgain +
                ", isSavedMeter=" + isSavedMeter +
                ", timestamp='" + timestamp + '\'' +
                ", relatedFiles=" + relatedFiles +
                '}';
    }
}
