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

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Activity;


import com.entertainment.jacklee.graficaexample.CameraFilter.filter.FilterManager;
import com.entertainment.jacklee.graficaexample.Widget.FilterBaseAdapter;
import com.entertainment.jacklee.graficaexample.Widget.Grab;
import com.entertainment.jacklee.graficaexample.Widget.LVItemFilter;
//import com.entertainment.jacklee.graficaexample.Widget.ProgressView;
import com.entertainment.jacklee.graficaexample.gles.EglCore;
import com.entertainment.jacklee.graficaexample.gles.FullFrameRect;
import com.entertainment.jacklee.graficaexample.gles.WindowSurface;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Demonstrates capturing video into a ring buffer.
 * <p/>
 * When the "capture" button is clicked, the buffered video is saved.
 * <p/>
 * Capturing and storing raw frames would be slow and require lots of memory.
 * Instead, we feed the frames into the video encoder and buffer the output.
 * <p/>
 * Whenever we receive a new frame from the camera,
 * our SurfaceTexture callback gets notified.
 * <p/>
 * That can happen on an arbitrary thread, so we use it to send a message
 * through our Handler.
 * <p/>
 * That causes us to render the new frame to the display and to
 * our video encoder.
 */
public class ContinuousCaptureActivity extends Activity implements
        View.OnClickListener,
        AdapterView.OnItemClickListener,
        SurfaceHolder.Callback,
        SurfaceTexture.OnFrameAvailableListener {




    private static final String TAG = "ContiCaptureActivity";

    private static final int VIDEO_WIDTH = 1280;  // dimensions for 720p video
    private static final int VIDEO_HEIGHT = 720;
    private static final int DESIRED_PREVIEW_FPS = 15;

    //GL
    private EglCore mEglCore;
    private WindowSurface surfaceSurfaceView_BpToSurfaceFlingerConsumer;
    private SurfaceTexture surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener;  // receives the output from the camera preview
    private FullFrameRect fullFrameRect;
    private final float[] mTmpMatrix = new float[16];
    private int intOpenGLTextureName;



    private int mFrameNum;

    // Camera
    private Camera mCamera;
    private int mCameraPreviewThousandFps;

    private File mOutputFile;


    //Encoder
    private CircularEncoder mCircEncoder;
    private WindowSurface windowSurface_BpToGraphicBufferSource;
    private boolean mFileSaveInProgress;

    private MainHandler mainHandler;
    private float floatSecondsOfVideo;

//    private ProgressView progressView;
    int intMaxRecordSec = 15;

    /**
     * Custom message handler for main UI thread.
     * <p/>
     * Used to handle camera preview "frame available" notifications, and implement the
     * blinking "recording" text.  Receives callback messages from the encoder thread.
     */
    private static class MainHandler extends Handler implements CircularEncoder.Callback {
        public static final int MSG_BLINK_TEXT = 0;
        public static final int MSG_FRAME_AVAILABLE = 1;
        public static final int MSG_FILE_SAVE_COMPLETE = 2;
        public static final int MSG_BUFFER_STATUS = 3;

        private WeakReference<ContinuousCaptureActivity> mWeakActivity;

        public MainHandler(ContinuousCaptureActivity activity) {
            mWeakActivity = new WeakReference<ContinuousCaptureActivity>(activity);
        }

        // CircularEncoder.Callback, called on encoder thread
        @Override
        public void fileSaveComplete(int status) {
            sendMessage(obtainMessage(MSG_FILE_SAVE_COMPLETE, status, 0, null));
        }

        // CircularEncoder.Callback, called on encoder thread
        @Override
        public void bufferStatus(long totalTimeMsec) {
            sendMessage(obtainMessage(MSG_BUFFER_STATUS,
                    (int) (totalTimeMsec >> 32), (int) totalTimeMsec));
        }


        @Override
        public void handleMessage(Message msg) {
            ContinuousCaptureActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.d(TAG, "Got message for dead activity");
                return;
            }

            switch (msg.what) {

                case MSG_FRAME_AVAILABLE: {
                    /************************************************************************

                     [ContinueosCaptureActivity : SurfaceTexture.OnFrameAvailableListener]

                     @Override public void onFrameAvailable(SurfaceTexture surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener) {
                     //Log.d(TAG, "frame available");
                     mainHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
                     }

                     ***********************************************************************/
                    activity.drawFrameOntoSurfaceViewAndEncoderSurface();
//                    activity.updateProgressBarJack(activity.isPressingRecordBtn);
                    break;
                }
                case MSG_FILE_SAVE_COMPLETE: {
                    activity.fileSaveComplete(msg.arg1);
                    break;
                }
                case MSG_BUFFER_STATUS: {
                    long duration = (((long) msg.arg1) << 32) |
                            (((long) msg.arg2) & 0xffffffffL);
                    Log.d(TAG, "handleMessage() MSG_BUFFER_STATUS " + String.valueOf(duration));
                    activity.updateBufferStatus(duration);
                    break;
                }
                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }
    }

    private PowerManager.WakeLock mWakeLock;
    private int recordingTime = 15000;
    //录制的最短时间
    private int recordingMinimumTime = 6000;
    //提示换个场景
    private int recordingChangeTime = 3000;
    int intTagToTimeItem = 0;
    LinearLayout loRecordBtnEtc;
    Button btRecord, btFlash, btChangeCamera, btFilter;
    Context mContext;
    private ArrayList<LVItemFilter> rvItemFilterList = new ArrayList<>();
    private ListView lvFilter;
    private FilterBaseAdapter filterBaseAdapter;


    boolean isFlashOn = false;
    Camera.Parameters cameraParameters = null;

    /**************************************
     * CameraFilter - TextureMovieEncoder
     *************************************/
    private FilterManager.FilterType mCurrentFilterType = FilterManager.FilterType.Normal;

    private FilterManager.FilterType mNewFilterType = null;

    private   String stCurrentFilterType = "";
    private   String stNewFilterType = "";

    // The first screen is pressed record time
    long firstTime = 0;

    //  Lift your finger   the time
    long startPauseTime = 0;

    //Each time you press the pause time between the fingers and lifted
    long totalPauseTime = 0;
    //手指抬起是的时间
    long pausedTime = 0;
    //总的暂停时间
    long stopPauseTime = 0;
    //录制的有效总时间
    long totalTime = 0;

    boolean isPressingRecordBtn = false;
    int intRecordedFrameNum = 0;

    private int mIncomingWidth, mIncomingHeight;


    /**************************************
     * CameraFilter - CameraRecordRenderer
     *************************************/
    public static int intTotalVideoLengthSeconds = 30;
    private int mSurfaceWidth, mSurfaceHeight;

    private float mMvpScaleX = 1f, mMvpScaleY = 1f;


    // Determine the need for recording, pause recording Click Next
    private boolean rec = false;
    int cameraSelection = 0;

    int screenWidth, screenHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        // Fullscreen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_continuous_capture);
        mContext = this;

        // WakeLock
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        //Find screen dimensions
        screenWidth = displaymetrics.widthPixels;
        screenHeight = displaymetrics.heightPixels;

        // init Configs
        mOutputFile = new File(getFilesDir(), "continuous-capture.mp4");
        Log.d(TAG, "getFilesDir() " + getFilesDir().toString());
        floatSecondsOfVideo = 0.0f;
//        updateControls();
        Grab.arlGrabbedBbEncodedData = new ArrayList<>();
        Grab.arlTimesOfBlocks = new ArrayList<>();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mainHandler = new MainHandler(this);

        initUI();
        initOnClickListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*******
         * "Remove any pending posts of callbacks and sent messages whose obj is token.
         * If token is null, all callbacks and messages will be removed."
         */
        mainHandler.removeCallbacksAndMessages(null);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        isResumed = true;
        // Ideally, the frames from the camera are at the same resolution as the input to
        // the video encoder so we don't have to scale.

        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }

        openCamera(
                VIDEO_WIDTH, // 1280
                VIDEO_HEIGHT,  //720
                DESIRED_PREVIEW_FPS); //15
        initListenersForCamera();

        trySetCameraTextureAndEncoder();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        isResumed = false;

        releaseCamera();
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        super.onPause();
        if (mCircEncoder != null) {
            mCircEncoder.shutdown();
            mCircEncoder = null;
        }
        if (surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener != null) {
            surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener.release();
            surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener = null;
        }
        if (surfaceSurfaceView_BpToSurfaceFlingerConsumer != null) {
            surfaceSurfaceView_BpToSurfaceFlingerConsumer.release();
            surfaceSurfaceView_BpToSurfaceFlingerConsumer = null;
        }
        if (fullFrameRect != null) {
            fullFrameRect.release(false);
            fullFrameRect = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
        Log.d(TAG, "onPause() done");
    }



    @Override   // SurfaceHolder.Callback
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated()");
        surfaceHolder = holder;
        isSurfaceCreated = true;
        trySetCameraTextureAndEncoder( );
        updateControls();
    }

    @Override   // SurfaceHolder.Callback
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged()" );
        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height +
                " holder=" + holder);

    }

    @Override   // SurfaceHolder.Callback
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed()" );
        Log.d(TAG, "surfaceDestroyed holder=" + holder);
        isSurfaceCreated = false;
        surfaceHolder = null;
    }

    @Override   // SurfaceTexture.OnFrameAvailableListener; runs on arbitrary thread
    public void onFrameAvailable(SurfaceTexture surfaceTexture_BufferConsumer_FromCameraSurface) {
        //Log.d(TAG, "frame available");
        mainHandler.sendEmptyMessage(MainHandler.MSG_FRAME_AVAILABLE);
    }


    boolean isSurfaceCreated= false ;
    boolean isResumed = false ;
    SurfaceHolder surfaceHolder ;


    private void trySetCameraTextureAndEncoder(){
        Log.d(TAG, "trySetCameraTextureAndEncoder()");

        if (isSurfaceCreated&&isResumed){
            Log.d(TAG, "isSurfaceCreated&&isResumed");

            // Set up everything that requires an EGL context.
            //
            // We had to wait until we had a surface because you can't make an EGL context current
            // without one, and creating a temporary 1x1 pbuffer is a waste of time.
            //
            // The display surface that we use for the SurfaceView, and the encoder surface we
            // use for video, use the same EGL context.
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);

            surfaceSurfaceView_BpToSurfaceFlingerConsumer = new WindowSurface(
                    mEglCore,
                    surfaceHolder.getSurface(),
                    false);
            surfaceSurfaceView_BpToSurfaceFlingerConsumer.makeCurrent();

            /**************************************
             *
             * CameraFilter - CameraRecordRenderer #onSurfaceCreated
             *         mVideoEncoder.initFilter(mCurrentFilterType);
             * CameraFilter - TextureMovieEncoder #initFilter
             *   mRecordingEnabled = mVideoEncoder.isRecording();
             if (mRecordingEnabled) {
             mRecordingStatus = RECORDING_RESUMED;
             } else {
             mRecordingStatus = RECORDING_OFF;
             mVideoEncoder.initFilter(mCurrentFilterType);
             }
             *************************************/
            initFilter(mCurrentFilterType); // YET
            fullFrameRect = new FullFrameRect(
                    FilterManager.getCameraFilter(mCurrentFilterType, getApplicationContext()));
            intOpenGLTextureName = fullFrameRect.createTexture();

            surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener = new SurfaceTexture(intOpenGLTextureName);
            surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener.setOnFrameAvailableListener(this);

            try {
                mCamera.setPreviewTexture(surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            mCamera.startPreview();


            // TODO: adjust bit rate based on frame rate?
            // TODO: adjust video width/height based on what we're getting from the camera preview?
            //       (can we guarantee that camera preview size is compatible with AVC video encoder?)
            try {
                mCircEncoder = new CircularEncoder(
                        VIDEO_WIDTH, //width Width of encoded video, in pixels.  Should be a multiple of 16.
                        VIDEO_HEIGHT,//height Height of encoded video, in pixels.  Usually a multiple of 16 (1080 is ok).
                        6000000, // Target bit rate, in bits.
                        mCameraPreviewThousandFps / 1000,//Expected frame rate.
                        intTotalVideoLengthSeconds,//desiredSpanSec How many seconds of video we want to have in our buffer at any time.
                        mainHandler);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            /**
             * Associates an EGL surface with the native window surface.
             * <p>
             * Set releaseSurface to true if you want the Surface to be released when release() is
             * called.  This is convenient, but can interfere with framework classes that expect to
             * manage the Surface themselves (e.g. if you release a SurfaceView's Surface, the
             * surfaceDestroyed() callback won't fire).
             */

            windowSurface_BpToGraphicBufferSource = new WindowSurface(
                    mEglCore,
                    mCircEncoder.getInputSurface(),
                    true);
        }
    }


    private void drawFrameOntoSurfaceViewAndEncoderSurface() {
        //Log.d(TAG, "drawFrameOntoSurfaceViewAndEncoderSurface");
        if (mEglCore == null) {
            Log.d(TAG, "Skipping drawFrameOntoSurfaceViewAndEncoderSurface after shutdown");
            return;
        }


        // Latch the next frame from the camera.
        /**
         * Makes our EGL context current, using the supplied surface for both "draw" and "read".
         */
        surfaceSurfaceView_BpToSurfaceFlingerConsumer.makeCurrent(); // (1) Producer Set
        surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener.updateTexImage(); // JHE - To BBB?
        surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener.getTransformMatrix(mTmpMatrix);

        // Fill the SurfaceView with it.
        SurfaceView sv = (SurfaceView) findViewById(R.id.continuousCapture_CameraSurfaceView);
        int intWidthOfSurfaceView = sv.getWidth();
        int intHeightOfSurfaceView = sv.getHeight();

        /**************************************
         *
         * CameraFilter - CameraRecordRenderer #onDrawFrame
         *
         *************************************/
        GLES20.glViewport(0, 0, intWidthOfSurfaceView, intHeightOfSurfaceView);  // (2) Window Size Set
        if (mNewFilterType !=null&&mNewFilterType != mCurrentFilterType) {
            fullFrameRect.changeProgram(
                    FilterManager.getCameraFilter(
                            mNewFilterType,
                            getApplicationContext()
                    )
            );
            mCurrentFilterType = mNewFilterType;
        }
        fullFrameRect.getFilter().setTextureSize(mIncomingWidth, mIncomingHeight);
        // JHE - BBB?
        fullFrameRect.drawFrameWithFilter(intOpenGLTextureName, mTmpMatrix);              // (3) Filter Set

        // drawExtra(mFrameNum, intWidthOfSurfaceView, intHeightOfSurfaceView);
        surfaceSurfaceView_BpToSurfaceFlingerConsumer.swapBuffers();            // (4) Producer Execute's Swap


        if (isPressingRecordBtn) {

            // Send it to the video encoder.
            if (!mFileSaveInProgress) {
                windowSurface_BpToGraphicBufferSource.makeCurrent(); // (1) Producer Set
                GLES20.glViewport(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);        // (2) Window Size Set
                // JHE - Filter Again?
                fullFrameRect.drawFrameWithFilter(intOpenGLTextureName, mTmpMatrix); // (3) Filter Set
                // drawExtra(mFrameNum, VIDEO_WIDTH, VIDEO_HEIGHT);
                mCircEncoder.frameAvailableSoon();
                windowSurface_BpToGraphicBufferSource.setPresentationTime(surfaceTexture_BcFrCameraSurf_ByCamaraSetPreviewTextureAndSetOnFrameAvailableListener.getTimestamp());
                windowSurface_BpToGraphicBufferSource.swapBuffers(); // (4) Producer Execute's Swap
            }

            intRecordedFrameNum++;
            Log.d(TAG, "JHE Recorded Frames : " + String.valueOf(intRecordedFrameNum));

            /***********************
             * ProgressView
             **********************/
            stopPauseTime = System.currentTimeMillis();

            totalTime = System.currentTimeMillis() - firstTime - pausedTime - ((long) (1.0 / (double) DESIRED_PREVIEW_FPS) * 1000);


        }

        mFrameNum++;

    }



    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p/>
     * Sets mCameraPreviewFps to the expected frame rate (which might actually be variable).
     */
    private void openCamera(int desiredWidth, int desiredHeight, int desiredFps) {
        Log.d(TAG, "openCamera()");


        // (1) Check If Camera Already Initialized
        Log.d(TAG, "openCamera() - (1)");
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        // (2) Get Camera Information
        Log.d(TAG, "openCamera() - (2)");
        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);

            /**
             * The direction that the camera faces. It should be
             * CAMERA_FACING_BACK or CAMERA_FACING_FRONT.
             */
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(i);
                btFlash.setVisibility(View.GONE);
                break;
            } else {
                btFlash.setVisibility(View.VISIBLE);

            }
        }

        // (3) Open Default Camera
        Log.d(TAG, "openCamera() - (3)");
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }

        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        /************************************
         * Camera Parameters
         ***********************************/
        Log.d(TAG, "openCamera() - Parameters");

        cameraParameters = mCamera.getParameters();

        CameraUtils.choosePreviewSize(cameraParameters, desiredWidth, desiredHeight);

        // Try to set the frame rate to a constant value.
        mCameraPreviewThousandFps = CameraUtils.chooseFixedPreviewFps(cameraParameters, desiredFps * 1000);
        Log.d(TAG, "mCameraPreviewThousandFps " + String.valueOf(mCameraPreviewThousandFps));
        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        cameraParameters.setRecordingHint(true);

        mCamera.setParameters(cameraParameters);

        Camera.Size cameraPreviewSize = cameraParameters.getPreviewSize();
        String previewFacts = cameraPreviewSize.width + "x" + cameraPreviewSize.height +
                " @" + (mCameraPreviewThousandFps / 1000.0f) + "fps";
        Log.i(TAG, "Camera config: " + previewFacts);

        // Set the preview aspect ratio.
        AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.continuousCapture_afl);
        layout.setAspectRatio((double) cameraPreviewSize.width / cameraPreviewSize.height);
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        Log.d(TAG, "releaseCamera()");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    private void initListenersForCamera() {
        Log.d(TAG, "initListenersForCamera()");
        /********************************************
         * Flash
         *******************************************/
        btFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    Log.d(TAG, "No Flash Supported");
                    return;
                }
                //闪光灯
                if (isFlashOn) {
                    isFlashOn = false;
                    btFlash.setSelected(false);
                    cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    isFlashOn = true;
                    btFlash.setSelected(true);
                    cameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
                mCamera.setParameters(cameraParameters);
            }
        });

        /********************************************
         * ChangeCamera
         *******************************************/
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            btChangeCamera.setVisibility(View.VISIBLE);
        }

        btChangeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        /********************************************
         * Record
         *******************************************/
        //initListener
        btRecord.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "Grab.arlGrabbedBbEncodedData.size() " + String.valueOf(Grab.arlGrabbedBbEncodedData.size()));

                        if (Grab.arlGrabbedBbEncodedData.size() <= (DESIRED_PREVIEW_FPS * intMaxRecordSec)) {
                            isPressingRecordBtn = true;
                            Grab.longGrabBeginTimeOfBlock = System.currentTimeMillis();
                            Grab.arlTimesOfBlocks.add(Grab.longGrabBeginTimeOfBlock);

                            //JHE - Fr case 3: of Handler
                            stopPauseTime = System.currentTimeMillis();
                            totalPauseTime = stopPauseTime - startPauseTime - ((long) (1.0 / (double) DESIRED_PREVIEW_FPS) * 1000);
                            pausedTime += totalPauseTime;
//                            progressView.setCurrentState(ProgressView.State.START);
                        }

                        return true;

                    case MotionEvent.ACTION_UP:
                        isPressingRecordBtn = false;

                        // Adding timestamp to suspend the progress of the queue
