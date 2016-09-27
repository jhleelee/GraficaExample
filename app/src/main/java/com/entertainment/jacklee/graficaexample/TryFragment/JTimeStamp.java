package com.entertainment.jacklee.graficaexample.TryFragment;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Jacklee on 16. 4. 17..
 */
public class JTimeStamp {

    public  static long longTimeStampA = 1;
    public static long longTimeStampB= 0;
    public  static long longTimeStampC= 0;
    public  static long  longBreakTime = 0;
    public  static long  longLastInterval = 0;

  public static long getRelativeStamp(long l ){
      Log.d(TAG, "input : "+ l);
       Log.d(TAG, "SystemNow "+ System.currentTimeMillis());
      long stamp = l - (longBreakTime /10) +longLastInterval; //+longLastInterval
      putStampStraight(stamp);
      Log.d(TAG, "stamp : "+ stamp);
      return stamp;
  }



    public static void putStampStraight(long l){
        if (longTimeStampA==0){
            putStampA(l);
            return;
        }

        if (longTimeStampB==0){
            putStampB(l);
            return;
        }

        if (longTimeStampC==0){
            putStampC(l);
            return;
        }

    }

      static void putStampA(long l){
        longTimeStampA = l;
        longTimeStampB = 0;
    }

      static void putStampB(long l){
        longTimeStampB = l;
        longTimeStampC = 0;
    }

      static void putStampC(long l){
        longTimeStampC = l;
        longTimeStampA = 0;
    }


    public static long getLatestInterval(){

        if (longTimeStampB==0&&longTimeStampC==0){
            return 0;
        }


        if (longTimeStampA==0){
            return longTimeStampC-longTimeStampB;
        }

        if (longTimeStampB==0){
            return longTimeStampA-longTimeStampC;
        }

        if (longTimeStampC==0){
            return longTimeStampB-longTimeStampA;
        }

        return 0;

    }

    public static long getBeginTimeStamp(){

        return getEndTimeStamp() + getLatestInterval();
    }




    public static long getEndTimeStamp(){

        if (longTimeStampB==0&&longTimeStampC==0){
            return 0;
        }


        if (longTimeStampA==0){
            return longTimeStampC;
        }

        if (longTimeStampB==0){
            return longTimeStampA;
        }

        if (longTimeStampC==0){
            return longTimeStampB;
        }
        return 0;
    }

    public static void onTouch(){
        Log.d(TAG,"onTouch()");
        long longSystemOnTouch = System.currentTimeMillis();
        if (longSystemOffTouch==0){
            longBreakTime = 0;
        } else {
            longBreakTime = longSystemOnTouch - longSystemOffTouch;
        }
        Log.d(TAG,"longNow, longBreakTime : "+ longSystemOnTouch+", "+longBreakTime);
    }

    static long longSystemOffTouch = 0;

    public static void offTouch(){
         longSystemOffTouch = System.currentTimeMillis();

        saveEndTimeStamps();
        longLastInterval = getLatestInterval();
        Log.d(TAG, "longLastInterval" + longLastInterval);
    }
 static    String TAG = "JTimeStamp";
    public  static ArrayList<Long> arlBeginTimeStamps;
    public  static ArrayList<Long> arlEndTimeStamps;

    public static void saveBeginTimeStamps(){
        if (arlBeginTimeStamps==null){
            arlBeginTimeStamps= new ArrayList<>();
        }
        arlBeginTimeStamps.add(getBeginTimeStamp());
    }


    public static void saveEndTimeStamps(){
        if (arlEndTimeStamps==null){
            arlEndTimeStamps= new ArrayList<>();
        }
        arlEndTimeStamps.add(getEndTimeStamp());
    }

}
