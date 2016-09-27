package com.entertainment.jacklee.graficaexample.CameraFilter.filter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import com.entertainment.jacklee.graficaexample.R;
import com.entertainment.jacklee.graficaexample.gles.GlUtil;

import java.nio.FloatBuffer;


public class CameraFilter extends AbstractFilter implements IFilter {

    protected int mGLProgId;
    private int maPositionLoc;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maTextureCoordLoc;
    private int mGLUniformTexture;

    protected int mIncomingWidth, mIncomingHeight;

    public CameraFilter(Context applicationContext) {
        mGLProgId = createProgram(applicationContext);
        if (mGLProgId == 0) {
            throw new RuntimeException("Unable to create program");
        }
        getGLSLValues();
    }

    @Override
    public int getTextureTarget() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    @Override
    public void setTextureSize(int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
        if (width == mIncomingWidth && height == mIncomingHeight) {
            return;
        }
        mIncomingWidth = width;
        mIncomingHeight = height;
    }

    @Override
    protected int createProgram(Context applicationContext) {

        return GlUtil.createProgram(
                applicationContext,
                R.raw.vertex_shader,
                R.raw.fragment_shader_ext);
    }

    @Override
    protected void getGLSLValues() {
        /*********************************************************************
         * glGetUniformLocation — return the location of a uniform variable
         * *******************************************************************
         * program : Specifies the program object to be queried.
         * name : Points to a null terminated string containing the name of the uniform variable whose
         * location is to be queried.
         * glGetUniformLocation returns an integer that represents the location of a specific uniform
         * variable within a program object. name must be a null terminated string that contains no
         * white space. name must be an active uniform variable name in program that is not a structure,
         * an array of structures, or a subcomponent of a vector or a matrix. This function returns -1 if
         * name does not correspond to an active uniform variable in program or if name starts with the
         * reserved prefix "gl_".
         *********************************************************************/

        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "uTexture");

        /*********************************************************************
         * glGetAttribLocation — return the location of an attribute variable
         * *******************************************************************
         * glGetAttribLocation queries the previously linked program object specified by
         * program for the attribute variable specified by name and returns the index of the
         * generic vertex attribute that is bound to that attribute variable. If name is a matrix
         * attribute variable, the index of the first column of the matrix is returned. If the named
         * attribute variable is not an active attribute in the specified program object or if name
         * starts with the reserved prefix "gl_", a value of -1 is returned.
         *********************************************************************/
        maPositionLoc = GLES20.glGetAttribLocation(mGLProgId, "aPosition");

