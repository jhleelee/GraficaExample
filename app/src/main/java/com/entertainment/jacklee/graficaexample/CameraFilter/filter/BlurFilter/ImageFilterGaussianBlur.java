package com.entertainment.jacklee.graficaexample.CameraFilter.filter.BlurFilter;

import android.content.Context;
import com.entertainment.jacklee.graficaexample.CameraFilter.filter.CameraFilter;
import com.entertainment.jacklee.graficaexample.CameraFilter.filter.FilterGroup;

public class ImageFilterGaussianBlur extends FilterGroup<CameraFilter> {

    public ImageFilterGaussianBlur(Context context, float blur) {
        super();
        addFilter(new ImageFilterGaussianSingleBlur(context, blur, false));
        addFilter(new ImageFilterGaussianSingleBlur(context, blur, true));
    }
}
