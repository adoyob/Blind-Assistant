package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import org.tensorflow.lite.examples.detection.DetectorActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class TrainActivity1 extends AppCompatActivity implements View.OnClickListener {
    private static TextToSpeech mtts;
    private SpeechRecognizer speech ;
    private CardView microid;
    private static final int PERMS_REQUEST_CODE = 123;
    private File xmlFile,dataPath,tempImgFile2;
    Storage local;
    int i=0;
    private ArrayList<String> name=new ArrayList<String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train1);
        microid = (CardView) findViewById(R.id.touchid);
        local=new Storage(TrainActivity1.this);

        mtts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mtts.setLanguage(new Locale("bn_BD"));
                    mobile_speak("পরিচিতি পর্বে আপনাকে স্বাগতম । নির্দেশনা জানতে স্ক্রিনে টাচ করে বলুন নির্দেশনা বা কমান্ড লাইন । আর ফিরে যেতে চাইলে একসাথে দুইবার স্ক্রিনে টাচ করুন ।" );

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {


                    } else {

                        microid.setEnabled(true);
                    }

                } else {


                }
            }
        });
        microid.setOnClickListener(TrainActivity1.this);


    }
    public static void mobile_speak(String str) {

        mtts.setPitch((float) 1.2);
        mtts.setSpeechRate((float) 0.7);
        mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);


    }

    private void person_input_speak_bangla() {
        speech = SpeechRecognizer.createSpeechRecognizer(TrainActivity1.this);


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
        speech.setRecognitionListener(new TrainActivity1.listener());

        speech.startListening(intent);
        new CountDownTimer(7000,1000){

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                speech.stopListening();
            }
        }.start();
    }


    class listener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(TrainActivity1.this, "Start Listening", Toast.LENGTH_LONG).show();
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
            Toast.makeText(TrainActivity1.this, "Stop Listening", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int error) {
            if (error == 7)
                mobile_speak("দুঃখিত কিছু বুঝা যায় নি");
            else if (error == 5) {
                //Toast.makeText(MainActivity.this,"Error Listening"+error,Toast.LENGTH_LONG).show();
            }




        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String sentence=matches.get(0).trim();
            if(matches!=null) {
                processResult(sentence);

            }

        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }
    @SuppressLint("WrongConstant")
    private boolean hasPermissions(){
        int res = 0;
        //string array of permissions,
        String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};

        for (String perms : permissions){
            res = checkCallingOrSelfPermission(perms);
            if (!(res == PackageManager.PERMISSION_GRANTED)){

                return false;
            }
        }
        return true;
    }
    private void requestPerms(){

        String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(permissions,PERMS_REQUEST_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allowed = true;
        switch (requestCode){
            case PERMS_REQUEST_CODE:
                for (int res : grantResults){
                    // if user granted all permissions.
                    allowed = allowed && (res == PackageManager.PERMISSION_GRANTED);
                }
                break;
            default:
                // if user not granted permissions.
                allowed = false;
                break;
        }
        if (allowed){
            //user granted all permissions we can perform our task.

        }
        else {
            // we will give warning to user that they haven't granted permissions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    //Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    private void processResult(String sentence) {
        if(sentence.indexOf("নতুন")!=-1||sentence.indexOf("নতুন নাম")!=-1||sentence.indexOf("নিউ নেম")!=-1||sentence.indexOf("নিউ")!=-1){
            Intent intent=new Intent(TrainActivity1.this, TrainActivity.class);
            intent.putExtra("delete","no");
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        }else if(sentence.indexOf("পুরাতন")!=-1 || sentence.indexOf("পুরাতন নাম")!=-1 || sentence.indexOf("পুরাতন নামগুলো")!=-1|| sentence.indexOf("ওল্ড নেম")!=-1|| sentence.indexOf("ওল্ড")!=-1){
            if(!name.isEmpty()){
                mobile_speak("নাম গুলো হল "+name);
            }else{
                mobile_speak("কোনো নাম নেই");
            }


        }else if(sentence.equals("ডিলিট")){
            Intent intent3=new Intent(TrainActivity1.this, TrainActivity.class);
            intent3.putExtra("delete","yes");
            intent3.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent3);


        }else if(sentence.equals("সব ডিলিট") ||  sentence.equals("অল ডিলিট")){
            String filename = "lbph_trained_data.xml";
            try{
                dataPath = new File(Environment.getExternalStorageDirectory(), "BlindAssistant");
                Boolean h=deleteDirectory(dataPath);
                Toast.makeText(TrainActivity1.this,"file Found and deleted: "+h,Toast.LENGTH_LONG).show();
            }catch (Exception e){
                Toast.makeText(TrainActivity1.this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
            }
            local.remove("names_en");
            name=new ArrayList<String>();
            mobile_speak("সবার নাম এবং ছবি মুছে ফেলা হয়েছে");
        }else if(sentence.equals("নির্দেশনা")||sentence.equals("কমান্ড")||sentence.equals("কমান্ড লাইন")){
            mobile_speak(getString(R.string.train_ins));
        }
        else{
            mobile_speak("দুঃখিত,"+sentence+" নামে কোন নির্দেশনা নেই । নির্দেশনা জানতে বলুন,নির্দেশনা বা, কমান্ড লাইন");
        }

    }
    public boolean isOnline(){
        ConnectivityManager cm=(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo=cm.getActiveNetworkInfo();
        if(netInfo!=null && netInfo.isConnectedOrConnecting()){
            return true;
        }
        else{
            return false;
        }
    }

    public static boolean deleteDirectory(File path) {
        if( path.exists() ) {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++) {
                if(files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return(path.delete());
    }
    @Override
    public void onClick(View v) {
        name=local.getListString("names_en");
        if(name.isEmpty()){
            try{
                dataPath = new File(Environment.getExternalStorageDirectory(), "BlindAssistant");
                if(dataPath.exists()){
                    Boolean h=deleteDirectory(dataPath);
                }

            }catch(Exception e){

            }
        }
        if(!hasPermissions()){
            mobile_speak("কারও সাহায্য নিয়ে পারমিশনগুলো এলাউ করুন");
            requestPerms();
        }
        else{
            mtts.stop();

            i++;
            Handler handler=new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(i==1){
                        if(!isOnline()){
                            mobile_speak("ইন্টারনেট সংযোগ অন করুন বা মোবাইল ডাটা অন করুন");
                        }else{
                            person_input_speak_bangla();
                        }
                        //personName=new ArrayList<String>();
                    }
                    else if(i==2){
                        mobile_speak("হোমে ফিরে এসেছেন");
                        Intent intent_main=new Intent(TrainActivity1.this, MainActivity.class);
                        intent_main.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent_main);
                    }
                    i=0;
                }
            },500);
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

    }


}