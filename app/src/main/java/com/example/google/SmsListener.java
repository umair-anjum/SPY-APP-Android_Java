package com.example.google;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.example.google.Classes.smsdata;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SmsListener extends BroadcastReceiver {
    FirebaseAuth firebaseAuth;
    String msg_from;
    String msgBody;
    LocationManager locationManager;
    double latitude1, longitude1;
    Geocoder geocoder;
    List<Address> addresses;

    @Override
    public void onReceive(Context context, Intent intent){

        firebaseAuth = FirebaseAuth.getInstance();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs = null;

            if (bundle != null) {

                //---retrieve the SMS message received---
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        msgBody = msgs[i].getMessageBody();
                    }
                    String uid = firebaseAuth.getUid();
                    SharedPreferences pref = context.getSharedPreferences("userprofiledata", Context.MODE_PRIVATE);
                    String recievername = pref.getString("username", "");

                    final int random = new Random().nextInt((1000000 - 0) + 1) + 0;

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Messages").child(uid).child(String.valueOf(random));
                        smsdata smsdata = new smsdata(uid, recievername, msg_from, msgBody, "N/A", latitude1, longitude1);
                        myref.setValue(smsdata);
                        return;
                    }
                    Location locationGPS = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                    if (locationGPS != null) {
                        latitude1 = locationGPS.getLatitude();
                        longitude1 = locationGPS.getLongitude();
                        geocoder = new Geocoder(context, Locale.getDefault());
                        addresses = geocoder.getFromLocation(latitude1, longitude1, 1);
                        //storing into database
                        DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Messages").child(uid).child(String.valueOf(random));
                        smsdata smsdata = new smsdata(uid, recievername, msg_from, msgBody, addresses.get(0).getAddressLine(0), latitude1, longitude1);
                        myref.setValue(smsdata);

                    } else if (locationGPS == null) {
                        latitude1 = 0.0;
                        longitude1 = 0.0;
                        //storing into database
                        DatabaseReference myref = FirebaseDatabase.getInstance().getReference("Messages").child(uid).child(String.valueOf(random));
                        smsdata smsdata = new smsdata(uid, recievername, msg_from, msgBody, "N/A", latitude1, longitude1);
                        myref.setValue(smsdata);
                    }
                } catch (Exception e) {
                    Log.d("Exception caught", e.getMessage());
                }
            }
        }
    }
}
