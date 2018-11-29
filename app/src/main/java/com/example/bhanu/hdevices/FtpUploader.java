package com.example.bhanu.hdevices;

public interface FtpUploader {
    void connect() throws Exception;
    void disconnect();
    void uploadFile(String localFileFullName, String fileName, String hostDir) throws Exception;
    boolean fileExists(String fileName);
}
