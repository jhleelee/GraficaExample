/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.entertainment.jacklee.graficaexample.gles;

/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Bitmap;
import android.opengl.Matrix;
import android.util.Log;

import com.entertainment.jacklee.graficaexample.CameraFilter.filter.IFilter;


/**
 * This class essentially represents a viewport-sized sprite (In computer graphics, a sprite (also known by other names; see synonyms below) is a two-dimensional image or animation that is integrated into a larger scene. Initially including just graphical objects handled separately from the memory bitmap of a video display, this now includes various manners of graphical overlays.)
 * that will be rendered with
 * a texture, usually from an external source like the camera or video decoder.
 */
public class FullFrameRect {
    private final Drawable2d mRectDrawable = new Drawable2d();
    private IFilter mFilter;
    public final float[] IDENTITY_MATRIX = new float[16];

    String   TAG = "FullFrameRect";

    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     * the program when no longer needed.
     */
    public FullFrameRect(IFilter program) {
        mFilter = program;
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }


    /**
     * Releases resources.
     * <p>
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mFilter != null) {
            if (doEglCleanup) {
                mFilter.releaseProgram();
            }
            mFilter = null;
        }
    }

    /**
     * Returns the program currently in use.
     */
    public IFilter getFilter() {
        return mFilter;
    }

    /**
     * Changes the program.  The previous program will be released.
     * <p>
     * The appropriate EGL context must be current.
     */
    public void changeProgram(IFilter newFilter) {
        Log.d(TAG, "changeProgram(IFilter newFilter)");
        mFilter.releaseProgram();
        mFilter = newFilter;
    }

    /**
     * Creates a texture object suitable for use with drawFrameWithFilter().
     */
    public int createTexture() {
        Log.d(TAG, "createTexture()");
        return GlUtil.createTexture(mFilter.getTextureTarget());
    }

    public int createTexture(Bitmap bitmap) {
        Log.d(TAG, "createTexture(Bitmap bitmap)");

        return GlUtil.createTexture(mFilter.getTextureTarget(), bitmap);
    }

    public void scaleMVPMatrix(float x, float y) {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
        Matrix.scaleM(IDENTITY_MATRIX, 0, x, y, 1f);
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */

    public void drawFrameWithFilter(int textureId, float[] texMatrix) {

        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mFilter.onDraw(
                IDENTITY_MATRIX,
                mRectDrawable.getVertexArray(),
                0,
                mRectDrawable.getVertexCount(),
                mRectDrawable.getCoordsPerVertex(),
                mRectDrawable.getVertexStride(),
                texMatrix,
                mRectDrawable.getTexCoordArray(),
                textureId,
                mRectDrawable.getTexCoordStride());
    }




    /******************************************************
     * ORIGINAL
     *****************************************************/
        private Texture2dProgram mProgram;

    public FullFrameRect(Texture2dProgram program) {
        mProgram = program;
    }

        public Texture2dProgram getProgram() {
        return mProgram;
    }

        public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }
}























///*
// * Copyright 2014 Google Inc. All rights reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.entertainment.jacklee.graficaexample.gles;
//
//import android.opengl.Matrix;
//
///**
// * This class essentially represents a viewport-sized sprite that will be rendered with
// * a texture, usually from an external source like the camera or video decoder.
// */
//public class FullFrameRect {
//    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
//    private Texture2dProgram mProgram;
//
//    /**
//     * Prepares the object.
//     *
//     * @param program The program to use.  FullFrameRect takes ownership, and will release
//     *     the program when no longer needed.
//     */
//    public FullFrameRect(Texture2dProgram program) {
//        mProgram = program;
//    }
//
//    /**
//     * Releases resources.
//     * <p>
//     * This must be called with the appropriate EGL context current (i.e. the one that was
//     * current when the constructor was called).  If we're about to destroy the EGL context,
//     * there's no value in having the caller make it current just to do this cleanup, so you
//     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
//     */
//    public void release(boolean doEglCleanup) {
//        if (mProgram != null) {
//            if (doEglCleanup) {
//                mProgram.release();
//            }
//            mProgram = null;
//        }
//    }
//
//    /**
//     * Returns the program currently in use.
//     */
//    public Texture2dProgram getProgram() {
//        return mProgram;
//    }
//
//    /**
//     * Changes the program.  The previous program will be released.
//     * <p>
//     * The appropriate EGL context must be current.
//     */
//    public void changeProgram(Texture2dProgram program) {
//        mProgram.release();
//        mProgram = program;
//    }
//
//    /**
//     * Creates a texture object suitable for use with drawFrameWithFilter().
//     */
//    public int createTextureObject() {
//        return mProgram.createTextureObject();
//    }
//
//    /**
//     * Draws a viewport-filling rect, texturing it with the specified texture object.
//     */
//    public void drawFrameWithFilter(int textureId, float[] texMatrix) {
//        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
//        mProgram.draw(
//                GlUtil.IDENTITY_MATRIX,
//                mRectDrawable.getVertexArray(),
//                0,
//                mRectDrawable.getVertexCount(),
//                mRectDrawable.getCoordsPerVertex(),
//                mRectDrawable.getVertexStride(),
//                texMatrix,
//                mRectDrawable.getTexCoordArray(),
//                textureId,
//                mRectDrawable.getTexCoordStride());
//    }
//}
