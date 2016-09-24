package com.example.ashu.measureit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class rectView extends ImageView {
    boolean drawRectangle = false;
    public PointF beginCoordinate;
    public PointF endCoordinate;
    public Paint paint;
    public rectView(Context context) {
        super(context);
        init();
        // TODO Auto-generated constructor stub
    }

    public rectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        // TODO Auto-generated constructor stub
    }


    public void init(){
        beginCoordinate = new PointF(0,0);
        endCoordinate = new PointF(0,0);

        paint = new Paint();
        paint.setDither(true);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5);
        paint.setFilterBitmap(true);

        Log.w("Draw", "Worked");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(drawRectangle) {
            // Note: I assume you have the paint object defined in your class
            canvas.drawBitmap(MainActivity.getBitmapImg(), 0, 0, null);

            Log.w("Draw", "Worked");
            canvas.drawRect(beginCoordinate.x, beginCoordinate.y, endCoordinate.x, endCoordinate.y, paint);
        }
    }

}
