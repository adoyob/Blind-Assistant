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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.blindassistant.MainActivity;
import com.example.blindassistant.R;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public class DetectorActivity2 extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  private static final int TF_OD_API_INPUT_SIZE = 300;
  private static final boolean TF_OD_API_IS_QUANTIZED = true;
  private static final String TF_OD_API_MODEL_FILE = "detect_money1.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labels.txt";
  private static final String TF_OD_API_MODEL_FILE9 = "detect_money9.tflite";
  private static final String TF_OD_API_LABELS_FILE9 = "file:///android_asset/labels(1).txt";
  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;
  private Button bt;
  private TextToSpeech mtts;
  private Classifier detector;
  private Classifier detector2;
  private List<String> object_name = Arrays.asList("one_এক","two_দুই","five_পাঁচ","ten_দশ","twenty_বিশ","fifty_পঞ্চাশ","one hundred_একশ",
          "five hundred_পাঁচশ","one thousand_এক হাজার");
  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;
  private Bitmap moneycrop=null;
  int it=0;
  private boolean computingDetection = false;

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
      detector2 =
              TFLiteObjectDetectionAPIModel.create(
                      getAssets(),
                      TF_OD_API_MODEL_FILE9,
                      TF_OD_API_LABELS_FILE9,
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
          mobile_speak("মোবাইল এবং টাকা সোজা করে ধরুন বা টাকা হাতে সোজা করে রাখুন এবং স্ক্রিনে টাচ করুন । আর ফিরে যেতে চাইলে একসাথে দুইবার স্ক্রিনে টাচ করুন");

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
    bt=(Button) findViewById(R.id.buttonid3);
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
    final ArrayList<String> money = new ArrayList<String>();
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
                if(location.left>=0 && location.top>=0 && location.width()>=0 && location.height()>=0 && (location.width()+location.left)<=300 && (location.top+location.height())<=300) {
                  moneycrop = Bitmap.createBitmap(cropCopyBitmap, (int) location.left, (int) location.top, (int) location.width(), (int) location.height());

                  // Log.i(TAG, "1");
                  Bitmap moneycopyCrop = moneycrop.copy(Config.ARGB_8888, true);
                  Bitmap resized = Bitmap.createScaledBitmap(moneycopyCrop, 300, 300, true);
                  final List<Classifier.Recognition> results2 = detector2.recognizeImage(resized);
                  for (final Classifier.Recognition result2 : results2) {
                    final RectF location2 = result2.getLocation();
                    if (location2 != null && result2.getConfidence() >= minimumConfidence) {
                        money.add((String) result2.getTitle());
                    }
                  }
                }
                canvas.drawRect(location, paint);

                cropToFrameTransform.mapRect(location);

                result.setLocation(location);
                mappedRecognitions.add(result);
              }
            }


            tracker.trackResults(mappedRecognitions, currTimestamp);
            trackingOverlay.postInvalidate();

            computingDetection = false;

            runOnUiThread(
                new Runnable() {
                  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                  @Override
                  public void run() {
                    showFrameInfo(previewWidth + "x" + previewHeight);
                    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                    showInference(lastProcessingTimeMs + "ms");
                  }
                });
          }
        });
    bt.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mtts.stop();
        it++;
        Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
          @Override
          public void run() {
            if(it==1){


                  String st="এখানে";
                  if(!money.isEmpty()) {
                    String ob_name=null;
                    for (int i = 0; i < money.size(); i++) {
                      int occurrences = Collections.frequency(money, money.get(i));
                      ob_name=object_name_bangla(money.get(i));
                      st+=" "+ob_name+" টাকার নোট "+occurrences+" টি,";
                    }
                  }else{
                    st="দুঃখিত কিছু খুজে পাওয়া যায়নি";
                  }
                  mobile_speak(st);
                  Toast.makeText(DetectorActivity2.this," "+money+" "+st,Toast.LENGTH_LONG).show();




              //personName=new ArrayList<String>();
            }
            else if(it==2){
              mobile_speak("হোমে ফিরে এসেছেন");
              Toast.makeText(DetectorActivity2.this,"Double clicked",Toast.LENGTH_LONG).show();

              Intent intent_main=new Intent(DetectorActivity2.this, MainActivity.class);
              intent_main.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
              startActivity(intent_main);
            }
            it=0;
          }
        },500);

      }
    });

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
  private void mobile_speak(String str) {

    mtts.setPitch((float) 1.2);
    mtts.setSpeechRate((float) 0.8);
    mtts.speak(str, TextToSpeech.QUEUE_FLUSH, null);

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

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }
}
