package com.entertainment.jacklee.graficaexample.CameraFilter.filter;

import android.content.Context;
import android.util.Log;

import com.entertainment.jacklee.graficaexample.R;


public class FilterManager {

    private static int mCurveIndex;
    private static int[] mCurveArrays = new int[] {
            R.raw.cross_1, R.raw.cross_2, R.raw.cross_3, R.raw.cross_4, R.raw.cross_5,
            R.raw.cross_6, R.raw.cross_7, R.raw.cross_8, R.raw.cross_9, R.raw.cross_10,
            R.raw.cross_11,
    };

    private FilterManager() {
    }

    public static IFilter getCameraFilter(FilterType filterType, Context context) {
        Log.d(TAG, "getCameraFilter(FilterType filterType, Context context)");

        switch (filterType) {
            case Normal:
            default:
                return new CameraFilter(context);
            case Blend:
                return new CameraFilterBlend(context, R.drawable.mask);
            case SoftLight:
                return new CameraFilterBlendSoftLight(context, R.drawable.mask);

            case ToneCurve0:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[0]));

            case ToneCurve1:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[1]));


            case ToneCurve2:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[2]));


            case ToneCurve3:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[3]));


            case ToneCurve4:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[4]));


            case ToneCurve5:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[5]));


            case ToneCurve6:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[6]));


            case ToneCurve7:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[7]));


            case ToneCurve8:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[8]));


            case ToneCurve9:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[9]));


            case ToneCurve10:
                return new CameraFilterToneCurve(context,
                        context.getResources().openRawResource(mCurveArrays[10]));



            case Jack:
                return new CameraFilterJack(context, "romance");


        }
    }
    public static String TAG = "FilterManager";
//    public static IFilter getImageFilter(FilterType filterType, Context context) {
//        Log.d(TAG, "getImageFilter(FilterType filterType, Context context)");
//        switch (filterType) {
//            case Normal:
//            default:
//                return new ImageFilter(context);
//            case Blend:
//                return new ImageFilterBlend(context, R.drawable.mask);
//            case SoftLight:
//                return new ImageFilterBlendSoftLight(context, R.drawable.mask);
//            case ToneCurve:
//                mCurveIndex++;
//                if (mCurveIndex > 10) {
//                    mCurveIndex = 0;
//                }
//                Log.d(TAG, "return new ImageFilterToneCurve(context,\n" +
//                        "                        context.getResources().openRawResource(mCurveArrays[mCurveIndex]));\n "+String.valueOf(mCurveIndex));
//                return new ImageFilterToneCurve(context,
//                        context.getResources().openRawResource(mCurveArrays[mCurveIndex]));
//        }
//    }

    public enum FilterType {
        Normal, Blend, SoftLight, ToneCurve0, ToneCurve1, ToneCurve2, ToneCurve3, ToneCurve4, ToneCurve5,
        ToneCurve6, ToneCurve7, ToneCurve8, ToneCurve9, ToneCurve10, Jack
    }
}
