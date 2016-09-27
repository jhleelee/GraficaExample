package com.entertainment.jacklee.graficaexample.CameraFilter.filter;

import android.content.Context;
import android.opengl.GLES20;

import com.entertainment.jacklee.graficaexample.R;
import com.entertainment.jacklee.graficaexample.gles.GlUtil;

import java.nio.FloatBuffer;

/**
 * Created by Jacklee on 16. 4. 7..
 */
public class CameraFilterJack extends CameraFilter {

     private final int mToneCurveTextureId;
    protected int muToneCurveTextureLoc;

    private String stFilterType;

    public CameraFilterJack(Context context, String stFilterType) {
        super(context);
        this.stFilterType = stFilterType;
        mToneCurveTextureId = GlUtil.createTexture(GLES20.GL_TEXTURE_2D);
    }

    @Override protected int createProgram(Context applicationContext) {
        return GlUtil.createProgram(
                applicationContext,
                R.raw.vertex_shader,
                R.raw.romance);
    }

    @Override protected void getGLSLValues() {
        super.getGLSLValues();

        muToneCurveTextureLoc = GLES20.glGetUniformLocation(mGLProgId, "curve");

    }

    @Override protected void bindTexture(int objectTextureId) {
        super.bindTexture(objectTextureId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mToneCurveTextureId);
        GLES20.glUniform1i(muToneCurveTextureLoc, 1);
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix, FloatBuffer vertexBuffer, int coordsPerVertex,
                                  int vertexStride, float[] texMatrix, FloatBuffer texBuffer, int texStride) {
        super.bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix,
                texBuffer, texStride);
    }

    @Override protected void unbindGLSLValues() {
        super.unbindGLSLValues();

    }

    @Override protected void unbindTexture() {
        super.unbindTexture();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}