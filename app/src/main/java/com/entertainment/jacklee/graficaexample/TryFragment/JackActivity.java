package com.entertainment.jacklee.graficaexample.TryFragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;

import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.entertainment.jacklee.graficaexample.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Jacklee on 16. 4. 16..
 */
public class JackActivity extends AppCompatActivity {

    // Base
    private static final String TAG = "ContiCaptureActivity";
    Context mContext;
    private PowerManager.WakeLock mWakeLock;

    // Screen
    int screenWidth, screenHeight = 0;

    // Record
    private File mOutputFile;
    private float floatSecondsOfVideo;
    public static boolean isVideoSaved = false;

//    private VerticalViewPager mPager;
//    public List<Fragment> fragments = new ArrayList<>();

    //PERMISSIONS
    final int REQUEST_PERSMISSIONS = 81;


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult()" + Arrays.toString(permissions));

        switch (requestCode) {

            case REQUEST_PERSMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!

                    android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    CameraFragment cameraFragment = new CameraFragment();
                    transaction.replace(R.id.fragment_container, cameraFragment, "CameraFragment");
                    transaction.addToBackStack(null);
                    transaction.commitAllowingStateLoss();
                } else {

                    // show dialog to finish

                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // Turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        // Fullscreen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_jack);

        mContext = this;

        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {

        }

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

        Log.d(TAG, String.valueOf(Build.VERSION.SDK_INT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            /**************************
             (0) Check Permission stat
             ***************************/
            ArrayList<String> arlPermissiionsToGet =new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                    arlPermissiionsToGet.add(Manifest.permission.CAMERA);
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                arlPermissiionsToGet.add(Manifest.permission.RECORD_AUDIO);
            }

            if (arlPermissiionsToGet.size()>0){

                Log.d(TAG, "arlPermissiionsToGet.size() : "+String.valueOf(arlPermissiionsToGet.size()));

                /*********************
                (1) Need Explanations?
                **********************/
                ArrayList<String> arlExplanations =new ArrayList<>();

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    arlExplanations.add("explain why need camera");
                }

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
                    arlExplanations.add("explain why need audiorecord");
                }

                if (arlExplanations.size()>0){
                    //show dialog for explanation
                }

                /************************
                 (2) Request Permissions
                *************************/
                ActivityCompat.requestPermissions(this,
                        arlPermissiionsToGet.toArray(new String[arlPermissiionsToGet.size()]),
                        REQUEST_PERSMISSIONS);

            } else {

                // All Persmissions Are OK

                android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                CameraFragment cameraFragment = new CameraFragment();
                transaction.replace(R.id.fragment_container, cameraFragment, "CameraFragment");
                transaction.addToBackStack(null);
                transaction.commit();
            }
        } else {
            android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            CameraFragment cameraFragment = new CameraFragment();
            transaction.replace(R.id.fragment_container, cameraFragment, "CameraFragment");
            transaction.addToBackStack(null);
            transaction.commit();
        }





