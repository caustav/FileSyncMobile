package com.kc.filesync;

/**
 * Created by Kaustav on 10-Sep-16.
 */
public class ConnectionInfo {

    public String getSenderDeviceName() {
        return senderDeviceName;
    }

    public void setSenderDeviceName(String senderDeviceName) {
        this.senderDeviceName = senderDeviceName;
    }

    private String senderDeviceName;

    public String getSenderDeviceIpAddress() {
        return senderDeviceIpAddress;
    }

    public void setSenderDeviceIpAddress(String senderDeviceIpAddress) {
        this.senderDeviceIpAddress = senderDeviceIpAddress;
    }

    private String senderDeviceIpAddress;

    public void load(String s){
        String [] conInfo = s.split(":");
        if (conInfo != null && conInfo.length > 1){
            senderDeviceName = conInfo[0].trim();
            senderDeviceIpAddress = conInfo[1].trim();
        }
    }
}
