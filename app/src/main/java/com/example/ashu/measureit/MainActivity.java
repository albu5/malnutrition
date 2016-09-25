package com.example.ashu.measureit;

import android.annotation.TargetApi;
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
import android.content.Context;
import android.os.Build;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.content.ContentUris;


public class MainActivity extends AppCompatActivity {
    Button cameraButton;
    Button galleryButton;
    ImageView mImageView;
    String mCurrentPhotoPath;
    Uri mPhotoUri;
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
    static final int REQUEST_GET_PHOTO = 2;

    boolean TAKEN_PHOTO = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        cameraButton = (Button)findViewById(R.id.buttonCamera);
        galleryButton = (Button)findViewById(R.id.buttonGallery);
        mImageView = (ImageView)findViewById(R.id.image_view);

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

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"),REQUEST_GET_PHOTO);
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

    private void setPicCamera() {
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
                final String[] selectionArgs = new String[] {
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
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static Bitmap getBitmapImg(){
        return bitmapImg;
    }
}