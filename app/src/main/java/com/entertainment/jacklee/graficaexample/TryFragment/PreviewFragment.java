package com.entertainment.jacklee.graficaexample.TryFragment;

/**
 * Created by Jacklee on 16. 4. 16..
 */
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.entertainment.jacklee.graficaexample.R;
import com.sprylab.android.widget.TextureVideoView;

import java.io.File;

public class PreviewFragment extends  Fragment implements View.OnClickListener {
    private static final String TAG = "PreviewFragment";
    private View view;
    Button ibBack;
    private TextureVideoView ttv;
    private boolean isVideoViewPrepared = false;
//    private int page;


    @NonNull
    private BroadcastReceiver mVideoSavedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            // Get extra data included in the Intent
            Log.d(TAG, "onReceive");
            boolean IS_PROFILE_UPDATED = intent.getBooleanExtra("mVideoSavedBR", false);

            if (JackActivity.isVideoSaved==false){
                JackActivity.isVideoSaved = true;
                if(isVideoViewPrepared){
                    startVideoPlayback();
                }
            }
        }
    };


    // newInstance constructor for creating fragment with arguments
    public static PreviewFragment newInstance(int page, String title) {
        PreviewFragment previewFragment = new PreviewFragment();
        Bundle args = new Bundle();
        args.putInt("someInt", page);
        previewFragment.setArguments(args);
        return previewFragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ...");
//        page = getArguments().getInt("someInt", 1);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView ...");
         view = inflater.inflate(R.layout.fragment_preview, container, false);
        initUI();
        return view;    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mVideoSavedBR,
                new IntentFilter("mVideoSavedBR"));
        ibBack.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");

    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        initVideoView();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mVideoSavedBR);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    private void initUI() {
        ttv = (TextureVideoView) view. findViewById(R.id.ttv);
        ibBack = (Button) view. findViewById(R.id.ibBack);
//        roEmpty = (RelativeLayout)view.findViewById(R.id.roEmpty);
//        lv_alr = (ListView) view.findViewById(R.id.lv_alr);
//        lv_alr.setLongClickable(true);
    }


    public void initVideoView() {
        Log.d(TAG, "initVideoView()");
        ttv.setVideoPath(getVideoPath());
        ttv.setMediaController(null);
        ttv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(final MediaPlayer mp) {
                isVideoViewPrepared  = true;
                mp.setLooping(true);
                if (JackActivity.isVideoSaved) {
                    Log.d(TAG, "JackActivity.isVideoSaved");



                    startVideoPlayback();
                }
//                startVideoAnimation();
            }
        });
    }

    public void startVideoPlayback() {
        // "forces" anti-aliasing - but increases time for taking frames - so keep it disabled
        // ttv.setScaleX(1.00001f);
        Log.d(TAG, "startVideoPlayback()");



        ttv.start();
    }

    private String getVideoPath() {
        return (new File(getActivity().getFilesDir(), "131313.mp4")).getAbsolutePath() ;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ibBack:
                ((JackActivity)getActivity()).backToFragment(new CameraFragment());
                break;
        }
    }
}