//                        progressView.setCurrentState(ProgressView.State.PAUSE);
//                        progressView.putProgressList((int) totalTime);

                        startPauseTime = System.currentTimeMillis();
                        Log.d(TAG, "totalTime,  startPauseTime" + String.valueOf(totalTime) + ", " + String.valueOf(startPauseTime));

//                        if (totalTime >= recordingMinimumTime) {
//                            currentRecorderState = RecorderState.SUCCESS;
//                            mHandler.sendEmptyMessage(2);
//                        } else if (totalTime >= recordingChangeTime) {
//                            currentRecorderState = RecorderState.CHANGE;
//                            mHandler.sendEmptyMessage(2);
//                        }


                        return true;
                }
                return false;
            }
        });

        /********************************************
         * Filter
         *******************************************/
        btFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loRecordBtnEtc.setVisibility(View.GONE);
                lvFilter.setVisibility(View.VISIBLE);
            }
        });

    }

    /**
     * Updates the current state of the controls.
     */
    private void updateControls() {


        boolean wantEnabled = (mCircEncoder != null) && !mFileSaveInProgress;
        Button button = (Button) findViewById(R.id.btGRAB);
        if (button.isEnabled() != wantEnabled) {
            Log.d(TAG, "setting enabled = " + wantEnabled);
            button.setEnabled(wantEnabled);
        }

        Button TTT = (Button) findViewById(R.id.btGRAB);
        TTT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPressingRecordBtn = !isPressingRecordBtn;
            }
        });

    }



    /**
     * The file save has completed.  We can resume recording.
     */
    private void fileSaveComplete(int status) {
        Log.d(TAG, "fileSaveComplete " + status);
        if (!mFileSaveInProgress) {
            throw new RuntimeException("WEIRD: got fileSaveCmplete when not in progress");
        }
        mFileSaveInProgress = false;
        updateControls();
        String str = getString(R.string.nowRecording);

        if (status == 0) {
            str = getString(R.string.recordingSucceeded);
        } else {
            str = getString(R.string.recordingFailed, status);
        }
        Toast toast = Toast.makeText(this, str, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Updates the buffer status UI.
     */
    private void updateBufferStatus(long durationUsec) {
        floatSecondsOfVideo = durationUsec / 1000000.0f;
        updateControls();
    }

    /**
     * Draws a frame onto the SurfaceView and the encoder surface.
     * <p/>
     * This will be called whenever we get a new preview frame from the camera.  This runs
     * on the UI thread, which ordinarily isn't a great idea -- you really want heavy work
     * to be on a different thread -- but we're really just throwing a few things at the GPU.
     * The upside is that we don't have to worry about managing state changes between threads.
     * <p/>
     * If there was a pending frame available notification when we shut down, we might get
     * here after onPause().
     */




    public void initFilter(FilterManager.FilterType filterType) {
        mCurrentFilterType = filterType;
    }

    private void prefareFilterList() {
        LVItemFilter rvItemFilter = new LVItemFilter();
        rvItemFilterList.add(rvItemFilter);

        rvItemFilter = new LVItemFilter(FilterManager.FilterType.Normal, "Normal");
        rvItemFilterList.add(rvItemFilter);

        rvItemFilter = new LVItemFilter(FilterManager.FilterType.Blend, "Blend");
        rvItemFilterList.add(rvItemFilter);

        rvItemFilter = new LVItemFilter(FilterManager.FilterType.SoftLight, "SoftLight");
        rvItemFilterList.add(rvItemFilter);

        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve0, "ToneCurve0");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve1, "ToneCurve1");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve2, "ToneCurve2");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve3, "ToneCurve3");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve4, "ToneCurve4");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve5, "ToneCurve5");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve6, "ToneCurve6");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve7, "ToneCurve7");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve8, "ToneCurve8");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve9, "ToneCurve9");
        rvItemFilterList.add(rvItemFilter);


        rvItemFilter = new LVItemFilter(FilterManager.FilterType.ToneCurve10, "ToneCurve10");
        rvItemFilterList.add(rvItemFilter);

        rvItemFilter = new LVItemFilter(FilterManager.FilterType.Jack, "Jack");
        rvItemFilterList.add(rvItemFilter);

        filterBaseAdapter.notifyDataSetChanged();
    }

    private void initUI(){

         btRecord = (Button) findViewById(R.id.btRecord);
         btFilter = (Button) findViewById(R.id.btFilter);
         loRecordBtnEtc = (LinearLayout) findViewById(R.id.loRecordBtnEtc);
         lvFilter = (ListView) findViewById(R.id.lvFilter);
         filterBaseAdapter = new FilterBaseAdapter(rvItemFilterList, mContext);
         lvFilter.setAdapter(filterBaseAdapter);
         prefareFilterList();
//        progressView = (ProgressView) findViewById(R.id.recorder_progress);
//        progressView.setTotalTime((float)recordingTime);

         SurfaceView surfaceView = (SurfaceView) findViewById(R.id.continuousCapture_CameraSurfaceView);
         SurfaceHolder surfaceHolder = surfaceView.getHolder();
         surfaceHolder.addCallback(this);

         btFlash = (Button) findViewById(R.id.btFlash);
         btChangeCamera = (Button) findViewById(R.id.btChangeCamera);


     }

    private void  initOnClickListeners(){

        lvFilter.setOnItemClickListener(this);


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LVItemFilter itemFilter =  filterBaseAdapter.getItem(position);
        mNewFilterType = itemFilter.getFilterType();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
        case    R.id.continuousCapture_CameraSurfaceView:

            break;
        }
    }
}






