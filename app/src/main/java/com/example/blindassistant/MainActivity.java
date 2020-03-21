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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String adr, sub_city, city;

    int longt,latt;
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
        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",new String[]{"bn-BD"});
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,3000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,3000);
        try {

            Handler handler=new Handler();
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            Runnable r =new Runnable() {
                @Override
                public void run() {

                }
            };
           // handler.postDelayed(r,3000);



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
                    longt=(int)longitude;
                    latt=(int)latitude;
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);

                        adr = addressList.get(0).getFeatureName();
                        sub_city = addressList.get(0).getSubLocality();
                        city= addressList.get(0).getLocality();
                        //Toast.makeText(MainActivity.this,"adr "+adr+" sub_city "+sub_city+" city "+city,Toast.LENGTH_LONG).show();
                        //Toast.makeText(MainActivity.this,"long"+(int)longitude+"lat"+(int)latitude,Toast.LENGTH_LONG).show();

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

                    processResult(result.get(0).trim());


                }
            }
        }
    }
    private void weather_service(){

        String content;
        Weather weather = new Weather();
        try {
            content = weather.execute("https://openweathermap.org/data/2.5/weather?lat="+latt+"&lon="+longt+"&appid=b6907d289e10d714a6e88b30761fae22").get();
            Toast.makeText(this,content,Toast.LENGTH_LONG).show();
            //Log.i("contentdata",content);

            JSONObject jsonObject = new JSONObject(content);
            String weatherData = jsonObject.getString("weather");
            String mainTemperature = jsonObject.getString("main");
            Toast.makeText(this,weatherData,Toast.LENGTH_LONG).show();

            // Log.i("weatherdata",weatherData);

            JSONArray array = new JSONArray(weatherData);

            String main="";
            String description="";
            String temperature ="";
            String city_Name = "";
            for (int i=0; i<array.length();i++){
                JSONObject weatherpart = array.getJSONObject(i);
                main= weatherpart.getString("main");
                description = weatherpart.getString("description");
            }
            JSONObject mainpart = new JSONObject(mainTemperature);
            temperature = mainpart.getString("temp");
            double temp_int = Double.parseDouble(temperature);
            temp_int = Math.round(temp_int);
            int t_value = (int) temp_int;
            String resultText = "আকাশ: "+main+"\n বর্ণনা: "+description+"\n তাপমাত্রা: "+t_value+" degree Celsius";
            mobile_speak(resultText);
            //Toast.makeText(this,"Temperature"+resultText,Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"Exception"+e.getMessage(),Toast.LENGTH_LONG).show();
        }


    }

    private void temperature_service(){

        String content;
        Weather weather = new Weather();
        try {
            content = weather.execute("https://openweathermap.org/data/2.5/weather?lat="+latt+"&lon="+longt+"&appid=b6907d289e10d714a6e88b30761fae22").get();
            Toast.makeText(this,content,Toast.LENGTH_LONG).show();
            //Log.i("contentdata",content);

            JSONObject jsonObject = new JSONObject(content);
            String weatherData = jsonObject.getString("weather");
            String mainTemperature = jsonObject.getString("main");
            Toast.makeText(this,weatherData,Toast.LENGTH_LONG).show();

            JSONArray array = new JSONArray(weatherData);

            String main="";
            String description="";
            String temperature ="";
            String city_Name = "";
            for (int i=0; i<array.length();i++){
                JSONObject weatherpart = array.getJSONObject(i);
                main= weatherpart.getString("main");
                description = weatherpart.getString("description");
            }
            JSONObject mainpart = new JSONObject(mainTemperature);
            temperature = mainpart.getString("temp");
            double temp_int = Double.parseDouble(temperature);
            temp_int = Math.round(temp_int);
            int t_value = (int) temp_int;
            String resultText = " তাপমাত্রা: "+t_value+" degree Celsius";
            mobile_speak(resultText);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,"Exception"+e.getMessage(),Toast.LENGTH_LONG).show();
        }


    }

    class Weather extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... address) {
            try {
                URL url = new URL(address[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream is = connection.getInputStream();
                InputStreamReader irs = new InputStreamReader(is);

                int data = irs.read();
                String content = "";
                char ch;
                while (data != -1){
                    ch = (char) data;
                    content = content + ch;
                    data = irs.read();
                }
                return content;
            }catch (Exception e){

            }
            return null;
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
        //Toast.makeText(MainActivity.this,"("+command+") "+command.indexOf("সময়")+" "+command.equals("সময়"),Toast.LENGTH_SHORT).show();

       //int x=command.charAt(2);
            //Toast.makeText(MainActivity.this, "(" + command.charAt(2) + ") "+x+" ", Toast.LENGTH_SHORT).show();



        //int a=(int)'স',b=(int)'ম',cc=(int)'য়';
        //Toast.makeText(MainActivity.this, "(" +a+". "+b+", "+cc, Toast.LENGTH_SHORT).show();
        if(command.indexOf("সময়")!=-1 || command.indexOf("সময")!=-1 ||command.indexOf("টাইম")!=-1)
        {
            String currentTime = new SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(new Date());
            mobile_speak("এখন সময় "+currentTime);
        }
        else if(command.indexOf("ডেট")!=-1||command.indexOf("তারিখ")!=-1||command.indexOf("তারিক")!=-1)
        {
            String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            mobile_speak("আজকের তারিখ "+currentDate);

        }else if (command.indexOf("লোকেশন")!=-1||command.indexOf("অবস্থান")!=-1||command.indexOf("লোকেসন")!=-1)
        {

            location_service();
            final Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mobile_speak("আপনি এখন" + adr +","+sub_city+"যায়গায়" + city + "শহরে আছেন");
                }
            },2000);


        } else if (command.indexOf("কেমেরা")!=-1||command.indexOf("ক্যামেরা")!=-1)
        {
            Intent intent2=new Intent(MainActivity.this,CameraActivity.class);
            startActivity(intent2);
            //Intent intent2=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //startActivity(intent2);

        }else if (command.indexOf("ওয়েদার ")!=-1 || command.indexOf("টেম্পারেচার")!=-1||command.indexOf("আবহাওয়া ")!=-1){
            location_service();
            final Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    weather_service();
                }
            },2000);

        }else if (command.indexOf("তাপমাত্রা")!=-1 || command.indexOf("টেম্পারেচার")!=-1){
            location_service();
            final Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    temperature_service();
                }
            },2000);

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