//        fragments = new ArrayList<>();
//        fragments.add(CameraFragment.newInstance(0, "0"));
//        fragments.add(PreviewFragment.newInstance(1, "1"));
//
//        mPager = (VerticalViewPager) findViewById(R.id.pager);
//        final FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
//            @Override
//            public CharSequence getPageTitle(int position) {
//                return String.valueOf(fragments.get(position));
//            }
//
//            @Override
//            public Fragment getItem(int position) {
//                return fragments.get(position);
//            }
//
//            @Override
//            public int getCount() {
//                return fragments.size();
//            }
//        };
//        mPager.setAdapter(adapter);
//        mPager.setOffscreenPageLimit(1);
//        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//
//
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//            /*
//            This method will be invoked when the current page is scrolled,
//            either as part of a programmatically initiated smooth scroll or
//            a user initiated touch scroll.
//             */
//                Log.d(TAG, "onPageScrolled(int position, float positionOffset, int positionOffsetPixels) : "
//                        + String.valueOf(position) + ", " + String.valueOf(positionOffset) + ", " + String.valueOf(positionOffsetPixels));
//
//
//                if (position==1&&positionOffsetPixels==0) {
//                    Log.d(TAG, "((PreviewFragment) adapter.getItem(position)).initVideoView()");
//                    ((PreviewFragment) adapter.getItem(position)).initVideoView();
//                }
//
//            }
//
//            @Override
//            public void onPageSelected(int position) {
//                Log.d(TAG, "onPageSelected(int position) : " + String.valueOf(position));
//
//
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//                /*
//                Called when the scroll state changes.
//                Useful for discovering when the user begins dragging, when the pager
//                is automatically settling to the current page, or when it is
//                fully stopped/idle.
//                 */
//
//
//                Log.d(TAG, "onPageScrollStateChanged(int state) : " + String.valueOf(state));
//
//                switch (state) {
//                    case ViewPager.SCROLL_STATE_DRAGGING:
//                    case ViewPager.SCROLL_STATE_SETTLING:
//                        Log.d(TAG, "SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING");
//                        if (((CameraFragment) adapter.getItem(0)).ivCover != null) {
//                            Log.d(TAG, "((CameraFragment) adapter.getItem(0)).ivCover != null");
//
//                            if (((CameraFragment) adapter.getItem(0)).ivCover.getVisibility() != View.VISIBLE) {
//                                Log.d(TAG, "((CameraFragment) adapter.getItem(0)).ivCover.getVisibility() != View.VISIBLE");
//                                ((CameraFragment) adapter.getItem(0)).ivCover.setVisibility(View.VISIBLE);
//
//                            }
//
//                        } else {
//                            Log.d(TAG, "ivCover is null");
//                        }
//                        break;
//
//                    case ViewPager.SCROLL_STATE_IDLE:
//                        Log.d(TAG, "SCROLL_STATE_IDLE");
//
//                        if (((CameraFragment) adapter.getItem(0)).ivCover != null) {
//                            ((CameraFragment) adapter.getItem(0)).ivCover.setVisibility(View.GONE);
//                        }
//
//
//
//                        break;
//                }
//
//
//            }
//        });
    }


    /**
     * PagerAdapter
     */
//    private class MyFragmentPagerAdapter extends SmartFragmentStatePagerAdapter {
//        int intNumberOfItems = 2;
//
//        public MyFragmentPagerAdapter(FragmentManager fm) {
//            super(fm);
//        }
//
//        @Override
//        public void startUpdate(ViewGroup container) {
//            super.startUpdate(container);
//        }
//
//        @Override
//        public Object instantiateItem(ViewGroup container, int position) {
//            return super.instantiateItem(container, position);
//        }
//
//        @Override
//        public void destroyItem(ViewGroup container, int position, Object object) {
//            super.destroyItem(container, position, object);
//        }
//
//        @Override
//        public void setPrimaryItem(ViewGroup container, int position, Object object) {
//            super.setPrimaryItem(container, position, object);
//        }
//
//        @Override
//        public void finishUpdate(ViewGroup container) {
//            super.finishUpdate(container);
//        }
//
//        @Override
//        public boolean isViewFromObject(View view, Object object) {
//            return super.isViewFromObject(view, object);
//        }
//
//        @Override
//        public Parcelable saveState() {
//            return super.saveState();
//        }
//
//        @Override
//        public void restoreState(Parcelable state, ClassLoader loader) {
//            super.restoreState(state, loader);
//        }
//
//
//        @Override
//        public Fragment getItem(int position) {
//            return fragments.get(position);
////            switch (position) {
////                case 0:
////
////                    return CameraFragment.newInstance(0, "Page # 1");
////
////                case 1:
////
////                    return PreviewFragment.newInstance(1, "Page # 2");
////
////                default:
////                    return null;
////            }
//
//        }
//
//        @Override
//        public int getCount() {
//            return intNumberOfItems;
//        }
//    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }




    public void goToFragment(Fragment fragment) {
        //Fragment
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enterfromright, R.anim.exittoleft, R.anim.pop_enter, R.anim.pop_exit);
        transaction.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
//        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void backToFragment(Fragment fragment) {
        //Fragment
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enterfromleft, R.anim.exittoright, R.anim.pop_enter, R.anim.pop_exit);
        transaction.replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName());
//        transaction.addToBackStack(null);
        transaction.commit();
    }


    public static void sendVideoSavedBR() {
        Log.d(TAG, "sendVideoSavedBR");
        Intent intent = new Intent("mVideoSavedBR");
        LocalBroadcastManager.getInstance(App.getContext()).sendBroadcast(intent);
    }


}
