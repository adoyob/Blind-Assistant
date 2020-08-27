package com.example.blindassistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;

import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.Converters;
import org.tensorflow.lite.examples.detection.DetectorActivity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;



public class TrainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static String TAG = TrainActivity.class.getSimpleName();
    private CameraBridgeViewBase openCVCamera;
    private Mat rgba,gray;
    private boolean addface=false;
    private boolean deleteface=false;
    public static final int CAMERA_ID_ANY   = -1;
    public static final int CAMERA_ID_BACK  = 99;
    public static final int CAMERA_ID_FRONT = 98;
    private CascadeClassifier classifier;
    private MatOfRect faces;
    private static final int PERMS_REQUEST_CODE = 123;
    private ArrayList<Mat> images=new ArrayList<Mat>();
    private ArrayList<String> imagesLabels=new ArrayList<String>();
    private ArrayList<String> name=new ArrayList<String>();
    private Storage local;
    private File xmlFile,txtFile,dataPath,tempImgFile,tempImgFile2;
    private String[] uniqueLabels;
    private boolean txtFileNotEmpty=false;
    private boolean delName=false;
    private TextToSpeech mtts;
    int btn_i=0;
    String del;
    private SpeechRecognizer speech ;
    FaceRecognizer recognize;
    private void trainfaces() {
        //if(images.isEmpty())
            //return false;
        if(!images.isEmpty() && !name.isEmpty() && images.size()==name.size())
        {
            List<Mat> imagesMatrix = new ArrayList<>();
            Log.i(TAG, "1");
            for (int i = 0; i < images.size(); i++)
                imagesMatrix.add(images.get(i));
            Log.i(TAG, "2");
            Set<String> uniqueLabelsSet = new HashSet<>(name); // Get all unique labels
            uniqueLabels = uniqueLabelsSet.toArray(new String[uniqueLabelsSet.size()]); // Convert to String array, so we can read the values from the indices
            Log.i(TAG, "3");
            int[] classesNumbers = new int[uniqueLabels.length];
            for (int i = 0; i < classesNumbers.length; i++)
                classesNumbers[i] = i + 1; // Create incrementing list for each unique label starting at 1
            Log.i(TAG, "4");
            int[] classes = new int[name.size()];
            for (int i = 0; i < name.size(); i++) {
                String label = name.get(i);
                for (int j = 0; j < uniqueLabels.length; j++) {
                    if (label.equals(uniqueLabels[j])) {
                        classes[i] = classesNumbers[j]; // Insert corresponding number
                        break;
                    }
                }
            }
            Log.i(TAG, "5");
            Mat vectorClasses = new Mat(classes.length, 1, CvType.CV_32SC1); // CV_32S == int
            vectorClasses.put(0, 0, classes); // Copy int array into a vector
            Log.i(TAG, "6");
            recognize = LBPHFaceRecognizer.create(3,8,8,8,200);

            Log.i(TAG, "7");
            SaveImage(imagesMatrix,vectorClasses);

        }

        //if(SaveImage())
            //return true;

        //return false;
    }
    public void showLabelsDialog() {
        Set<String> uniqueLabelsSet = new HashSet<>(name); // Get all unique labels
        if (!uniqueLabelsSet.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select label:");
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    images.remove(images.size()-1);
                }
            });
            builder.setCancelable(false); // Prevent the user from closing the dialog

            String[] uniqueLabels = uniqueLabelsSet.toArray(new String[uniqueLabelsSet.size()]); // Convert to String array for ArrayAdapter
            //Arrays.sort(uniqueLabels); // Sort labels alphabetically
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, uniqueLabels) {
                @Override
                public @NonNull
                View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView textView = (TextView) super.getView(position, convertView, parent);
                    if (getResources().getBoolean(R.bool.isTablet))
                        textView.setTextSize(20); // Make text slightly bigger on tablets compared to phones
                    else
                        textView.setTextSize(18); // Increase text size a little bit
                    return textView;
                }
            };
            String[] animals = {"add new Person"};
            builder.setItems(animals, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:  showEnterLabelDialog();

                    }
                }
            });
            ListView mListView = new ListView(this);
            mListView.setAdapter(arrayAdapter); // Set adapter, so the items actually show up
            builder.setView(mListView); // Set the ListView

            final AlertDialog dialog = builder.show(); // Show dialog and store in final variable, so it can be dismissed by the ListView

            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    dialog.dismiss();
                    addLabel(arrayAdapter.getItem(position));
                    Log.i(TAG, "Labels Size "+imagesLabels.size()+"");
                }
            });
        } else {
            showEnterLabelDialog();
        }

    }
    private void showEnterLabelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter your name:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Submit", null); // Set up positive button, but do not provide a listener, so we can check the string before dismissing the dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                images.remove(images.size()-1);
            }
        });
        builder.setCancelable(false); // User has to input a name
        AlertDialog dialog = builder.create();

        // Source: http://stackoverflow.com/a/7636468/2175837
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button mButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String string = input.getText().toString().trim();
                        if (!string.isEmpty()) { // Make sure the input is valid
                            // If input is valid, dismiss the dialog and add the label to the array
                            dialog.dismiss();
                            addLabel(string);
                        }
                    }
                });
            }
        });
        // Show keyboard, so the user can start typing straight away
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }
    private void addLabel(String string) {
        //String label = string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1).trim().toLowerCase(Locale.US); // Make sure that the name is always uppercase and rest is lowercase
        imagesLabels.add(string);
        name.add(string);// Add label to list of labels
        Log.i(TAG, "Label: " + string);
        Toast.makeText(TrainActivity.this,"images= "+images.size()+" names= "+name+"iamgeMat= "+images,Toast.LENGTH_LONG).show();

    }
    public void SaveImage(List<Mat> imagesMatrix,Mat vectorClasses)  {
        String filename = "lbph_trained_data.xml";
        //Log.i(TAG, "8");
        xmlFile = new File(dataPath, filename);
        recognize.train(imagesMatrix, vectorClasses);
        recognize.save(xmlFile.toString());
        writeTxtFile();
    }
    public void saveToFile(Bitmap bmp) {
        try {
            FileOutputStream out = new FileOutputStream(tempImgFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch(Exception e) {}
    }


    private void writeTxtFile() {
        int i=0;
        for (Mat mat : images) {
            Bitmap bm=Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat,bm);
            tempImgFile=new File(dataPath,name.get(i)+".png");
            saveToFile(bm);

            i=i+1;
        }
        Toast.makeText(TrainActivity.this,"images= "+images,Toast.LENGTH_LONG).show();
        /*try {
            FileOutputStream stream = new FileOutputStream(txtFile);
            ObjectOutputStream out = new ObjectOutputStream(stream);
            out.reset();
            out.writeBytes(String.valueOf(name));
            out.close();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        local.putListString("names", name);

    }


    public void readTxtFile() {
        StringBuilder stringBuilder = new StringBuilder();
        String line=null;
        BufferedReader in = null;




        try {
            //FileInputStream in = new FileInputStream(txtFile);
            in = new BufferedReader(new FileReader(txtFile));
            Toast.makeText(TrainActivity.this,"in "+in,Toast.LENGTH_LONG).show();
            if(in!=null){
                line=in.readLine();

                Toast.makeText(TrainActivity.this,"line "+line,Toast.LENGTH_LONG).show();
                while (line != null)
                    stringBuilder.append(line);

                if(line.isEmpty() && !imagesLabels.isEmpty()) {
                    try {
                        FileWriter out = new FileWriter(txtFile);
                        out.write(String.valueOf(imagesLabels));
                        out.close();
                        txtFileNotEmpty=true;
                        //name.add(String.valueOf(imagesLabels));

                    } catch (IOException e) {

                    }
                }
                else{
                    name.add(stringBuilder.toString());
                }
            }

        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }

        //return stringBuilder.toString();
    }

    public void cropedImages(Mat mat) {
        Rect rect_Crop=null;
        for(Rect face: faces.toArray()) {
            rect_Crop = new Rect(face.x, face.y, face.width, face.height);
        }
        Mat croped = new Mat(mat, rect_Crop);

        images.add(croped);
    }
    public Bitmap loadFromFile(File filename) {
        try {
            //File f = new File(filename);
            if (!filename.exists()) { return null; }
            Bitmap tmp = BitmapFactory.decodeFile(filename.getAbsolutePath());
            return tmp;
        } catch (Exception e) {
            return null;
        }
    }
    private void mobile_speak(String str) {

        mtts.setPitch((float) 0.8);
        mtts.setSpeechRate((float)0.7);
        mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);

    }
    class listener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(TrainActivity.this, "Start Listening", Toast.LENGTH_LONG).show();
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
            Toast.makeText(TrainActivity.this, "Stop Listening", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(int error) {
            if (error == 7) {
                mobile_speak("দুঃখিত কিছু বুঝা যায় নি");
                onBack();
            }
            else if (error == 5) {
                //Toast.makeText(MainActivity.this,"Error Listening"+error,Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String sentence=matches.get(0).trim();
            if(matches!=null) {
                if(delName){
                    boolean found=false;
                    int in=0;
                    for(String temp:name){
                        if(temp.equals(sentence)){
                            name.remove(in);
                            images.remove(in);
                            found=true;
                            break;
                        }
                        in++;
                    }

                    if(found){
                        trainfaces();
                        if(name.isEmpty() && images.isEmpty()){
                            Boolean h=TrainActivity1.deleteDirectory(dataPath);
                            local.putListString("names",name);
                        }
                        mobile_speak(sentence+" নামটিি এবং উনার ছবি মুছে ফেলা হয়েছে");
                        Toast.makeText(TrainActivity.this,"name: "+name+" images: "+images,Toast.LENGTH_LONG).show();
                    }else{
                        mobile_speak("দুঃখিত "+sentence+" নামে কিছু খুজে পাওয়া যায় নি ");
                    }
                    final Handler handler=new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onBack();
                        }
                    },3500);

                }else {
                    addLabel(sentence);
                    trainfaces();
                    mobile_speak(" নতুন "+sentence+" নামটি এবং উনার ছবি যোগ করা হয়েছে");
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



    private void person_input_speak_bangla() {
        speech = SpeechRecognizer.createSpeechRecognizer(TrainActivity.this);


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
        speech.setRecognitionListener(new TrainActivity.listener());

        speech.startListening(intent);
        new CountDownTimer(4000,1000){

            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                speech.stopListening();
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.train_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Stetho.initializeWithDefaults(this);

        if (hasPermissions()){
            Log.i(TAG, "Permission Granted Before");

        }
        else {
            mobile_speak("কারও সাহায্য নিয়ে পারমিশনগুলো এলাউ করুন");
            requestPerms();
        }
        mtts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mtts.setLanguage(new Locale("bn_BD"));
                    if(del.equals("yes"))
                        mobile_speak("যার নাম কাটতে চান তার নাম বলুন");
                    else if(del.equals("no"))
                        mobile_speak("উনাকে কেমেরার সামনে থাকতে বলুন এবং স্ক্রিনে একবার টাচ করুন আর ফিরে যেতে চাইলে একসাথে দুইবার টাচ করুন");
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    } else {
                        
                    }

                } else {

                }
            }
        });
        dataPath = new File(Environment.getExternalStorageDirectory(), "BlindAssistant");
        if(!dataPath.exists())
            Log.i(TAG, "filename not found");
            dataPath.mkdirs();
        String filename = "names.txt";
        txtFile = new File(dataPath, filename);


        openCVCamera = (CameraBridgeViewBase)findViewById(R.id.java_camera_view);
        //openCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        openCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
        openCVCamera.getScreenOrientation();

        //Toast.makeText(TrainActivity.this,"sco "+openCVCamera.getScreenOrientation(),Toast.LENGTH_LONG).show();
        openCVCamera.setVisibility(CameraBridgeViewBase.VISIBLE);
        openCVCamera.setCvCameraViewListener(this);
        local = new Storage(this);



        Button detect = (Button)findViewById(R.id.take_picture_button);
        detect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mtts.stop();
                btn_i++;
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(TrainActivity.this,"Btn "+btn_i,Toast.LENGTH_LONG).show();
                        if (btn_i == 1) {
                            if (del.equals("no")) {
                                if (gray.total() == 0)
                                    Toast.makeText(getApplicationContext(), "Can't Detect Faces", Toast.LENGTH_SHORT).show();
                                classifier.detectMultiScale(gray, faces, 1.1, 3, 0 | CASCADE_SCALE_IMAGE, new Size(30, 30));
                                if (!faces.empty()) {
                                    if (faces.toArray().length > 1)
                                        mobile_speak("কেমেরার সামনে একজন হতে হবে । আবার চেষ্টা করুন");
                                    else {
                                        if (gray.total() == 0) {
                                            mobile_speak("দুঃখিত ছবি বুঝা যাচ্ছে না । আবার চেষ্টা করুন");
                                        }
                                        cropedImages(gray);
                                        //showLabelsDialog();
                                        mobile_speak("ছবি তোলা হয়েছে, এখন নাম বলুন");
                                        final Handler handler = new Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                person_input_speak_bangla();
                                            }
                                        }, 2000);

                                        Toast.makeText(getApplicationContext(), "Face Detected", Toast.LENGTH_SHORT).show();
                                    }
                                } else
                                    mobile_speak("দুঃখিত ছবি বুঝা যাচ্ছে না । আবার চেষ্টা করুন");

                            }

                        }
                        else if (btn_i == 2) {
                            Toast.makeText(TrainActivity.this, "Double clicked", Toast.LENGTH_LONG).show();
                            onBack();
                            //finish();
                            //onBack();
                        }
                        btn_i = 0;
                    }
                }, 500);

            }

        });
        /*Button back=(Button) findViewById(R.id.camera);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(del.equals("no")) {
                    openCVCamera.swapCamera();
                   /* int k=openCVCamera.getmCameraIndex();
                    Toast.makeText(TrainActivity.this,"index "+k,Toast.LENGTH_LONG).show();
                    int index = CameraBridgeViewBase.CAMERA_ID_FRONT;
                    if (k == CAMERA_ID_FRONT)
                        index = CameraBridgeViewBase.CAMERA_ID_BACK;
                    openCVCamera.disableView();
                    openCVCamera.setCameraIndex(index);
                    openCVCamera.setVisibility(CameraBridgeViewBase.VISIBLE);
                    openCVCamera.setCvCameraViewListener(TrainActivity.this);
                    openCVCamera.enableView();

                }
            }
        });
*/


    }


    private void onBack(){
        mobile_speak("পরিচিতি পর্বে ফিরে এসেছেন");
        finish();
        //Intent intent_train=new Intent(TrainActivity.this, TrainActivity1.class);
        //intent_train.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //startActivity(intent_train);
    }

    @SuppressLint("WrongConstant")
    private boolean hasPermissions(){
        int res = 0;
        //string array of permissions,
        String[] permissions = new String[]{Manifest.permission.CAMERA};

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
            Log.i(TAG, "Permission has been added");
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
    private BaseLoaderCallback callbackLoader = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case BaseLoaderCallback.SUCCESS:
                    faces = new MatOfRect();
                    openCVCamera.enableView();
                    name = local.getListString("names");

                    if(!name.isEmpty()) {
                        int i=0;
                        for (String iamgeName : name){
                            tempImgFile2=new File(dataPath,name.get(i)+".png");
                            Bitmap bm=loadFromFile(tempImgFile2);

                            Mat temp=new Mat();
                            Utils.bitmapToMat(bm,temp);
                            Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2GRAY);
                            images.add(temp);
                            i=i+1;

                        }
                    }
                    Bundle bundle=getIntent().getExtras();
                    del=bundle.getString("delete");
                    if(del.equals("yes")){
                        Toast.makeText(TrainActivity.this,"name= "+name+" image= "+images.size()+" Mat= "+images,Toast.LENGTH_LONG).show();
                        delName=true;
                        if(!name.isEmpty()) {

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    person_input_speak_bangla();
                                }
                            }, 3500);
                        }else{
                            mobile_speak("এখনও কোন নাম বা ছবি যোগ করা হয় নাই ।");
                            onBack();
                        }

                    }else if(del.equals("no")){
                        Toast.makeText(TrainActivity.this,"name= "+name+" image= "+images.size()+" Mat= "+images,Toast.LENGTH_LONG).show();

                    }

                    //images = local.getListMat("images");
                    Log.i(TAG, "Images "+images.size());

                    //name = local.getListString("imagesLabels");
                    Log.i(TAG, "ImagesLabels "+imagesLabels);
                    //Toast.makeText(TrainActivity.this,"name= "+name+" image= "+images.size()+" Mat= "+images+" del "+del+" boolean yes "+del.equals("yes")+" boolean no "+del.equals("no"),Toast.LENGTH_LONG).show();


                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if(openCVCamera != null)
            openCVCamera.disableView();


    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (images != null && imagesLabels != null) {
            //local.putListMat("images", images);
            //local.putListString("imagesLabels", imagesLabels);
            Log.i(TAG, "Images have been saved");
            //if(trainfaces()) {
               // images.clear();
              //  imagesLabels.clear();
            //}
        }
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(openCVCamera != null)
            openCVCamera.disableView();
    }
    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()) {
            Log.i(TAG, "System Library Loaded Successfully");
            callbackLoader.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.i(TAG, "Unable To Load System Library");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, callbackLoader);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        rgba = new Mat();
        gray = new Mat();
        classifier = FileUtils.loadXMLS(this, "lbpcascade_frontalface_improved.xml");
    }

    @Override
    public void onCameraViewStopped() {
        rgba.release();
        gray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mGrayTmp = inputFrame.gray();
        Mat mRgbaTmp = inputFrame.rgba();

        int id=openCVCamera.getmCameraIndex();
        int orientation=openCVCamera.getScreenOrientation();
        if (openCVCamera.isEmulator()) // Treat emulators as a special case
            Core.flip(mRgbaTmp, mRgbaTmp, 1); // Flip along y-axis
        else {
            switch (orientation) { // RGB image
                case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                    Core.flip(mRgbaTmp, mRgbaTmp, 0); // Flip along x-axis
                    break;
                case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                    Core.flip(mRgbaTmp, mRgbaTmp, 1); // Flip along y-axis
                    break;
            }
            switch (orientation) { // Grayscale image
                case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                    Log.i(TAG, "gray,potrait");
                    Core.transpose(mGrayTmp, mGrayTmp); // Rotate image
                    if(id==99){
                        Core.flip(mGrayTmp, mGrayTmp, 1);
                    }
                    else if(id==98){
                        Core.flip(mGrayTmp, mGrayTmp, -1);
                    }
                     // Flip along both axis
                    break;
                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                    Log.i(TAG, "gray reverse potrait");
                    Core.transpose(mGrayTmp, mGrayTmp); // Rotate image
                    //Core.flip(mGrayTmp, mGrayTmp, -1);
                    break;
                case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                    Log.i(TAG, "gray landscape");
                    Core.flip(mGrayTmp, mGrayTmp, 1); // Flip along y-axis
                    break;
                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                    Log.i(TAG, "gray reverse_land");
                    Core.flip(mGrayTmp, mGrayTmp, 0); // Flip along x-axis
                    break;
            }
        }
        gray = mGrayTmp;
        rgba = mRgbaTmp;
        Imgproc.resize(gray, gray, new Size(200,200.0f/ ((float)gray.width()/ (float)gray.height())));
        return rgba;
    }
}
