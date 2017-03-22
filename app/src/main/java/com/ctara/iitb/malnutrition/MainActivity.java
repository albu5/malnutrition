package com.ctara.iitb.malnutrition;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import com.ctara.iitb.malnutrition.CSVFile;

import java.text.DateFormat;
import java.util.List;
import java.io.InputStream;

import java.util.Iterator;
import java.io.IOException;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import com.ctara.iitb.malnutrition.R;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.content.ContentUris;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;


public class MainActivity extends Activity {
    Button cameraButton;
    Button galleryButton;
    ImageView mImageView;
    String mCurrentPhotoPath;
    Uri mPhotoUri;
    static Bitmap bitmapImg;
    static Bitmap bitmapScale;
    static Bitmap bitmapImgRot;

    PointF babyStartPoint;
    PointF babyEndPoint;
    PointF scaleStartPoint;
    PointF scaleEndPoint;

    boolean bmRotate = false;
    static final float SCALE_ASPECT_RATIO = (float) (85.60 / 53.98);
    static final float SCALE_LENGTH = (float) (85.60);
    static int BABY_LENGTH = (int) 6;
    static int AGE_YEARS = (int) 0;
    static int AGE_MONTHS = (int) 0;
    static int AGE_WEEKS = (int) 0;
    static double BABY_WEIGHT = (float) 0;


    int SET_STATUS = 0;

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_GET_PHOTO = 2;
//    static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 43;

