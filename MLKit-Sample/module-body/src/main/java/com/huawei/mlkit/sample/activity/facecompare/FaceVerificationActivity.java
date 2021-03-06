/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.mlkit.sample.activity.facecompare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.common.MLException;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.faceverify.MLFaceTemplateResult;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzer;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzerFactory;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationAnalyzerSetting;
import com.huawei.hms.mlsdk.faceverify.MLFaceVerificationResult;
import com.huawei.mlkit.sample.R;
import com.huawei.mlkit.sample.activity.face3d.Live3DFaceAnalyseActivity;
import com.huawei.mlkit.sample.util.BitmapUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.huawei.mlkit.sample.R.layout.activity_face_verification;

public class FaceVerificationActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "FaceVerificationActivity";

    private static final int FACEMAX = 3;

    ImageView template;

    ImageView compare;

    RelativeLayout templateRe;

    RelativeLayout compareRe;

    TextView templateTextView;

    TextView compareTextView;

    TextView resultTextView;

    ImageView templatePreview;

    ImageView comparePreview;

    ImageView back;

    Button compareBtn;

    MLFaceVerificationAnalyzer analyzer;

    private int REQUEST_CHOOSE_TEMPLATEPIC = 2001;

    private int REQUEST_CHOOSE_COMPAEPIC = 2002;

    private Bitmap templateBitmap;
    private Bitmap compareBitmap;
    private Bitmap templateBitmapCopy;
    private Bitmap compareBitmapCopy;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analyzer != null) {
            analyzer.stop();
        }
        BitmapUtils.recycleBitmap(templateBitmap);
        BitmapUtils.recycleBitmap(templateBitmapCopy);
        BitmapUtils.recycleBitmap(compareBitmap);
        BitmapUtils.recycleBitmap(compareBitmapCopy);
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_face_verification);
        template = findViewById(R.id.img_template);
        template.setOnClickListener(this);
        compare = findViewById(R.id.img_verify);
        compare.setOnClickListener(this);
        compareBtn = findViewById(R.id.btn_verify);
        compareBtn.setOnClickListener(this);
        templateRe = findViewById(R.id.template);
        templateRe.setOnClickListener(this);
        compareRe = findViewById(R.id.verify);
        compareRe.setOnClickListener(this);
        back = findViewById(R.id.back);
        back.setOnClickListener(this);
        templateTextView = findViewById(R.id.txt_template);
        compareTextView = findViewById(R.id.txt_verify);

        templatePreview = findViewById(R.id.tempPreview);
        comparePreview = findViewById(R.id.compPreview);
        resultTextView = findViewById(R.id.edit_text);

        /**
        Intent intent = getIntent();
        if (intent.getAction() == null) {
            int i = 0;
            while (true) {
                synchronized (this) {
                    Log.i(TAG, "Times: " + ++i);
                    run_fy();       //the full task flow
                }
            }
        } else if (intent.getAction().equals("fy.action.out_command")) {
            int times = intent.getIntExtra("times", 1);

            // adb shell am start
            // -n com.mlkit.sample.body/com.huawei.mlkit.sample.activity.facecompare.FaceVerificationActivity
            // -a fy.action.out_command
            // --ei times 5

            int i = 0;
            for (; i < times; i++) {
                synchronized (this) {
                    Log.i(TAG, "Times: " + (i + 1));
                    run_fy();       //the full task flow
                }
            }
        } else {
            Log.e(TAG, "[E] UNKNOWN COMMAND");
        }
         **/
        run_fy();