//    boolean _isPressingRecordBtn = false;
//    ProgressItem progressItem;

//    public void updateProgressBarJack(boolean isPressingRecordBtn) {
//        if (isPressingRecordBtn) {
//
//            if (!_isPressingRecordBtn) {
//                Log.d(TAG, "updateProgressBarJack() Create ");
//
//                LayoutInflater inflater = LayoutInflater.from(mContext);
//                progressItem = (ProgressItem) inflater.inflate(R.layout.i_timebar, null, false);
//                progressItem.setTag(Grab.longGrabBeginTimeOfBlock);
//                currentProgressItem = progressItem;
//                loRecordBtnEtc.addView(progressItem);
//                _isPressingRecordBtn = isPressingRecordBtn;
//            } else {
//                Log.d(TAG, "updateProgressBarJack() Add Width ");
//
//                currentProgressItem.getLayoutParams().height += intMoreWidthOfProgressBarPerFrame;
//
//                Log.d(TAG, "intMoreWidthOfProgressBarPerFrame " + String.valueOf(intMoreWidthOfProgressBarPerFrame));
//                Log.d(TAG, "progressItem.getLayoutParams().height " + String.valueOf(progressItem.getLayoutParams().height));
//            }
//        } else {
//            if (_isPressingRecordBtn) {
//                _isPressingRecordBtn = false;
//            }
//        }
//    }

