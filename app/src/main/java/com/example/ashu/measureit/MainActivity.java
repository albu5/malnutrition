package com.example.ashu.measureit;

import android.app.Activity;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import android.graphics.PointF;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.RunnableFuture;

public class MainActivity extends AppCompatActivity {
    Button cameraButton;
    ImageView mImageView;
    String mCurrentPhotoPath;
    static Bitmap bitmapImg;

    PointF babyStartPoint;
    PointF babyEndPoint;
    PointF scaleStartPoint;
    PointF scaleEndPoint;

    static final float SCALE_ASPECT_RATIO = (float)4/3;
    static final float SCALE_LENGTH = (float)6;
    static float BABY_LENGTH = (float)6;

    int SET_STATUS = 0;

    static final int REQUEST_TAKE_PHOTO = 1;
    boolean TAKEN_PHOTO = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        cameraButton = (Button)findViewById(R.id.button);
        mImageView = (ImageView)findViewById(R.id.image_view);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
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

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        //int scaleFactor = Math.min(photoH/targetW, photoW/targetH);
        int scaleFactor = Math.max(Math.round((float)photoH/targetW), Math.round((float)photoW/targetH));
        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        bitmapImg = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmapImg);

        switchToSet();
    }

    public void switchToSet(){
        setContentView(R.layout.activity_rect);

        final rectView rectview;
        rectview = (rectView)findViewById(R.id.rect_view);
        rectview.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                Log.w("Touch", "Worked");
                switch(event.getAction()) {
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
                        if (SET_STATUS == 1){
                            SET_STATUS = 2;
                        }
                        break;
                }
                return true;
            }
        });

        final TextView bltexthint = (TextView)findViewById(R.id.bltexthint);
        final TextView bltext = (TextView)findViewById(R.id.bl_text);
        final TextView bwtexthint = (TextView)findViewById(R.id.bwtexthint);
        final EditText bwtext = (EditText)findViewById(R.id.bwtext);

        final Button setButton;
        setButton = (Button)findViewById(R.id.set_button);
        setButton.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event) {
                Log.w("SET Touch", "Worked");
                if (SET_STATUS == 0) {
                    babyStartPoint = new PointF(rectview.beginCoordinate.x, rectview.beginCoordinate.y);
                    babyEndPoint = new PointF(rectview.endCoordinate.x, rectview.endCoordinate.y);
                    setButton.setText("Set Scale");
                    rectview.invalidate();
                    rectview.drawRectangle = false;
                    SET_STATUS = 1;
                } else {
                    if (SET_STATUS == 2) {
                        scaleStartPoint = new PointF(rectview.beginCoordinate.x, rectview.beginCoordinate.y);
                        scaleEndPoint = new PointF(rectview.endCoordinate.x, rectview.endCoordinate.y);

                        float hs = Math.abs(scaleStartPoint.y - scaleEndPoint.y);
                        float ws = Math.abs(scaleStartPoint.x - scaleEndPoint.x);

                        boolean validScale = hs*ws>100;
                        if (validScale) {
                            float hb = Math.abs(babyStartPoint.y - babyEndPoint.y);
                            float wb = Math.abs(babyStartPoint.x - babyEndPoint.x);
                            float relh = SCALE_ASPECT_RATIO * (float) Math.sqrt((double) hs * ws / SCALE_ASPECT_RATIO);
                            BABY_LENGTH = Math.round((hb/relh)* SCALE_LENGTH);

                            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            p.addRule(RelativeLayout.BELOW, R.id.bwtext);

                            rectview.setVisibility(View.GONE);
                            bltexthint.setVisibility(View.VISIBLE);
                            bltext.setVisibility(View.VISIBLE);
                            bltext.setText(String.format("%f cm", BABY_LENGTH));
                            bwtexthint.setVisibility(View.VISIBLE);
                            bwtext.setVisibility(View.VISIBLE);
                            setButton.setText("Calcualte Chart");
                            setButton.setLayoutParams(p);
                        }
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
            setPic();
            TAKEN_PHOTO = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static Bitmap getBitmapImg(){
        return bitmapImg;
    }
}