    boolean TAKEN_PHOTO = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        setContentView(R.layout.activity_main);

        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        cameraButton = (Button) findViewById(R.id.buttonCamera);
        galleryButton = (Button) findViewById(R.id.buttonGallery);
        mImageView = (ImageView) findViewById(R.id.image_view);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryIntent();
            }
        });

    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_GET_PHOTO);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String timeStamp = DateFormat.getDateTimeInstance().format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getFilesDir();
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPicCamera() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        //bmOptions.inSampleSize = scaleFactor;
        bmOptions.inMutable = true;
        bitmapImg = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        int photoW;
        int photoH;
        float scaleFactor;

        if (bmOptions.outHeight < bmOptions.outWidth) {
            photoH = bmOptions.outWidth;
            photoW = bmOptions.outHeight;
            scaleFactor = Math.max(((float) photoW / targetW), ((float) photoH / targetH));
            bmRotate = true;
        } else {
            photoW = bmOptions.outWidth;
            photoH = bmOptions.outHeight;
            scaleFactor = Math.max(((float) photoW / targetW), ((float) photoH / targetH));
            bmRotate = false;
        }
        System.out.format("rect view params are %d, %d\n", targetH, targetW);
        System.out.format("image params are %d, %d\n", bmOptions.outHeight, bmOptions.outWidth);

        System.out.format("scale factor is %f\n", scaleFactor);
        System.out.format("is image rotated? %b\n", bmRotate);

        // Determine how much to scale down the image
        //int scaleFactor = Math.min(photoH/targetW, photoW/targetH);
        //int scaleFactor = Math.max(Math.round((float)photoH/targetW), Math.round((float)photoW/targetH));
        // Decode the image file into a Bitmap sized to fill the View


        if (bmRotate) {
            bitmapImgRot = RotateBitmap(bitmapImg, 90, 1/scaleFactor);
        } else {
            bitmapImgRot = RotateBitmap(bitmapImg, 0, 1/scaleFactor);
        }
        bitmapImg = bitmapImgRot;
        mImageView.setImageBitmap(bitmapImgRot);
        System.out.format("output image params are %d, %d\n", bitmapImgRot.getHeight(), bitmapImgRot.getWidth());

        switchToSet();
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle, float resizeScale) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        matrix.postScale(resizeScale, resizeScale);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public void switchToSet() {
        setContentView(R.layout.activity_rect);

        final rectView rectview;
        rectview = (rectView) findViewById(R.id.rect_view);

        rectview.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                Log.w("Touch", "Worked");
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        rectview.drawRectangle = true; // Start drawing the rectangle
                        rectview.beginCoordinate.x = event.getX();
                        rectview.beginCoordinate.y = event.getY();
                        rectview.endCoordinate.x = event.getX();
                        rectview.endCoordinate.y = event.getY();
                        rectview.invalidate(); // Tell View that the canvas needs to be redrawn
                        break;
                    case MotionEvent.ACTION_MOVE:
                        rectview.endCoordinate.x = event.getX();
                        rectview.endCoordinate.y = event.getY();
                        rectview.invalidate(); // Tell View that the canvas needs to be redrawn
                        break;
                    case MotionEvent.ACTION_UP:
                        // Do something with the beginCoordinate and endCoordinate, like creating the 'final' object
                        rectview.drawRectangle = true; // Stop drawing the rectangle
                        rectview.invalidate(); // Tell View that the canvas needs to be redrawn
                        if (SET_STATUS == 1) {
                            SET_STATUS = 2;
                        }
                        break;
                }
                return true;
            }
        });

        final TextView bltexthint = (TextView) findViewById(R.id.bltexthint);
        final EditText bltext = (EditText) findViewById(R.id.bl_text);
        final TextView bwtexthint = (TextView) findViewById(R.id.bwtexthint);
        final EditText bwtext = (EditText) findViewById(R.id.bwtext);
        final TextView genderHint = (TextView) findViewById(R.id.genderhint);
        final RadioGroup genderSwitch = (RadioGroup) findViewById(R.id.genderswitch);
        final RelativeLayout wrapperLayout = (RelativeLayout)findViewById(R.id.hintwrapper);
        final EditText ageyear = (EditText) findViewById(R.id.ageyear);
        final EditText agemonth = (EditText) findViewById(R.id.agemonth);
        final EditText ageweek = (EditText) findViewById(R.id.ageweek);


        final Button setButton;
        setButton = (Button) findViewById(R.id.set_button);
        setButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                buttonWait();
                Log.w("SET Touch", "Worked");
                if (SET_STATUS == 0) {

                    babyStartPoint = new PointF(rectview.beginCoordinate.x, rectview.beginCoordinate.y);
                    babyEndPoint = new PointF(rectview.endCoordinate.x, rectview.endCoordinate.y);
                    setButton.setText(R.string.set_scale);
                    rectview.invalidate();
                    rectview.drawRectangle = false;
                    SET_STATUS = 1;
                }
                if (SET_STATUS == 2) {
                    scaleStartPoint = new PointF(rectview.beginCoordinate.x, rectview.beginCoordinate.y);
                    scaleEndPoint = new PointF(rectview.endCoordinate.x, rectview.endCoordinate.y);

                    System.out.format("%f %f %f %f\n", scaleStartPoint.x, scaleStartPoint.y, scaleEndPoint.x, scaleEndPoint.y);
                    float hs = Math.abs(scaleStartPoint.y - scaleEndPoint.y);
                    float ws = Math.abs(scaleStartPoint.x - scaleEndPoint.x);

                    boolean validScale = hs * ws > 100;
                    if (validScale) {

                        float hb = Math.abs(babyStartPoint.y - babyEndPoint.y);
                        float wb = Math.abs(babyStartPoint.x - babyEndPoint.x);

                        bitmapScale = Bitmap.createBitmap(bitmapImg,
                                Math.round(scaleStartPoint.x),
                                Math.round(scaleStartPoint.y),
                                Math.round(ws),
                                Math.round(hs));

                        Mat scaleMat = new Mat();
                        Utils.bitmapToMat(bitmapScale.copy(Bitmap.Config.ARGB_8888, true), scaleMat);

                        double[] ctheta = new double[4];
                        double[] crho = new double[4];
                        float[] boundaries;
                        boundaries = getScaleBoundaries(scaleMat);
                        for (int i = 0; i < 4; i++) {
                            ctheta[i] = (double) boundaries[2 * i + 1];
                            crho[i] = (double) boundaries[2 * i];
                            System.out.format("%f, %f\n", ctheta[i], crho[i]);
                        }

                        double[] xint = new double[4];
                        double[] yint = new double[4];

                        int counter = 0;
                        if (Math.abs(ctheta[0] - ctheta[1]) > Math.PI / 4) {
                            xint[counter] = (Math.sin(ctheta[1]) * crho[0] - Math.sin(ctheta[0]) * crho[1]) / (Math.sin(ctheta[1] - ctheta[0]));
                            yint[counter] = (-Math.cos(ctheta[1]) * crho[0] + Math.cos(ctheta[0]) * crho[1]) / (Math.sin(ctheta[1] - ctheta[0]));
                            counter = counter + 1;
                        }

                        if (Math.abs(ctheta[0] - ctheta[2]) > Math.PI / 4) {
                            xint[counter] = (Math.sin(ctheta[2]) * crho[0] - Math.sin(ctheta[0]) * crho[2]) / (Math.sin(ctheta[2] - ctheta[0]));
                            yint[counter] = (-Math.cos(ctheta[2]) * crho[0] + Math.cos(ctheta[0]) * crho[2]) / (Math.sin(ctheta[2] - ctheta[0]));
                            counter = counter + 1;
                        }
                        if (Math.abs(ctheta[0] - ctheta[3]) > Math.PI / 4) {
                            xint[counter] = (Math.sin(ctheta[3]) * crho[0] - Math.sin(ctheta[0]) * crho[3]) / (Math.sin(ctheta[3] - ctheta[0]));
                            yint[counter] = (-Math.cos(ctheta[3]) * crho[0] + Math.cos(ctheta[0]) * crho[3]) / (Math.sin(ctheta[3] - ctheta[0]));
                            counter = counter + 1;
                        }
                        if (Math.abs(ctheta[2] - ctheta[1]) > Math.PI / 4) {
                            xint[counter] = (Math.sin(ctheta[1]) * crho[2] - Math.sin(ctheta[2]) * crho[1]) / (Math.sin(ctheta[1] - ctheta[2]));
                            yint[counter] = (-Math.cos(ctheta[1]) * crho[2] + Math.cos(ctheta[2]) * crho[1]) / (Math.sin(ctheta[1] - ctheta[2]));
                            counter = counter + 1;
                        }
                        if (Math.abs(ctheta[3] - ctheta[1]) > Math.PI / 4) {
                            xint[counter] = (Math.sin(ctheta[1]) * crho[3] - Math.sin(ctheta[3]) * crho[1]) / (Math.sin(ctheta[1] - ctheta[3]));
                            yint[counter] = (-Math.cos(ctheta[1]) * crho[3] + Math.cos(ctheta[3]) * crho[1]) / (Math.sin(ctheta[1] - ctheta[3]));
                            counter = counter + 1;
                        }
                        if (Math.abs(ctheta[3] - ctheta[2]) > Math.PI / 4) {
                            xint[counter] = (Math.sin(ctheta[2]) * crho[3] - Math.sin(ctheta[3]) * crho[2]) / (Math.sin(ctheta[2] - ctheta[3]));
                            yint[counter] = (-Math.cos(ctheta[2]) * crho[3] + Math.cos(ctheta[3]) * crho[2]) / (Math.sin(ctheta[2] - ctheta[3]));
                        }

                        System.out.format("%d INTERSECTIONS\n", counter + 1);
                        for (int i = 0; i < 4; i++) {
                            System.out.format("%f, %f\n", xint[i], yint[i]);
                        }
                        double a1 = areaTriangle(xint[1], yint[1], xint[2], yint[2], xint[3], yint[3]);
                        double a2 = areaTriangle(xint[0], yint[0], xint[2], yint[2], xint[3], yint[3]);
                        double a3 = areaTriangle(xint[1], yint[1], xint[0], yint[0], xint[3], yint[3]);
                        double a4 = areaTriangle(xint[1], yint[1], xint[2], yint[2], xint[0], yint[0]);
                        double area1 = (a1 + a2 + a3 + a4) / 2;

                        float relh = ((float) Math.sqrt(area1 * SCALE_ASPECT_RATIO)) / ((float) 1.41);
                        //float relh =  ((float) Math.sqrt(ws*hs * SCALE_ASPECT_RATIO));


                        BABY_LENGTH = Math.round((Math.max(wb, hb) / relh) * SCALE_LENGTH);


                        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        p.addRule(RelativeLayout.BELOW, R.id.hintwrapper);
                        p.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        rectview.setVisibility(View.GONE);
                        bltext.setText(String.format(Locale.getDefault(), "%d", BABY_LENGTH));
                        setButton.setText(R.string.calc_chart);
                        setButton.setLayoutParams(p);
                        /*
                        genderHint.setVisibility(View.VISIBLE);
                        genderSwitch.setVisibility(View.VISIBLE);
                        bwtexthint.setVisibility(View.VISIBLE);
                        bwtext.setVisibility(View.VISIBLE);
                        bltexthint.setVisibility(View.VISIBLE);
                        bltext.setVisibility(View.VISIBLE);
                        */
                        wrapperLayout.setVisibility(View.VISIBLE);

                        SET_STATUS = 3;
                    } else {
                        SET_STATUS = 1;
                    }
                } else if (SET_STATUS == 3) {
                    SET_STATUS = 4;
                } else if (SET_STATUS == 4) {
                    try {
                        BABY_WEIGHT = Double.parseDouble(bwtext.getText().toString());
                        BABY_LENGTH = Integer.parseInt(bltext.getText().toString());
                        AGE_YEARS = Integer.parseInt(ageyear.getText().toString());
                        AGE_MONTHS = Integer.parseInt(agemonth.getText().toString());
                        AGE_WEEKS = Integer.parseInt(ageweek.getText().toString());

                        double totalDays = (double) AGE_YEARS * 365.0 + (double) AGE_MONTHS * 30.4 + (double) AGE_WEEKS * 7.0;

                        GraphView weightGraph = (GraphView) findViewById(R.id.graph_weight);
                        GraphView heightGraph = (GraphView) findViewById(R.id.graph_height);

                        InputStream wfaBoys = getResources().openRawResource(R.raw.wfaboys);
                        InputStream wfaGirls = getResources().openRawResource(R.raw.wfagirls);

                        CSVFile csvFile = new CSVFile(wfaBoys);
                        List<String[]> scoreList = csvFile.read();
                        int wfa_boys_days[] = new int[scoreList.size()];
                        double wfa_boys_SD0[] = new double[scoreList.size()];
                        double wfa_boys_SD1[] = new double[scoreList.size()];
                        double wfa_boys_SD2[] = new double[scoreList.size()];
                        double wfa_boys_SD3[] = new double[scoreList.size()];
                        double wfa_boys_SD4[] = new double[scoreList.size()];
                        double wfa_boys_SD1neg[] = new double[scoreList.size()];
                        double wfa_boys_SD2neg[] = new double[scoreList.size()];
                        double wfa_boys_SD3neg[] = new double[scoreList.size()];
                        double wfa_boys_SD4neg[] = new double[scoreList.size()];

                        Iterator<String[]> it = scoreList.iterator();
                        int j = 0;
                        while (it.hasNext()) {
                            String[] row = it.next();
                            wfa_boys_days[j] = (int) (Double.parseDouble(row[0]));
                            wfa_boys_SD0[j] = (Double.parseDouble(row[1]));
                            wfa_boys_SD1[j] = (Double.parseDouble(row[2]));
                            wfa_boys_SD2[j] = (Double.parseDouble(row[3]));
                            wfa_boys_SD3[j] = (Double.parseDouble(row[4]));
                            wfa_boys_SD4[j] = (Double.parseDouble(row[5]));
                            wfa_boys_SD1neg[j] = (Double.parseDouble(row[6]));
                            wfa_boys_SD2neg[j] = (Double.parseDouble(row[7]));
                            wfa_boys_SD3neg[j] = (Double.parseDouble(row[8]));
                            wfa_boys_SD4neg[j] = (Double.parseDouble(row[9]));
                            j = j + 1;
                        }
                        int WFA_BOYS_SIZE = j;
                        WFA_BOYS_SIZE = 100;

                        weightGraph.getViewport().setYAxisBoundsManual(true);
                        weightGraph.getViewport().setMinY(2);
                        weightGraph.getViewport().setMaxY(10);

                        weightGraph.getViewport().setXAxisBoundsManual(true);
                        weightGraph.getViewport().setMinX(0);
                        weightGraph.getViewport().setMaxX(WFA_BOYS_SIZE);

                        weightGraph.getViewport().setScalable(true);
                        weightGraph.getViewport().setScalableY(true);

                        DataPoint[] points = new DataPoint[WFA_BOYS_SIZE];

                        for (int i = 0; i < WFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(wfa_boys_days[i], wfa_boys_SD0[i]);
                        }
                        LineGraphSeries<DataPoint> SD0 = new LineGraphSeries<>(points);
                        SD0.setColor(Color.GREEN);
                        weightGraph.addSeries(SD0);

                        for (int i = 0; i < WFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(wfa_boys_days[i], wfa_boys_SD1[i]);
                        }
                        LineGraphSeries<DataPoint> SD1 = new LineGraphSeries<>(points);
                        SD1.setColor(Color.RED);
                        weightGraph.addSeries(SD1);

                        for (int i = 0; i < WFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(wfa_boys_days[i], wfa_boys_SD2[i]);
                        }
                        LineGraphSeries<DataPoint> SD2 = new LineGraphSeries<>(points);
                        SD2.setColor(Color.BLACK);
                        weightGraph.addSeries(SD2);

                        for (int i = 0; i < WFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(wfa_boys_days[i], wfa_boys_SD1neg[i]);
                        }
                        LineGraphSeries<DataPoint> SD1neg = new LineGraphSeries<>(points);
                        SD1neg.setColor(Color.RED);
                        weightGraph.addSeries(SD1neg);

                        for (int i = 0; i < WFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(wfa_boys_days[i], wfa_boys_SD2neg[i]);
                        }
                        LineGraphSeries<DataPoint> SD2neg = new LineGraphSeries<>(points);
                        SD2neg.setColor(Color.BLACK);
                        weightGraph.addSeries(SD2neg);

                        DataPoint[] wfa_baby_data = new DataPoint[1];
                        wfa_baby_data[0] = new DataPoint(totalDays, BABY_WEIGHT);
                        System.out.format("weight is %f, total days are %f", BABY_WEIGHT, totalDays);
                        LineGraphSeries<DataPoint> wfa_baby = new LineGraphSeries<>(wfa_baby_data);
                        wfa_baby.setColor(Color.BLUE);
                        wfa_baby.setDrawDataPoints(true);
                        wfa_baby.setDataPointsRadius(20);
                        weightGraph.addSeries(wfa_baby);

                        //graph.getViewport().setBackgroundColor(Color.WHITE);
                        weightGraph.setVisibility(View.VISIBLE);

                        //Height plot
                        InputStream lfaBoys = getResources().openRawResource(R.raw.lfaboys);
                        InputStream lfaGirls = getResources().openRawResource(R.raw.lfagirls);

                        CSVFile csvFile2 = new CSVFile(lfaBoys);
                        List<String[]> scoreList2 = csvFile2.read();
                        int lfa_boys_days[] = new int[scoreList2.size()];
                        double lfa_boys_SD0[] = new double[scoreList2.size()];
                        double lfa_boys_SD1[] = new double[scoreList2.size()];
                        double lfa_boys_SD2[] = new double[scoreList2.size()];
                        double lfa_boys_SD3[] = new double[scoreList2.size()];
                        double lfa_boys_SD4[] = new double[scoreList2.size()];
                        double lfa_boys_SD1neg[] = new double[scoreList2.size()];
                        double lfa_boys_SD2neg[] = new double[scoreList2.size()];
                        double lfa_boys_SD3neg[] = new double[scoreList2.size()];
                        double lfa_boys_SD4neg[] = new double[scoreList2.size()];

                        Iterator<String[]> it2 = scoreList2.iterator();
                        j = 0;
                        while (it2.hasNext()) {
                            String[] row = it2.next();
                            lfa_boys_days[j] = (int) (Double.parseDouble(row[0]));
                            lfa_boys_SD0[j] = (Double.parseDouble(row[1]));
                            lfa_boys_SD1[j] = (Double.parseDouble(row[2]));
                            lfa_boys_SD2[j] = (Double.parseDouble(row[3]));
                            lfa_boys_SD3[j] = (Double.parseDouble(row[4]));
                            lfa_boys_SD4[j] = (Double.parseDouble(row[5]));
                            lfa_boys_SD1neg[j] = (Double.parseDouble(row[6]));
                            lfa_boys_SD2neg[j] = (Double.parseDouble(row[7]));
                            lfa_boys_SD3neg[j] = (Double.parseDouble(row[8]));
                            lfa_boys_SD4neg[j] = (Double.parseDouble(row[9]));
                            j = j + 1;
                        }
                        int LFA_BOYS_SIZE = j;
                        LFA_BOYS_SIZE = 100;

                        heightGraph.getViewport().setYAxisBoundsManual(true);
                        heightGraph.getViewport().setMinY(40);
                        heightGraph.getViewport().setMaxY(80);

                        heightGraph.getViewport().setXAxisBoundsManual(true);
                        heightGraph.getViewport().setMinX(0);
                        heightGraph.getViewport().setMaxX(LFA_BOYS_SIZE);

                        heightGraph.getViewport().setScalable(true);
                        heightGraph.getViewport().setScalableY(true);

                        points = new DataPoint[LFA_BOYS_SIZE];

                        for (int i = 0; i < LFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(lfa_boys_days[i], lfa_boys_SD0[i]);
                        }
                        SD0 = new LineGraphSeries<>(points);
                        SD0.setColor(Color.GREEN);
                        heightGraph.addSeries(SD0);

                        for (int i = 0; i < LFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(lfa_boys_days[i], lfa_boys_SD1[i]);
                        }
                        SD1 = new LineGraphSeries<>(points);
                        SD1.setColor(Color.RED);
                        heightGraph.addSeries(SD1);

                        for (int i = 0; i < LFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(lfa_boys_days[i], lfa_boys_SD2[i]);
                        }
                        SD2 = new LineGraphSeries<>(points);
                        SD2.setColor(Color.BLACK);
                        heightGraph.addSeries(SD2);

                        for (int i = 0; i < LFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(lfa_boys_days[i], lfa_boys_SD1neg[i]);
                        }
                        SD1neg = new LineGraphSeries<>(points);
                        SD1neg.setColor(Color.RED);
                        heightGraph.addSeries(SD1neg);

                        for (int i = 0; i < LFA_BOYS_SIZE; i++) {
                            points[i] = new DataPoint(lfa_boys_days[i], lfa_boys_SD2neg[i]);
                        }
                        SD2neg = new LineGraphSeries<>(points);
                        SD2neg.setColor(Color.BLACK);
                        heightGraph.addSeries(SD2neg);

                        DataPoint[] lfa_baby_data = new DataPoint[1];
                        lfa_baby_data[0] = new DataPoint(totalDays, BABY_LENGTH/10);
                        LineGraphSeries<DataPoint> lfa_baby = new LineGraphSeries<>(lfa_baby_data);
                        lfa_baby.setColor(Color.BLUE);
                        lfa_baby.setDrawDataPoints(true);
                        lfa_baby.setDataPointsRadius(20);
                        heightGraph.addSeries(lfa_baby);

                        //graph.getViewport().setBackgroundColor(Color.WHITE);
                        heightGraph.setVisibility(View.VISIBLE);

                        RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        p.addRule(RelativeLayout.BELOW, R.id.graph_height);
                        p.addRule(RelativeLayout.CENTER_HORIZONTAL);

                        setButton.setLayoutParams(p);
                        setButton.setText(R.string.graph);

                    /*
                    bwtexthint.setVisibility(View.GONE);
                    bwtext.setVisibility(View.GONE);
                    genderHint.setVisibility(View.GONE);
                    genderSwitch.setVisibility(View.GONE);
                    bltexthint.setVisibility(View.GONE);
                    bltext.setVisibility(View.GONE);
                    */
                        wrapperLayout.setVisibility(View.GONE);
                    }
                    catch (NumberFormatException e){
                        SET_STATUS = 3;
                    }
                }
                return true;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            setPicCamera();
            TAKEN_PHOTO = true;
        }
        if (requestCode == REQUEST_GET_PHOTO && resultCode == RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            onSelectFromGalleryResult(data);
            TAKEN_PHOTO = true;
        }
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {
        if (data != null) {
            mPhotoUri = data.getData();
            mCurrentPhotoPath = getPath(getApplicationContext(), mPhotoUri);
        }
        setPicCamera();
    }

    @TargetApi(19)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static double areaTriangle(double x1, double y1, double x2, double y2, double x3, double y3) {
        return Math.abs(x1 * y2 - x1 * y3 + y1 * x3 - y1 * x2 + x2 * y3 - y2 * x3);
    }

    private static float[] getScaleBoundaries(Mat image) {
        // get edges
        Mat edges = new Mat();
        Imgproc.Canny(image, edges, 20, 150, 3, true);

        // get lines
        Mat lines = new Mat();
        Imgproc.HoughLines(edges, lines, 1, 0.001, 50);
        lines.convertTo(lines, CvType.CV_32FC2);
        float[] theta = new float[lines.cols() / 2];
        float[] rho = new float[lines.cols() / 2];
        float[] linedata = new float[(int) (lines.total() * lines.channels())];
        double pi = Math.PI;

        lines.get(0, 0, linedata);

        for (int i = 0; i < (lines.cols() / 2); i++) {
            theta[i] = linedata[2 * i + 1];
            rho[i] = linedata[2 * i];
        }

        int t1 = 0;
        int t2 = 0;
        float theta1 = 0;
        float theta2 = 0;
        float rho1 = 0;
        float rho2 = 0;

        int idx = 0;
        float[] ctheta = new float[4];
        float[] crho = new float[4];

        for (int i = 0; i < theta.length; i++) {
            theta[i] = (theta[i] <= 0.75 * (float) Math.PI) ? theta[i] : -theta[i] + (float) Math.PI;
        }

        for (int i = 0; i < theta.length; i++) {

            if (t1 == 0) {
                theta1 = theta[i];
                rho1 = rho[i];
                t1 = 1;

                ctheta[idx] = theta[i];
                crho[idx] = rho[i];
                idx = idx + 1;
            } else if (t1 == 1 && t2 == 0) {
                if (Math.abs(theta1 - theta[i]) > 4 * pi / 10) {
                    t2 = 1;
                    theta2 = theta[i];
                    rho2 = rho[i];

                    ctheta[idx] = theta[i];
                    crho[idx] = rho[i];
                    idx = idx + 1;
                } else if (Math.abs(rho[i] - rho1) > 20) {
                    t1 = 2;

                    ctheta[idx] = theta[i];
                    crho[idx] = rho[i];
                    idx = idx + 1;
                }
            } else if (t1 == 2 && t2 == 0) {
                if (Math.abs(theta[i] - theta1) > 4 * pi / 10) {
                    t2 = 1;
                    theta2 = theta[i];
                    rho2 = rho[i];

                    ctheta[idx] = theta[i];
                    crho[idx] = rho[i];
                    idx = idx + 1;
                }
            } else if (t1 == 1 && t2 == 1) {
                if (Math.abs(theta1 - theta[i]) > 4 * pi / 10) {
                    if (Math.abs(rho2 - rho[i]) > 20) {
                        t2 = 2;

                        ctheta[idx] = theta[i];
                        crho[idx] = rho[i];
                        idx = idx + 1;
                    }
                } else {
                    if (Math.abs(rho1 - rho[i]) > 20) {
                        t1 = 2;

                        ctheta[idx] = theta[i];
                        crho[idx] = rho[i];
                        idx = idx + 1;
                    }
                }
            } else if (t1 == 1 && t2 == 2) {
                if (Math.abs(theta1 - theta[i]) < pi / 10) {
                    if (Math.abs(rho1 - rho[i]) > 20) {
                        t1 = 2;

                        ctheta[idx] = theta[i];
                        crho[idx] = rho[i];
                        idx = idx + 1;
                    }
                }
            } else if (t1 == 2 && t2 == 1) {
                if (Math.abs(theta1 - theta[i]) > 4 * pi / 10) {
                    if (Math.abs(rho2 - rho[i]) > 20) {
                        t2 = 2;

                        ctheta[idx] = theta[i];
                        crho[idx] = rho[i];
                        idx = idx + 1;
                    }
                }
            } else {
                break;
            }
        }

        float[] boundaries = new float[8];
        for (int i = 0; i < 4; i++) {
            boundaries[2 * i + 1] = ctheta[i];
            boundaries[2 * i] = crho[i];
        }
        return boundaries;
    }
    public void buttonWait(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        // wait for sometime
        new CountDownTimer(200, 100){
            public void onTick(long millisUntilFinished) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
            public void onFinish() {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public static Bitmap getBitmapImg() {
        return bitmapImgRot;
    }
}