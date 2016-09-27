package com.entertainment.jacklee.graficaexample.CameraFilter.filter.BlurFilter;

import android.content.Context;

import com.entertainment.jacklee.graficaexample.CameraFilter.filter.CameraFilter;
import com.entertainment.jacklee.graficaexample.CameraFilter.filter.FilterGroup;


public class CameraFilterGaussianBlur extends FilterGroup<CameraFilter> {

    public CameraFilterGaussianBlur(Context context, float blur) {
        super();
        addFilter(new CameraFilterGaussianSingleBlur(context, blur, false));
        addFilter(new CameraFilterGaussianSingleBlur(context, blur, true));
    }
}