        muMVPMatrixLoc = GLES20.glGetUniformLocation(mGLProgId, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mGLProgId, "uTexMatrix");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mGLProgId, "aTextureCoord");
    }

    String TAG = "CameraFilter";

    @Override
    public void onDraw(
            float[] mvpMatrix,
            FloatBuffer vertexBuffer,
            int firstVertex,
            int vertexCount,
            int coordsPerVertex,
            int vertexStride,
            float[] texMatrix,
            FloatBuffer texBuffer,
            int textureId,
            int texStride) {
//        Log.d(TAG, "onDraw()");
        GlUtil.checkGlError("draw start");

        glUseProgram();

        bindTexture(textureId);

        //runningOnDraw();

        bindGLSLValues(mvpMatrix, vertexBuffer, coordsPerVertex, vertexStride, texMatrix, texBuffer,
                texStride);

        drawArrays(firstVertex, vertexCount);

        unbindGLSLValues();

        unbindTexture();

        disuseProgram();
    }

    @Override
    protected void glUseProgram() {
        /****************************************************************************
         * glUseProgram — install a program object as part of current rendering state
         ***************************************************************************/
        GLES20.glUseProgram(mGLProgId);
        //GlUtil.checkGlError("glUseProgram");
    }

    @Override
    protected void bindTexture(int objectTextureId) {
        /****************************************************************************
         * void glActiveTexture(GLenum texture);
         * texture : Specifies which texture unit to make active. The number of texture units is
         * implementation dependent, but must be at least 8. texture must be one of GL_TEXTUREi,
         * where i ranges from 0 to (GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS - 1).
         * The initial value is GL_TEXTURE0.
         ***************************************************************************/
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        /****************************************************************************
         * glBindTexture — bind a named texture to a texturing target
         ***************************************************************************/
        GLES20.glBindTexture(getTextureTarget(), objectTextureId);

        /*********************************************************************************
         * glUniform — specify the value of a uniform variable for the current program object
         * location : Specifies the location of the uniform variable to be modified.
         * v0 : Specifies the new values to be used for the specified uniform variable.
         ********************************************************************************/
//        Log.d(TAG, "mGLUniformTexture " + String.valueOf(mGLUniformTexture));
        GLES20.glUniform1i(mGLUniformTexture, 0); // Jack : mGLUniformTexture -> mGLUniformTexture
    }

    @Override
    protected void bindGLSLValues(float[] mvpMatrix,
                                  FloatBuffer vertexBuffer,
                                  int coordsPerVertex,
                                  int vertexStride,
                                  float[] texMatrix,
                                  FloatBuffer texBuffer,
                                  int texStride) {
        /*****************************************************************************************
         * glUniform — specify the value of a uniform variable for the current program object
         ***************************************************************************************
         * location : Specifies the location of the uniform value to be modified.
         * count : Specifies the number of matrices that are to be modified. This should be 1
         * if the targeted uniform variable is not an array of matrices, and 1 or more if it is
         * an array of matrices.
         * transpose : Specifies whether to transpose the matrix as the values are loaded into the
         * uniform variable. Must be GL_FALSE.
         * value : Specifies a pointer to an array of count values that will be used to update
         * the specified uniform variable.
        *********************************************************************************************/

        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);

        GLES20.glEnableVertexAttribArray(maPositionLoc);

        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex, GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);

        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2, GLES20.GL_FLOAT, false, texStride,
                texBuffer);
    }

    @Override
    protected void drawArrays(int firstVertex, int vertexCount) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        /*****************************************************************************************
         * glDrawArrays — render primitives from array data
         ***************************************************************************************
         * mode : Specifies what kind of primitives to render. Symbolic constants GL_POINTS,
         * GL_LINE_STRIP, GL_LINE_LOOP, GL_LINES, GL_TRIANGLE_STRIP, GL_TRIANGLE_FAN, and GL_TRIANGLES are accepted.
         * first : Specifies the starting index in the enabled arrays.
         * count : Specifies the number of indices to be rendered.
         *********************************************************************************************/
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
    }

    @Override
    protected void unbindGLSLValues() {
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
    }

    @Override
    protected void unbindTexture() {
        /*************************************************************************************************
         * glBindTexture — bind a named texture to a texturing target
         * target : Specifies the target of the active texture unit to which the texture is bound.
         * Must be either GL_TEXTURE_2D or GL_TEXTURE_CUBE_MAP.
         * texture : Specifies the name of a texture. Texture names are unsigned integers.
         * The value zero is reserved to represent the default texture for each texture target.
         * Texture names and the corresponding texture contents are local to the shared object space
         * of the current GL rendering context.
         ************************************************************************************************/

        GLES20.glBindTexture(getTextureTarget(), 0);
    }

    @Override
    protected void disuseProgram() {
        GLES20.glUseProgram(0);
    }

    @Override
    public void releaseProgram() {
        GLES20.glDeleteProgram(mGLProgId);
        mGLProgId = -1;
    }

    /////////// Set Runnable ////////////
    //protected void addRunnableOnDraw(final Runnable runnable) {
    //    synchronized (mRunnableOnDraw) {
    //        mRunnableOnDraw.addLast(runnable);
    //    }
    //}
    //
    //protected void setFloat(final int location, final float floatValue) {
    //    addRunnableOnDraw(new Runnable() {
    //        @Override public void run() {
    //            GLES20.glUniform1f(location, floatValue);
    //        }
    //    });
    //}
    //
    //@Override protected void runningOnDraw() {
    //    while (!mRunnableOnDraw.isEmpty()) {
    //        mRunnableOnDraw.removeFirst().run();
    //    }
    //}
}
