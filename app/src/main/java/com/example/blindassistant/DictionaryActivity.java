package com.example.blindassistant;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import java.util.ArrayList;
import java.util.Locale;

public class DictionaryActivity extends AppCompatActivity {

    private TextToSpeech mtts;
    private SpeechRecognizer speech ;
    String language;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dictionary);
        Bundle bundle=getIntent().getExtras();
        language=bundle.getString("language");



        mtts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mtts.setLanguage(new Locale("bn_BD"));
                    mobile_speak("দয়া করে, "+language+ ",শব্দটি বা বাক্য বলুন",(float)0.8);

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {


                    } else {


                    }

                } else {


                }
            }
        });
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(language.equals("english")){
                    person_input_speak_english();
                }
                else if(language.equals("bangla")){
                    person_input_speak_bangla();
                }

            }
        },3500);



    }

    private void person_input_speak_bangla() {
        speech = SpeechRecognizer.createSpeechRecognizer(DictionaryActivity.this);


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
        speech.setRecognitionListener(new DictionaryActivity.listener());

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

    private void person_input_speak_english() {

        speech = SpeechRecognizer.createSpeechRecognizer(DictionaryActivity.this);


        final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en_US");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        speech.setRecognitionListener(new DictionaryActivity.listener());

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
            Toast.makeText(DictionaryActivity.this, "Start Listening", Toast.LENGTH_LONG).show();
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
            Toast.makeText(DictionaryActivity.this, "Stop Listening", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int error) {
            if (error == 7)
                mobile_speak("দুঃখিত কিছু বুঝা যায় নি",(float)0.8);
            else if (error == 5) {
                //Toast.makeText(MainActivity.this,"Error Listening"+error,Toast.LENGTH_LONG).show();
            }
            finish();



        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String sentence=matches.get(0).trim();
            if(matches!=null) {
                if(language.equals("english")){
                    if(sentence.indexOf("spelling")!=-1){
                        String[] arr=sentence.split("spelling");
                        //Toast.makeText(DictionaryActivity.this,sentence+","+arr[0]+","+arr[1],Toast.LENGTH_LONG).show();
                        if(arr.length>0 && arr[0].equals("")){
                            String temp=arr[1];
                            String k=temp.replaceAll(" ","");
                            translate_english(k);
                        }
                        else{
                            translate_english(sentence);
                        }
                    }
                    else if(sentence.indexOf("banaan")!=-1){
                        //Toast.makeText(DictionaryActivity.this,sentence+","+matches.get(0),Toast.LENGTH_LONG).show();
                        String[] arr=sentence.split("banaan");
                        //Toast.makeText(DictionaryActivity.this,sentence+","+arr[0]+","+arr[1],Toast.LENGTH_LONG).show();
                        if(arr.length>0 && arr[0].equals("")){
                            String temp=arr[1];
                            String k=temp.replaceAll(" ","");
                            translate_english(k);
                        }
                        else{
                            translate_english(sentence);
                        }
                        //
                    }
                    else{
                        translate_english(sentence);
                    }
                }
                else if(language.equals("bangla")){
                    translate_bangla(sentence);
                }

            }

        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }

    private void translate_bangla(String inputtext1) {
        Toast.makeText(DictionaryActivity.this,""+inputtext1,Toast.LENGTH_LONG).show();
        //Install Firebase Model English

        final String[] inputText = {inputtext1};
        FirebaseTranslatorOptions options =
                new FirebaseTranslatorOptions.Builder()
                        .setSourceLanguage(FirebaseTranslateLanguage.BN)
                        .setTargetLanguage(FirebaseTranslateLanguage.EN)
                        .build();
        FirebaseTranslator translator =
                FirebaseNaturalLanguage.getInstance().getTranslator(options);

        translator.translate(inputText[0])
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(@NonNull String translatedText) {
                                if(translatedText.indexOf(" ")!=-1){
                                    String k=translatedText.replaceAll(".","$0,");
                                    String banaan=k.trim().replaceAll(", ",",তারপর");
                                    //Toast.makeText(DictionaryActivity.this,""+inputtext1+",,"+k+"banaan"+banaan,Toast.LENGTH_LONG).show();
                                    mobile_speak(inputText[0]+",বাক্যটির ইংরেজি হল, " +translatedText+",বানান হল,"+banaan,(float)0.7);
                                }
                                else{
                                    String k=translatedText.replaceAll(".","$0,");
                                    mobile_speak(inputText[0]+",শব্দটির ইংরেজি হল, " +translatedText+",বানান হল,"+k,(float)0.7);
                                }
                                finish();
                                //Toast.makeText(MainActivity.this,"tranlate"+inputText2[0],Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(DictionaryActivity.this,"exception"+e.getMessage(),Toast.LENGTH_LONG).show();
                                mobile_speak("দুঃখিত,"+inputText[0]+ " শব্দটির ইংরেজি খুজে পাওয়া যায় নি",(float)0.8);
                                finish();
                            }
                        });
    }

    private void translate_english(String inputtext1) {

        Toast.makeText(DictionaryActivity.this,""+inputtext1,Toast.LENGTH_LONG).show();
        //Install Firebase Model English

        final String[] inputText = {inputtext1};
        FirebaseTranslatorOptions options =
                new FirebaseTranslatorOptions.Builder()
                        .setSourceLanguage(FirebaseTranslateLanguage.EN)
                        .setTargetLanguage(FirebaseTranslateLanguage.BN)
                        .build();
        FirebaseTranslator translator =
                FirebaseNaturalLanguage.getInstance().getTranslator(options);

        translator.translate(inputText[0])
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(@NonNull String translatedText) {
                                if(translatedText.indexOf(" ")!=-1){

                                    mobile_speak(inputText[0]+",বাক্যটির বাংলা অর্থ হল, " +translatedText+",,",(float)0.7);

                                }
                                else{
                                    mobile_speak(inputText[0]+",শব্দটির বাংলা অর্থ হল, " +translatedText+",,",(float)0.7);
                                }
                                finish();
                                //Toast.makeText(MainActivity.this,"tranlate"+inputText2[0],Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(DictionaryActivity.this,"exception"+e.getMessage(),Toast.LENGTH_LONG).show();
                                mobile_speak("দুঃখিত,"+inputText[0]+ " শব্দটির বাংলা অর্থ খুজে পাওয়া যায় নি",(float)0.8);
                                finish();
                            }
                        });


    }
    private void mobile_speak(String str,float rate) {

        mtts.setPitch((float) 0.8);
        mtts.setSpeechRate(rate);
        mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);

    }
}
