package com.example.blindassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import org.tensorflow.lite.examples.detection.DetectorActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class TrainActivity1 extends AppCompatActivity implements View.OnClickListener {
    private static TextToSpeech mtts;
    private SpeechRecognizer speech ;
    private CardView microid;
    private File xmlFile,dataPath;
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

        mtts.setPitch((float) 0.8);
        mtts.setSpeechRate((float) 0.8);
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

    private void processResult(String sentence) {
        if(sentence.indexOf("নতুন")!=-1||sentence.indexOf("নতুন নাম")!=-1){
            Intent intent=new Intent(TrainActivity1.this, TrainActivity.class);
            intent.putExtra("delete","no");
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);

        }else if(sentence.indexOf("পুরাতন")!=-1 || sentence.indexOf("পুরাতন নাম")!=-1 || sentence.indexOf("পুরাতন নামগুলো")!=-1){
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
                //if(dataPath.exists()) {
                   // Boolean h=dataPath.delete();
                   // Toast.makeText(TrainActivity1.this,"file Found and deleted: "+h,Toast.LENGTH_LONG).show();

               // }
            }catch (Exception e){
                Toast.makeText(TrainActivity1.this,"Error: "+e.getMessage(),Toast.LENGTH_LONG).show();
            }

            local.remove("names");
            name=new ArrayList<String>();
            //Log.i(TAG, "8");
            /*xmlFile = new File(dataPath, filename);
            if(xmlFile.exists()){
                local.remove("names");
                xmlFile.delete();
            }*/
            mobile_speak("সবার নাম এবং ছবি মুছে ফেলা হয়েছে");
        }else if(sentence.equals("নির্দেশেনা")||sentence.equals("কমান্ড")||sentence.equals("কমান্ড লাইন")){
            mobile_speak(getString(R.string.train_ins));
        }
        else{
            mobile_speak(getString(R.string.sorry_ins));
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
        name=local.getListString("names");
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