package com.example.blindassistant;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView microid;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000 ;
    private TextToSpeech mtts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        microid=(CardView) findViewById(R.id.microid);
        mtts=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    int result = mtts.setLanguage(Locale.ENGLISH);
                    mobile_speak("Blind assistant Application e apnake Sha gotom");

                    if(result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED){


                    }else{

                        microid.setEnabled(true);
                    }

                }
                else{


                }
            }
        });

        microid.setOnClickListener(this);


    }
    private void mobile_speak(String str){

        mtts.setPitch((float)0.8);
        mtts.setSpeechRate((float)0.8);
        mtts.speak(str,TextToSpeech.QUEUE_FLUSH,null);

    }
    private void person_input_speak(){
        Intent intent =new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault());
        try{
            startActivityForResult(intent,REQUEST_CODE_SPEECH_INPUT);
        }
        catch(Exception e){
            Toast.makeText(this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQUEST_CODE_SPEECH_INPUT:{
                if(resultCode==RESULT_OK && null!= data){
                    ArrayList<String> result= data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    processResult(result.get(0));


                }
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
    }
    private void processResult(String command)
    {
        command= command.toLowerCase();
        if(command.indexOf("time")!=-1)
        {
            String currentTime = new SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(new Date());
            mobile_speak("the time is "+currentTime);
        }
        else if(command.indexOf("date")!=-1)
        {
            String currentDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(new Date());
            mobile_speak("The Date is "+currentDate);

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