//    ProgressItem currentProgressItem;
//    int intMoreWidthOfProgressBarPerFrame;

//    private int calcIntMoreWidthOfProgressBarPerFrame() {
//        DisplayMetrics displaymetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
//        int height = displaymetrics.heightPixels;
//        int width = displaymetrics.widthPixels;
//        Log.d(TAG, "height : " + String.valueOf(height));
//        return ((height - 60) / (DESIRED_PREVIEW_FPS * intTotalVideoLengthSeconds * 2));
//    }


//    /**
//     * Adds a bit of extra stuff to the display just to give it flavor.
//     */
//    private static void drawExtra(int frameNum, int width, int height) {
//        // We "draw" with the scissor rect and clear calls.  Note this uses window coordinates.
//        int val = frameNum % 3;
//        switch (val) {
//            case 0:  GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);   break;
//            case 1:  GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);   break;
//            case 2:  GLES20.glClearColor(0.0f, 0.0f, 1.0f, 1.0f);   break;
//        }
//
//        int xpos = (int) (width * ((frameNum % 100) / 100.0f));
//        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
//        GLES20.glScissor(xpos, 0, width / 32, height / 32);
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
//    }


/**
 * Handles onClick for "capture" button.
 */
//    public void clickCapture(@SuppressWarnings("unused") View unused) {
//        Log.d(TAG, "capture");
//        if (mFileSaveInProgress) {
//            Log.w(TAG, "HEY: file save is already in progress");
//            return;
//        }
//
//        // The button is disabled in onCreate(), and not enabled until the encoder and output
//        // surface is ready, so it shouldn't be possible to get here with a null mCircEncoder.
//        mFileSaveInProgress = true;
//        updateControls();
//        /**************
//         * NotifySaving
//         */
//
//
//        mCircEncoder.saveVideo(mOutputFile);
//    }


//    public void setCameraPreviewSize(int width, int height) {
//        Log.d(TAG, "setCameraPreviewSize(int width, int height) " + String.valueOf(width) + "," + String.valueOf(height));
//        mIncomingWidth = width;
//        mIncomingHeight = height;
//
//        float scaleHeight = mSurfaceWidth / (width * 1f / height * 1f);
//        float surfaceHeight = mSurfaceHeight;
//
//        if (fullFrameRect != null) {
//            mMvpScaleX = 1f;
//            mMvpScaleY = scaleHeight / surfaceHeight;
//            fullFrameRect.scaleMVPMatrix(mMvpScaleX, mMvpScaleY);
//        }
//    }
