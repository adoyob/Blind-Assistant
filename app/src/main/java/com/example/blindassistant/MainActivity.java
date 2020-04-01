package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

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


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    String adr, sub_city, city,translated1,translated2;

    int longt,latt;
    private CardView microid;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    private TextToSpeech mtts;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SpeechRecognizer speech ;
    private FirebaseTranslator translator;

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
        FirebaseTranslatorOptions options =
                new FirebaseTranslatorOptions.Builder()
                        .setSourceLanguage(FirebaseTranslateLanguage.EN)
                        .setTargetLanguage(FirebaseTranslateLanguage.BN)
                        .build();
        translator =
                FirebaseNaturalLanguage.getInstance().getTranslator(options);

        microid.setOnClickListener(this);


    }

    private void mobile_speak(String str) {

        mtts.setPitch((float) 0.8);
        mtts.setSpeechRate((float) 0.8);
        mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);


    }

    private void person_input_speak() {
        checkspeechRecordpermission();
        speech = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);


        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"bn_BD");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",new String[]{"bn_BD"});
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,3000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,3000);
        intent.putExtra("android.intent.extra.durationLimit",3);
        speech.setRecognitionListener(new listener());

        speech.startListening(intent);
        new CountDownTimer(5000,1000){

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                speech.stopListening();
            }
        }.start();

    }
    class listener implements RecognitionListener{

        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(MainActivity.this,"Start Listening",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            Toast.makeText(MainActivity.this,"Stop Listening",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int error) {
            if(error==7)
                mobile_speak("দুঃখিত কিছু বুঝা যায় নি");
            else if(error==5){
                Toast.makeText(MainActivity.this,"Error Listening"+error,Toast.LENGTH_LONG).show();
            }


        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(matches!=null){
                processResult(matches.get(0).trim());
                //Toast.makeText(MainActivity.this,""+matches.get(0).trim(),Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }

    private void checkspeechRecordpermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.RECORD_AUDIO},1);
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
            //Toast.makeText(this,content,Toast.LENGTH_LONG).show();
            //Log.i("contentdata",content);

            JSONObject jsonObject = new JSONObject(content);
            String weatherData = jsonObject.getString("weather");
            String mainTemperature = jsonObject.getString("main");
            //Toast.makeText(this,weatherData,Toast.LENGTH_LONG).show();

            // Log.i("weatherdata",weatherData);

            JSONArray array = new JSONArray(weatherData);

            String main = "";
            String description="";
            String temperature ="";
            final String city_Name = "";
            for (int i=0; i<array.length();i++){
                JSONObject weatherpart = array.getJSONObject(i);
                main = weatherpart.getString("main");
                description = weatherpart.getString("description");
            }
            JSONObject mainpart = new JSONObject(mainTemperature);
            temperature = mainpart.getString("temp");
            double temp_int = Double.parseDouble(temperature);
            temp_int = Math.round(temp_int);
            int t_value = (int) temp_int;

            translate(main,description);


            final String[] finalMain = {null};

            final String[] finalDescription = {null};
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
               public void run() {
                    finalMain[0] = translated1;
                    finalDescription[0] =translated2;

                    Toast.makeText(MainActivity.this,"আকাশ: "+ finalMain[0]+"বর্ণনা: "+ finalDescription[0],Toast.LENGTH_LONG).show();
                    String resultText = "আকাশ: "+ finalMain[0] +"\n বর্ণনা: "+ finalDescription[0] +"\n তাপমাত্রা: "+t_value+" degree Celsius";
                    mobile_speak(resultText);

               }
            },2000);


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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, permissions[0], Toast.LENGTH_SHORT).show();


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
        Toast.makeText(MainActivity.this,""+command,Toast.LENGTH_LONG).show();
        for(int i=0;i<command.length();i++){
            if(command.charAt(i)== 2479 && command.charAt(i+1)== 2492){
                StringBuilder command2=new StringBuilder(command);
                command2.deleteCharAt(i+1);
                command=command2.toString();
                break;
            }
        }

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


        } else if (command.indexOf("পরিবেশ")!=-1||command.indexOf("এনভাযরনমেন্ট")!=-1)
        {
            Intent intent2=new Intent(MainActivity.this,CameraActivity.class);
            startActivity(intent2);
            //Intent intent2=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //startActivity(intent2);

        }else if (command.indexOf("ওযেদার")!=-1 || command.indexOf("টেম্পারেচার")!=-1||command.indexOf("আবহাওযা")!=-1){
            location_service();
            FirebaseApp.initializeApp(MainActivity.this);
            final Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mobile_speak("একটু অপেক্ষা করুন");
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

        } else if (command.indexOf("ব্যাটারি চার্জ")!=-1 || command.indexOf("ফোনের চার্জ")!=-1|| command.indexOf("ব্যাটারি পার্সেন্টেজ")!=-1|| command.indexOf("চার্জ")!=-1|| command.indexOf("পার্সেন্টেজ")!=-1){
            getBattery_percentage();

        }else if (command.indexOf("close")!=-1||command.indexOf("exit")!=-1)
        {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

        }


    }

    private void translate(String inputtext1,String inputtext2) {

      /*Install Firebase Model English
        FirebaseModelManager modelManager = FirebaseModelManager.getInstance();
        FirebaseTranslateRemoteModel frModel =
                new FirebaseTranslateRemoteModel.Builder(FirebaseTranslateLanguage.BN).build();
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        modelManager.downloadRemoteModelIfNeeded(frModel)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        Log.d("download","bdmodel");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Error.
                    }
                });

        Install Firebase Model Bangla
        FirebaseTranslateRemoteModel frModel2 =
                new FirebaseTranslateRemoteModel.Builder(FirebaseTranslateLanguage.EN).build();
        FirebaseModelDownloadConditions conditions2 = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        modelManager.downloadRemoteModelIfNeeded(frModel2)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        Log.d("download","enmodel");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Error.
                    }
                });*/
        final String[] inputText = {inputtext1,inputtext2};

        translator.translate(inputText[0])
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(@NonNull String translatedText) {
                               translated1=translatedText;
                               //Toast.makeText(MainActivity.this,"tranlate"+inputText2[0],Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this,"exception"+e.getMessage(),Toast.LENGTH_LONG).show();
                            }
                        });
        translator.translate(inputText[1])
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(@NonNull String translatedText) {
                                translated2=translatedText;
                                //Toast.makeText(MainActivity.this,"tranlate"+inputText2[0],Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this,"exception"+e.getMessage(),Toast.LENGTH_LONG).show();
                            }
                        });

    }

    private void getBattery_percentage() {
        IntentFilter ifilter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus=getApplicationContext().registerReceiver(null,ifilter);
        int level=batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        float batterPCT=level/(float)scale;
        float p=batterPCT*100;
        mobile_speak("আপনার ফোনের চার্জ"+String.valueOf(Math.round(p))+"পার্সেন্টেজ");
    }

    @Override
    public void onClick(View v) {
        person_input_speak();
    }
}