//        initAnalyzer();
    }

    private void initAnalyzer() {
        MLFaceVerificationAnalyzerSetting.Factory factory = new MLFaceVerificationAnalyzerSetting.Factory().setMaxFaceDetected(FACEMAX);
        MLFaceVerificationAnalyzerSetting setting = factory.create();
        analyzer = MLFaceVerificationAnalyzerFactory
                .getInstance()
                .getFaceVerificationAnalyzer(setting);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.template:
            case R.id.img_template:
                BitmapUtils.recycleBitmap(templateBitmap);
                BitmapUtils.recycleBitmap(templateBitmapCopy);
                selectLocalImage(REQUEST_CHOOSE_TEMPLATEPIC);
                break;
            case R.id.verify:
            case R.id.img_verify:
                BitmapUtils.recycleBitmap(compareBitmap);
                BitmapUtils.recycleBitmap(compareBitmapCopy);
                selectLocalImage(REQUEST_CHOOSE_COMPAEPIC);
                break;
            case R.id.btn_verify:
                compare();
                break;
            case R.id.back:
                finish();
                break;

            default:
                break;
        }
    }

    @SuppressLint("LongLogTag")
    private void loadTemplatePic() {
        Log.e(TAG, "setTemplateFace");
        //  Loading template images
        if (templateBitmap == null) {
            return;
        }
        long startTime = System.currentTimeMillis();
        try {
            List<MLFaceTemplateResult> results = analyzer.setTemplateFace(MLFrame.fromBitmap(templateBitmap));
            long endTime = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            sb.append("##setTemplateFace|COST[");
            sb.append(endTime - startTime);
            sb.append("]");
            if (results.isEmpty()) {
                sb.append("Failure!");
            } else {
                sb.append("Success!");
            }
            for (MLFaceTemplateResult template : results) {
                int id = template.getTemplateId();
                Rect location = template.getFaceInfo().getFaceRect();
                Canvas canvas = new Canvas(templateBitmapCopy);
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);// Not Filled
                paint.setStrokeWidth((location.right - location.left) / 50);  // Line width
                canvas.drawRect(location, paint);// framed
                templatePreview.setImageBitmap(templateBitmapCopy);
                sb.append("|Face[");
                sb.append(location);
                sb.append("]ID[");
                sb.append(id);
                sb.append("]");
            }
            sb.append("\n");
            resultTextView.setText(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Set the image containing the face for comparison.");
        }
    }

    @SuppressLint("LongLogTag")
    private Bitmap loadPic(Uri picUri, ImageView view) {
        Bitmap pic = null;

//        pic = BitmapUtils.loadFromPath(this, picUri, ((View) view.getParent()).getWidth(),
//                ((View) view.getParent()).getHeight()).copy(Bitmap.Config.ARGB_8888, true);
//        if (pic == null) {
//            Toast.makeText(getApplicationContext(), R.string.please_select_picture, Toast.LENGTH_SHORT).show();
//        }

        // copy from SceneDectionActivity::initAnalyzer()
        try {
            pic = MediaStore.Images.Media.getBitmap(getContentResolver(), picUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        view.setImageBitmap(pic);
        return pic;
    }

    @SuppressLint("LongLogTag")
    private void compare() {

        if (compareBitmap == null) {
            return;
        }
        final long startTime = System.currentTimeMillis();
        try {
            Task<List<MLFaceVerificationResult>> task = analyzer.asyncAnalyseFrame(MLFrame.fromBitmap(compareBitmap));
            final StringBuilder sb = new StringBuilder();
            sb.append("##getFaceSimilarity|");
            task.addOnSuccessListener(new OnSuccessListener<List<MLFaceVerificationResult>>() {
                @Override
                public void onSuccess(List<MLFaceVerificationResult> mlCompareList) {
                    long endTime = System.currentTimeMillis();
                    sb.append("COST[");
                    sb.append(endTime - startTime);
                    sb.append("]|Success!");
                    for (MLFaceVerificationResult template : mlCompareList) {
                        Rect location = template.getFaceInfo().getFaceRect();
                        Canvas canvas = new Canvas(compareBitmapCopy);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);// Not Filled
                        paint.setStrokeWidth((location.right - location.left) / 50);  // Line width
                        canvas.drawRect(location, paint);// framed
                        int id = template.getTemplateId();
                        float similarity = template.getSimilarity();
                        comparePreview.setImageBitmap(compareBitmapCopy);
                        sb.append("|Face[");
                        sb.append(location);
                        sb.append("]Id[");
                        sb.append(id);
                        sb.append("]Similarity[");
                        sb.append(similarity);
                        sb.append("]");
                    }
                    sb.append("\n");
                    resultTextView.append(sb.toString());
                    analyzer.stop();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    long endTime = System.currentTimeMillis();
                    sb.append("COST[");
                    sb.append(endTime - startTime);
                    sb.append("]|Failure!");
                    if (e instanceof MLException) {
                        MLException mlException = (MLException) e;
                        // Obtain the error code. Developers can process the error code and provide differentiated page prompts based on the error code.
                        int errorCode = mlException.getErrCode();
                        // Obtain the error information. Developers can quickly locate the fault based on the error code.
                        String errorMessage = mlException.getMessage();
                        sb.append("|ErrorCode[");
                        sb.append(errorCode);
                        sb.append("]Msg[");
                        sb.append(errorMessage);
                        sb.append("]");
                    } else {
                        sb.append("|Error[");
                        sb.append(e.getMessage());
                        sb.append("]");
                    }
                    sb.append("\n");
                    resultTextView.append(sb.toString());
                    analyzer.stop();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Set the image containing the face for comparison.");
        }

    }

    private void selectLocalImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CHOOSE_TEMPLATEPIC) && (resultCode == Activity.RESULT_OK)) {
            // In this case, imageUri is returned by the chooser, save it.
            if (data == null) {
                Toast.makeText(getApplicationContext(), R.string.please_select_picture, Toast.LENGTH_SHORT).show();
                return;
            }
            templateBitmap = loadPic(data.getData(), templatePreview);
            templateBitmapCopy = templateBitmap.copy(Bitmap.Config.ARGB_8888, true);
            template.setVisibility(View.INVISIBLE);
            template.setFocusableInTouchMode(false);
            templateTextView.setVisibility(View.INVISIBLE);
            loadTemplatePic();
        } else if ((requestCode == REQUEST_CHOOSE_COMPAEPIC)) {
            if (data == null) {
                Toast.makeText(getApplicationContext(), R.string.please_select_picture, Toast.LENGTH_SHORT).show();
                return;
            }
            compareBitmap = loadPic(data.getData(), comparePreview);
            compareBitmapCopy = compareBitmap.copy(Bitmap.Config.ARGB_8888, true);
            compare.setVisibility(View.INVISIBLE);
            compare.setFocusableInTouchMode(false);
            compareTextView.setVisibility(View.INVISIBLE);
        }
    }


    @SuppressLint("LongLogTag")
    public void run_fy() {
        Log.e(TAG, "initAnalyzer");
        initAnalyzer();

        Log.e(TAG, "chooseTemplatePic");
        chooseTemplatePic();

        Log.e(TAG, "loadTemplatePic");
        loadTemplatePic();

        Log.e(TAG, "chooseComparePic");
        chooseComparePic();

        Log.e(TAG, "compare");
        compare();      // stop() is been invoked in compare() callback, so this is a single task
    }

    private void chooseTemplatePic() {
        BitmapUtils.recycleBitmap(templateBitmap);
        BitmapUtils.recycleBitmap(templateBitmapCopy);

        File image_file_temp = new File(Environment.getExternalStorageDirectory() + File.separator + "Template.png");
        Uri image_uri_temp = getImageContentUri(this, image_file_temp);
        templateBitmap = loadPic(image_uri_temp, templatePreview);
        templateBitmapCopy = templateBitmap.copy(Bitmap.Config.ARGB_8888, true);
        template.setVisibility(View.INVISIBLE);
        template.setFocusableInTouchMode(false);
        templateTextView.setVisibility(View.INVISIBLE);
    }

    private void chooseComparePic() {
        BitmapUtils.recycleBitmap(compareBitmap);
        BitmapUtils.recycleBitmap(compareBitmapCopy);

        File image_file_compare = new File(Environment.getExternalStorageDirectory() + File.separator + "Compare.png");
        Uri image_uri_compare = getImageContentUri(this, image_file_compare);
        compareBitmap = loadPic(image_uri_compare, comparePreview);
        compareBitmapCopy = compareBitmap.copy(Bitmap.Config.ARGB_8888, true);
        compare.setVisibility(View.INVISIBLE);
        compare.setFocusableInTouchMode(false);
        compareTextView.setVisibility(View.INVISIBLE);
    }

    // by fy
    public static Uri getImageContentUri(Context context, java.io.File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }
}
