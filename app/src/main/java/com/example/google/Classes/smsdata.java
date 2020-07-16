package com.example.google.Classes;

public class smsdata {
    String recieverid;
    String recievername;
    String sendercontact;
    String messagebody;
    String userlocation;
    Double latitude;
    Double longitude;

    public smsdata() {
    }

    public smsdata(String recieverid, String recievername, String sendercontact, String messagebody, String userlocation, Double latitude
            , Double longitude) {
        this.recieverid = recieverid;
        this.recievername = recievername;
        this.sendercontact = sendercontact;
        this.messagebody = messagebody;
        this.userlocation = userlocation;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getRecievername() {
        return recievername;
    }

    public String getRecieverid() {
        return recieverid;
    }

    public String getSendercontact() {
        return sendercontact;
    }

    public String getMessagebody() {
        return messagebody;
    }

    public String getUserlocation() {
        return userlocation;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
