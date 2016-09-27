package com.entertainment.jacklee.graficaexample;

import android.util.Log;

/**
 * Created by Jacklee on 16. 6. 27..
 */
public class Bar {

    String TAG = "Bar";

    int aAudioShortDx;
    int bAudioShortDx;


    int aVideoDxOfFrame;
    int bVideoDxOfFrame;

    int aVideoDxOfByteArrayTotal;

    int bVideoDxOfByteArrayTotal;


    long aTimeMill;
    long bTimeMill;

    public long getbTimeMill() {
        Log.d(TAG, "getbTimeMill : " + String.valueOf(bTimeMill));

        return bTimeMill;
    }

    public void setbTimeMill(long bTimeMill) {
        Log.d(TAG, "setbTimeMill : " + String.valueOf(bTimeMill));

        this.bTimeMill = bTimeMill;
    }

    public long getaTimeMill() {
        Log.d(TAG, "getaTimeMill : " + String.valueOf(aTimeMill));

        return aTimeMill;
    }

    public void setaTimeMill(long aTimeMill) {
        Log.d(TAG, "setaTimeMill : " + String.valueOf(aTimeMill));


        this.aTimeMill = aTimeMill;
    }


    public int getaAudioShortDx() {
        Log.d(TAG, "getaAudioShortDx : " + String.valueOf(aAudioShortDx));

        return aAudioShortDx;
    }

    public void setaAudioShortDx(int aAudioShortDx) {
        Log.d(TAG, "setaAudioShortDx : " + String.valueOf(aAudioShortDx));


        this.aAudioShortDx = aAudioShortDx;
    }

    public int getbAudioShortDx() {
        Log.d(TAG, "getbAudioShortDx : " + String.valueOf(bAudioShortDx));

        return bAudioShortDx;
    }

    public void setbAudioShortDx(int bAudioShortDx) {
        Log.d(TAG, "setbAudioShortDx : " + String.valueOf(bAudioShortDx));
        this.bAudioShortDx = bAudioShortDx;
    }
    public int getbVideoDxOfFrame() {
        return bVideoDxOfFrame;
    }

    public void setbVideoDxOfFrame(int bVideoDxOfFrame) {
        this.bVideoDxOfFrame = bVideoDxOfFrame;
    }

    public int getaVideoDxOfFrame() {
        return aVideoDxOfFrame;
    }

    public void setaVideoDxOfFrame(int aVideoDxOfFrame) {
        this.aVideoDxOfFrame = aVideoDxOfFrame;
    }





    public int getbVideoDxOfByteArrayTotal() {
        return bVideoDxOfByteArrayTotal;
    }

    public void setbVideoDxOfByteArrayTotal(int bVideoDxOfByteArrayTotal) {
        this.bVideoDxOfByteArrayTotal = bVideoDxOfByteArrayTotal;
    }

    public int getaVideoDxOfByteArrayTotal() {
        return aVideoDxOfByteArrayTotal;
    }

    public void setaVideoDxOfByteArrayTotal(int aVideoDxOfByteArrayTotal) {
        this.aVideoDxOfByteArrayTotal = aVideoDxOfByteArrayTotal;
    }

}
