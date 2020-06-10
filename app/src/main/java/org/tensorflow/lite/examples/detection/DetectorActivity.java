/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.blindassistant.MainActivity;
import com.example.blindassistant.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  int i=0;
  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;
  private String str_name;
  private Classifier detector;
  private List<String> object_name = Arrays.asList("person_মানুষ","bicycle_সাইকেল","boat_নৌকা","bird_পাখি","cat_বিড়াল","dog_কুকুর","horse_ঘোড়া",
            "sheep_ভেড়া","cow_গরু","elephant_হাতি","bear_ভালুক","umbrella_ছাতা","kite_ঘুড়ি","bottle_বোতল","fork_কাটাচামচ","knife_ছুরি","spoon_চামচ",
            "banana_কলা","orange_কমলা","broccoli_ফুলকপি","carrot_গাজর","couch_পালং","potted plant_টব","bed_বিছানা", "cell phone_মোবাইল",
            "book_বই","clock_ঘড়ি","scissors_কাচি");
  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private Button button;
  private boolean computingDetection = false;
  private TextToSpeech mtts;
  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);

    int cropSize = TF_OD_API_INPUT_SIZE;

    try {
      detector =
          TFLiteObjectDetectionAPIModel.create(
              getAssets(),
              TF_OD_API_MODEL_FILE,
              TF_OD_API_LABELS_FILE,
              TF_OD_API_INPUT_SIZE,
              TF_OD_API_IS_QUANTIZED);
      cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing classifier!");
      Toast toast =
          Toast.makeText(
              getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }
      mtts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
          @Override
          public void onInit(int status) {
              if (status == TextToSpeech.SUCCESS) {
                  int result = mtts.setLanguage(new Locale("bn_BD"));
                  mobile_speak("মোবাইল সোজা করে ধরুন এবং স্ক্রিনে টাচ করুন । আর ফিরে যেতে চাইলে একসাথে দুইবার স্ক্রিনে টাচ করুন");

                  if (result == TextToSpeech.LANG_MISSING_DATA ||
                          result == TextToSpeech.LANG_NOT_SUPPORTED) {


                  } else {
                  }

              } else {


              }
          }
      });
    previewWidth = size.getWidth();
    previewHeight = size.getHeight();
    button=(Button) findViewById(R.id.buttonid3);

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

    frameToCropTransform =
        ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropSize, cropSize,
            sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);

    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
        new DrawCallback() {
          @Override
          public void drawCallback(final Canvas canvas) {
            tracker.draw(canvas);
            if (isDebug()) {
              tracker.drawDebug(canvas);
            }
          }
        });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }

  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }
    final ArrayList<String> re = new ArrayList<String>();
    final ArrayList<String> reL = new ArrayList<String>();
    final ArrayList<String> reR = new ArrayList<String>();
    final ArrayList<String> reM = new ArrayList<String>();
    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            LOGGER.i("Running detection on image " + currTimestamp);
            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
            final Canvas canvas = new Canvas(cropCopyBitmap);
            final Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2.0f);

            float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            switch (MODE) {
              case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
            }

            final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();

            for (final Classifier.Recognition result : results) {
              final RectF location = result.getLocation();
              if (location != null && result.getConfidence() >= minimumConfidence) {
                  if(location.left<150.00){
                      if(location.right<140.00){
                          re.add((String) result.getTitle()+"_L");
                          continue;
                      }
                      else{
                          re.add((String) result.getTitle()+"_M");
                          continue;
                      }
                  }
                  else if(location.right>150.00){
                      if(location.left>140.00){
                          re.add((String) result.getTitle()+"_R");
                          continue;
                      }
                      else
                      {
                          re.add((String) result.getTitle()+"_M");
                          continue;
                      }

                  }
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }
              for (int i = 0; i < re.size(); i++) {
                  String[] k = re.get(i).split("_");
                  String n="টি";
                  String ob_name=null;
                  int count = 1;

                  for (int j = i; j < re.size(); j++) {
                      if(i !=  j)
                      {
                          if (re.get(i).equals(re.get(j))) {
                              count = count + 1;
                              re.remove(j);
                              j=j-1;
                          }
                      }

                  }

                  if(k[0].equals("person"))
                      n="জন";
                  ob_name=object_name_bangla(k[0]);
                  if(k[1].equals("L"))
                      reL.add((String) ob_name + " " + Integer.toString(count)+" "+n);
                  else if(k[1].equals("R"))
                      reR.add((String) ob_name + " " + Integer.toString(count)+" "+n);
                  else if(k[1].equals("M"))
                      reM.add((String) ob_name + " " + Integer.toString(count)+" "+n);
              }
              if(reL.isEmpty()==false) {
                  reL.add(0, "বামে");
                  if(reM.isEmpty()==false){
                      reL.add((int)reL.size(), "তারপরে");
                  }
              }


              if(reR.isEmpty()==false) {
                  reR.add(0, "ডানে");
              }
              if(reM.isEmpty()==false){
                  reM.add(0,"মাঝখানে");
                  if(reR.isEmpty()==false){
                      reM.add((int)reM.size(),"তারপরে");
                  }
              }


              String left=reL.toString().replace("[","").replace("]","");
              String middle=reM.toString().replace("[","").replace("]","");
              String right=reR.toString().replace("[","").replace("]","");
              //Toast.makeText(CameraActivity.this, results+""+left+","+middle+","+right, Toast.LENGTH_LONG).show();
              if(reL.isEmpty()==false||reM.isEmpty()==false||reR.isEmpty()==false){
                  //mobile_speak("আপনার সামনে "+left+","+middle+","+right);
                  str_name=left+" "+middle+" "+right;
              }
              else{
                  str_name="দুঃখিত কিছু খুজে পাওয়া যায়নি";
                  //mobile_speak("দুঃখিত কিছু খুজে পাওয়া যায়নি");
              }

            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                  }
                });
          }
        });
      button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              mtts.stop();
              i++;
              Handler handler=new Handler();
              handler.postDelayed(new Runnable() {
                  @Override
                  public void run() {
                     if(i==1){
                         Toast.makeText(DetectorActivity.this,""+str_name,Toast.LENGTH_LONG).show();
                         mobile_speak(str_name);
                     }
                     else if(i==2){
                         Toast.makeText(DetectorActivity.this,"Double clicked",Toast.LENGTH_LONG).show();
                         mobile_speak("হোমে ফিরে এসেছেন");
                         Intent intent_main=new Intent(DetectorActivity.this, MainActivity.class);
                         intent_main.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                         startActivity(intent_main);
                     }
                     i=0;
                  }
              },500);

          }
      });
  }


  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }
  private void mobile_speak(String str) {

        mtts.setPitch((float) 0.8);
        mtts.setSpeechRate((float) 0.8);
        mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);

    }
  private String object_name_bangla(String s) {

        for(int i=0;i<object_name.size();i++)
        {
            String[] k = object_name.get(i).split("_");
            if(s.equals(k[0])){
                s=k[1];
                break;
            }
        }
        return s;
    }
  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }
}
