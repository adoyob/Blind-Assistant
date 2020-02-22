package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView microid;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private TextToSpeech mtts;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);

        microid = (CardView) findViewById(R.id.microid);
        mtts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mtts.setLanguage(new Locale("bn_BD"));
                    mobile_speak("ব্লাইন্ড এসিসটেন্ট ‍এপলিকেশনে আপনাকে স্বাগতম");

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {


                    } else {

                        microid.setEnabled(true);
                    }

                } else {


                }
            }
        });

        microid.setOnClickListener(this);


    }

    private void mobile_speak(String str) {

        mtts.setPitch((float) 0.8);
        mtts.setSpeechRate((float) 0.8);
        mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);

    }

    private void person_input_speak() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            mobile_speak("আপনার ডাটা অথবা ইন্টারনেট অন করুন");
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void location_service() {
        boolean isGPS_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        //Toast.makeText(this,"location service entered",Toast.LENGTH_SHORT).show();
        if (isGPS_enabled) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                        String adr, sub_city, city;
                        adr = addressList.get(0).getFeatureName();
                        sub_city = addressList.get(0).getSubLocality();
                        city= addressList.get(0).getLocality();
                        mobile_speak("আপনি এখন" + adr +","+sub_city+"যায়গায়" + city + "শহরে আছেন");
                        locationManager.removeUpdates(locationListener);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show();
            mobile_speak("কারো সাহায্য নিয়ে লকেশন সারবিছটি এলাও করুন");

            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
        } else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    processResult(result.get(0));


                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
           if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, permissions[0], Toast.LENGTH_SHORT).show();
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            }
        }

    }

    @Override
    protected void onDestroy() {
        if(mtts!= null)
        {
            mtts.stop();
            mtts.shutdown();
        }
        super.onDestroy();
        if (locationManager != null) {

                try {
                    locationManager.removeUpdates(locationListener);
                } catch (Exception ex) {
                   Toast.makeText(this,"failed to remove location listener",Toast.LENGTH_SHORT).show();
                }

        }
    }
    private void processResult(String command)
    {
        command= command.toLowerCase();
        if(command.indexOf("time")!=-1||command.indexOf("samay")!=-1||command.indexOf("somoy")!=-1)
        {
            String currentTime = new SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(new Date());
            mobile_speak("এখন সময় "+currentTime);
        }
        else if(command.indexOf("date")!=-1||command.indexOf("tarikh")!=-1||command.indexOf("tariq")!=-1)
        {
            String currentDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());
            mobile_speak("আজকের তারিখ "+currentDate);

        }else if (command.indexOf("location")!=-1)
        {
            location_service();

        } else if (command.indexOf("close")!=-1||command.indexOf("exit")!=-1)
        {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

        }


    }

    @Override
    public void onClick(View v) {
        person_input_speak();
    }
}
