package com.entertainment.jacklee.graficaexample.CameraFilter.filter.BlurFilter;

import android.content.Context;
import android.opengl.GLES20;
import java.nio.FloatBuffer;
import com.entertainment.jacklee.graficaexample.CameraFilter.filter.CameraFilter;
import com.entertainment.jacklee.graficaexample.R;
import com.entertainment.jacklee.graficaexample.gles.GlUtil;

class CameraFilterGaussianSingleBlur extends CameraFilter {

    private int muTexelWidthOffset;
    private int muTexelHeightOffset;

    private float mBlurRatio;
    private boolean mWidthOrHeight;

    public CameraFilterGaussianSingleBlur(Context applicationContext, float blurRatio,
            boolean widthOrHeight) {
        super(applicationContext);
        mBlurRatio = blurRatio;
        mWidthOrHeight = widthOrHeight;
    }

    @Override protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(applicationContext, R.raw.vertex_shader_blur,
                R.raw.fragment_shader_ext_blur);
    }

    @Override protected void getGLSLValues() {
        super.getGLSLValues();

        muTexelWidthOffset = GLES20.glGetUniformLocation(mGLProgId, "uTexelWidthOffset");
        muTexelHeightOffset = GLES20.glGetUniformLocation(mGLProgId, "uTexelHeightOffset");
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
            int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);

        if (mWidthOrHeight) {
            GLES20.glUniform1f(muTexelWidthOffset,
                    mIncomingWidth == 0 ? 0f : mBlurRatio / mIncomingWidth);
        } else {
            GLES20.glUniform1f(muTexelHeightOffset,
                    mIncomingHeight == 0 ? 0f : mBlurRatio / mIncomingHeight);
        }
    }
}
