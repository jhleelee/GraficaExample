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

package com.entertainment.jacklee.graficaexample;

import android.hardware.Camera;
import android.util.Log;
import android.util.Size;

import java.util.List;

/**
 * Camera-related utility functions.
 */
public class CameraUtils {
    private static final String TAG = "CameraUtils";

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.

        /*
        D/CameraUtils: Camera preferred preview size for video is 1920x1080
         */

        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }


        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            Log.d(TAG, "supported: " + size.width + "x" + size.height);

            /*
            D/CameraUtils: supported: 1920x1080
            D/CameraUtils: supported: 1600x1200
            D/CameraUtils: supported: 1536x864
            D/CameraUtils: supported: 1280x960
            D/CameraUtils: supported: 1280x768
            D/CameraUtils: supported: 1280x720
             */
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        //I/CameraFragment: Camera config: 1280x720 @15.0fps

        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }






//    private void checkSupportedPictureSizeAtPreviewSize(List<Camera.Size> pictureSizes) {
//        List<Camera.Size> previewSizes = getParameters().getSupportedPreviewSizes();
//        Camera.Size pictureSize;
//        Camera.Size previewSize;
//        double pictureRatio = 0;
//        double previewRatio = 0;
//        final double aspectTolerance = 0.05;
//        boolean isUsablePicture = false;
//
//        for (int indexOfPicture = pictureSizes.size() - 1; indexOfPicture >= 0; --indexOfPicture) {
//            pictureSize = pictureSizes.get(indexOfPicture);
//            pictureRatio = (double) pictureSize.width / (double) pictureSize.height;
//            isUsablePicture = false;
//
//            for (int indexOfPreview = previewSizes.size() - 1; indexOfPreview >= 0; --indexOfPreview) {
//                previewSize = previewSizes.get(indexOfPreview);
//
//                previewRatio = (double) previewSize.width / (double) previewSize.height;
//
//                if (Math.abs(pictureRatio - previewRatio) < aspectTolerance) {
//                    isUsablePicture = true;
//                    break;
//                }
//            }
//
//            if (isUsablePicture == false) {
//                pictureSizes.remove(indexOfPicture);
//                Log.d(TAG, "remove picture size : " + pictureSize.width + ", " + pictureSize.height);
//            }
//        }
//
//    }


    private static Camera.Size getCameraPreviewSize(List<Camera.Size> previewList, int targetPreviewWidth, int targetPreviewHeight, double targetRatio, int targetHeight) {
        final double aspectTolerance = 0.05;
        double      minDiff = Double.MAX_VALUE;
        Camera.Size optimalSize = null;

        for (Camera.Size size : previewList) {
            double ratio = (double) size.width / size.height;

            if (Math.abs(ratio - targetRatio) > aspectTolerance) {
                continue;
            }

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        return optimalSize;
    }
}
