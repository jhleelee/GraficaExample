package com.entertainment.jacklee.graficaexample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Jacklee on 16. 6. 27..
 */
public class ProgressView extends View
{

    String TAG = "ProgressView";


    private Paint barBodyPaint, flashPaint, minRecordPaint,breakPaint;
    private float flashWidth = 40f, breakWidth = 5f;
    private LinkedList<Long> linkedList = new LinkedList<Long>();
    private float iWidthPixelsPerMilliSec = 0l;
    private float mFullRecordTime = 20*1000;


    public ProgressView(Context context) {
        super(context);
        init(context);
    }

    public ProgressView(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        init(paramContext);
    }

    public ProgressView(Context paramContext, AttributeSet paramAttributeSet,
                        int paramInt) {
        super(paramContext, paramAttributeSet, paramInt);
        init(paramContext);
    }



    public void setTotalTime(float fullRecordTime){
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
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        iWidthPixelsPerMilliSec = dm.widthPixels/ mFullRecordTime;
        perSecProgress = iWidthPixelsPerMilliSec;
    }


    public enum State {
        START(0x1),PAUSE(0x2),PLAY(0x3);

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
    private float fBarBodyWidth = 0;
    private float perProgress = 0;
    private float perSecProgress = 0;
    private long initTime;
    private long lFlashIntervalTime = 0;

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long curTime = System.currentTimeMillis();
        fBarBodyWidth = 0;

        if(!linkedList.isEmpty()){
            float lTimeItemBefore = 0;
            Iterator<Long> iterator = linkedList.iterator();
            while(iterator.hasNext()){
                long lTimeItem = iterator.next();
                float fX1FromX0 = fBarBodyWidth;
                fBarBodyWidth += (lTimeItem-lTimeItemBefore)* iWidthPixelsPerMilliSec;
                canvas.drawRect(
                        fX1FromX0, // (X1, Y1)
                        0,
                        fBarBodyWidth, //width (distance from X1 to X2)
                        getMeasuredHeight(), // height (distance from Y1 to Y2)
                        barBodyPaint
                );
                canvas.drawRect(
                        fBarBodyWidth,
                        0,
                        fBarBodyWidth + breakWidth,
                        getMeasuredHeight(),
                        breakPaint
                );
                fBarBodyWidth += breakWidth;
                lTimeItemBefore = lTimeItem;
            }

            if(linkedList.getLast() <= 3000)
                canvas.drawRect(
                        iWidthPixelsPerMilliSec *3000,
                        0,
                        iWidthPixelsPerMilliSec *3000+ breakWidth,
                        getMeasuredHeight(),
                        minRecordPaint
                );
        }else
            canvas.drawRect(iWidthPixelsPerMilliSec *3000, 0, iWidthPixelsPerMilliSec *3000+ breakWidth,getMeasuredHeight(), minRecordPaint);

        if(currentState == State.START){
            perProgress += perSecProgress*(curTime - initTime );

            if(fBarBodyWidth + perProgress <= getMeasuredWidth()) {
                canvas.drawRect(fBarBodyWidth, 0, fBarBodyWidth + perProgress, getMeasuredHeight(), barBodyPaint);
            }else {
                canvas.drawRect(fBarBodyWidth, 0, getMeasuredWidth(), getMeasuredHeight(), barBodyPaint);
            }
        }

        if(lFlashIntervalTime ==0 || curTime - lFlashIntervalTime >= 500){
            isVisible = !isVisible;
            lFlashIntervalTime = System.currentTimeMillis();
        }

        if(isVisible) {
            if (currentState == State.START) {
                canvas.drawRect(
                        fBarBodyWidth + perProgress,
                        0,
                        fBarBodyWidth + flashWidth + perProgress,
                        getMeasuredHeight(),
                        flashPaint
                );
            } else {
                canvas.drawRect(
                        fBarBodyWidth,
                        0,
                        fBarBodyWidth + flashWidth,
                        getMeasuredHeight(),
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

    public void setCurrentState(State state){
        currentState = state;
        if(state == State.PAUSE)
            perProgress = perSecProgress;
    }

    public void putProgressList(long time) {
        linkedList.add(time);
    }

    public void removeLastItemOfLinkedList() {
        if (linkedList.size()>0) {
            linkedList.remove(linkedList.size() - 1);
        }
    }


}