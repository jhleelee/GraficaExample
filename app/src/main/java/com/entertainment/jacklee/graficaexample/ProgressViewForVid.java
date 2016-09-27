package com.entertainment.jacklee.graficaexample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;


/**
 * Created by Jacklee on 16. 6. 27..
 */
public class ProgressViewForVid extends View {

    String TAG = "ProgressViewForVid";


    private Paint barBodyPaint, flashPaint, minRecordPaint, breakPaint;
    private float flashHeight = 20f, breakHeight = 5f;
    private LinkedList<Long> linkedList = new LinkedList<Long>();
    private float iHeightPixelsPerMilliSec = 0l;
    private float mFullRecordTime = 20 * 1000;


    public ProgressViewForVid(Context context) {
        super(context);
        init(context);
    }

    public ProgressViewForVid(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init(paramContext);
    }

    public ProgressViewForVid(Context paramContext, AttributeSet paramAttributeSet,
                              int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramContext);
    }


    public void setTotalTime(float fullRecordTime) {
        mFullRecordTime = fullRecordTime;
    }

    private void init(Context context) {

        barBodyPaint = new Paint();
        flashPaint = new Paint();
        minRecordPaint = new Paint();
        breakPaint = new Paint();

        setBackgroundColor(Color.parseColor("#616161"));

        barBodyPaint.setStyle(Paint.Style.FILL);
        barBodyPaint.setColor(Color.parseColor("#EF6C00")); // Orange800

        flashPaint.setStyle(Paint.Style.FILL);
        flashPaint.setColor(Color.parseColor("#ffcc42"));

        minRecordPaint.setStyle(Paint.Style.FILL);
        minRecordPaint.setColor(Color.parseColor("#12a899"));

        breakPaint.setStyle(Paint.Style.FILL);
        breakPaint.setColor(Color.parseColor("#FFFFFF"));

        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        iHeightPixelsPerMilliSec = dm.heightPixels / mFullRecordTime;
        perSecProgress = iHeightPixelsPerMilliSec;
    }


    public enum State {
        START(0x1), PAUSE(0x2), PLAY(0x3);

        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return PAUSE;
        }

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        int getIntValue() {
            return mIntValue;
        }
    }


    private volatile State currentState = State.PAUSE;
    private boolean isVisible = true;
    private float fBarBodyHeight = 0;



    private float perProgress = 0;
    private float perSecProgress = 0;
    private long initTime;
    private long lFlashIntervalTime = 0;

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long curTime = System.currentTimeMillis();
        fBarBodyHeight = 0;
        int numOfBreaks =0;
        if (!linkedList.isEmpty()) {
            float lTimeItemBefore = 0;
            Iterator<Long> iterator = linkedList.iterator();


            while (iterator.hasNext()) {
                long lTimeItem = iterator.next();
                /*
                Example :
                2s, 5s, (D0.5s), Fully100
                 */

                //(0) 2s
                //(1) 5.2s
                fBarBodyHeight += (lTimeItem - lTimeItemBefore) * iHeightPixelsPerMilliSec;


                //(0) 98, 100
                //(1) 100-5.2, 100-2.2

                 canvas.drawRect(
                        0, // X1 of (X1, Y1)
                        getMeasuredHeight() - fBarBodyHeight, // Y1 of (X1, Y1)
                        getMeasuredWidth(), //X2 of (X2, Y2)
                        getMeasuredHeight() - lTimeItemBefore*iHeightPixelsPerMilliSec - breakHeight*numOfBreaks, // Y2 of (X2, Y2)
                        barBodyPaint
                );

                //White Dividers


                canvas.drawRect(
                        0, // X1 of (X1, Y1)
                        getMeasuredHeight() - fBarBodyHeight - breakHeight,
                        getMeasuredWidth(),
                        getMeasuredHeight() - fBarBodyHeight,
                        breakPaint
                );

                //(0) 2.2s
                fBarBodyHeight += breakHeight;





                lTimeItemBefore = lTimeItem;
                numOfBreaks++;
            }

            //(0) 97.5, 97
            if (linkedList.getLast() <= 3000)
                canvas.drawRect(
                        0,
                        getMeasuredHeight() - iHeightPixelsPerMilliSec * 3000 + breakHeight,
                        getMeasuredWidth(),
                        getMeasuredHeight() - iHeightPixelsPerMilliSec * 3000,
                        minRecordPaint
                );
        } else {
            canvas.drawRect(
                    0,
                    getMeasuredHeight() - iHeightPixelsPerMilliSec * 3000 + breakHeight,
                    getMeasuredWidth(),
                    getMeasuredHeight() - iHeightPixelsPerMilliSec * 3000,
                    minRecordPaint
            );
        }


        if (currentState == State.START) {
            perProgress += perSecProgress * (curTime - initTime);

            if (fBarBodyHeight + perProgress <= getMeasuredHeight()) {

                //(0) 97.8, 98
                canvas.drawRect(
                        0,
                        getMeasuredHeight() - fBarBodyHeight - perProgress,
                        getMeasuredWidth(),
                        getMeasuredHeight() - fBarBodyHeight,
                        barBodyPaint);
            } else {
                canvas.drawRect(
                        0,
                       getMeasuredHeight() - fBarBodyHeight,
                        getMeasuredWidth(),
                        getMeasuredHeight(),
                        barBodyPaint);
            }
        }

        if (lFlashIntervalTime == 0 || curTime - lFlashIntervalTime >= 500) {
            isVisible = !isVisible;
            lFlashIntervalTime = System.currentTimeMillis();
        }

        if (isVisible) {
            if (currentState == State.START) {
                canvas.drawRect(
                        0,
                        getMeasuredHeight() - fBarBodyHeight - flashHeight - perProgress,
                        getMeasuredWidth(),
                        getMeasuredHeight() - fBarBodyHeight - perProgress,
                        flashPaint
                );
            } else {
                canvas.drawRect(
                        0,
                        getMeasuredHeight() - fBarBodyHeight - flashHeight,
                        getMeasuredWidth(),
                        getMeasuredHeight() - fBarBodyHeight,
                        flashPaint
                );
            }
        }


        initTime = System.currentTimeMillis();

        /**
         * Invalidate the whole view. If the view is visible,
         * {@link #onDraw(Canvas)} will be called at some point in
         * the future.
         * <p>
         * This must be called from a UI thread. To call from a non-UI thread, call
         * {@link #postInvalidate()}.
         */
        invalidate();
    }

    public void setCurrentState(State state) {
        currentState = state;
        if (state == State.PAUSE)
            perProgress = perSecProgress;
    }

    public void putProgressList(long time) {
        linkedList.add(time);
    }

    public void removeLastItemOfLinkedList() {
        if (linkedList.size() > 0) {
            linkedList.remove(linkedList.size() - 1);
        }
    }


}