package com.nhj.video;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public  class SurfaceViewL extends SurfaceView implements SurfaceHolder.Callback,Runnable{

        private Paint paint ;
        SurfaceHolder surfaceHolder ;
        private volatile boolean isDrawing ;
        private int offset = 10;
        private MotionEvent event ;
        private Path mPath ;
        public SurfaceViewL(Context context) {
            super(context);
            init() ;
        }

        public SurfaceViewL(Context context, AttributeSet attrs) {
            super(context, attrs);
            init() ;
        }

        public SurfaceViewL(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init() ;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

            isDrawing = true ;
            Executors.newCachedThreadPool().submit(this) ;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            isDrawing= false ;
        }



        private float lastX ;
        private float lastY ;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.event = event ;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX() ;
                lastY = event.getY() ;
                mPath.moveTo(lastX,lastY);
                break;

            case MotionEvent.ACTION_UP:

                break;

            case MotionEvent.ACTION_MOVE:
                mPath.rLineTo(event.getX()-lastX,event.getY()-lastY);
                lastX = event.getX() ;
                lastY = event.getY() ;
                break;
        }
        return true;
    }

    @Override
        public void run() {
            while (isDrawing){
                // 获取 canvans 绘图
                Canvas canvas = null;
                try {
                    offset+=5 ;
                    TimeUnit.MILLISECONDS.sleep(100);
                    canvas = surfaceHolder.lockCanvas() ;
//                    canvas.drawText("聂慧俊 ",100+offset,100+offset,paint);
//                    if(event!=null){
//                        canvas.drawPoint(event.getX(),event.getY(),paint);
//                    }
                    canvas.drawPath(mPath,paint);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if(canvas!=null){
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

            }
        }

        public void init(){
            surfaceHolder = getHolder() ;
            surfaceHolder.addCallback(this);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setKeepScreenOn(true);
            paint = new Paint() ;
            paint.setAntiAlias(true);
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setTextSize(18);
            paint.setStrokeWidth(10f);
            mPath = new Path() ;
        }
    }