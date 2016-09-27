package com.entertainment.jacklee.graficaexample.Widget;

import android.media.MediaRecorder;

/**
 * Created by Jacklee on 16. 4. 2..
 */
public class Constant {

    public static boolean mFileSaveInProgress = false;
    public static int DESIRED_PREVIEW_FPS =15;
    public static int intMaxRecordSec = 15;

    public static int SCREEN_WIDTH = 0;
    public static int SCREEN_HEIGHT = 0;
    public  static   int VIDEO_WIDTH = 1280;  // dimensions for 720p video
    public  static   int VIDEO_HEIGHT = 720;


    public  static boolean isEqualizer = false;

    public static boolean isNoiseSuppressed = false;
    public static int audiorecordsource = MediaRecorder.AudioSource.MIC;

    //MediaRecorder.AudioSource.VOICE_RECOGNITION,
    //NoiseSuppressor(API16)
    //1.            am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
    //2.            am.setParameters("noise_suppression=auto");
                    /*
                    For my app in particular I needed to record speech WITHOUT all the distracting background fuzz. Using VOICE_RECOGNITION will pass the audio data into the phone's noise cancellation filters before handing it over to you.

So unless you're comfortable with implementing your own noise cancelling algorithm, it's much easier to just let the phone's built-in hardware handle it.

Another way to reduce noise is by using the AudioEffect NoiseSuppressor class available with Jelly Bean (Android 4.1 API level 16). I haven't looked into this very much as I still need to support Android 2.3.

The last method of noise cancellation is to use AudioManager.setParameters() which is available in API level 5+.

1.
am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
2.
am.setParameters("noise_suppression=auto");
I'm uncertain about effective this is as there's little to no feedback on it's success status or whether manufacturers have even implemented it at all.
                     */


    public static class AudioSpeed {

        public  static int iFreq = 8000;
        public   static int iSeekbarPoint = 50;
        public  static float fPlaySpeed = 1.0f;

        public static void withFreq(int freq) {


        }


        public static void withSeekbarPoint() {

            if (iSeekbarPoint==50){
                fPlaySpeed = 1.0f;
                iFreq = 8000;

            } else if (iSeekbarPoint>50){
                fPlaySpeed = (float) (iSeekbarPoint  *0.02) ;
                iFreq = Math.round(8000 * fPlaySpeed);

            } else {
                fPlaySpeed = (float)0.5 + (float)(iSeekbarPoint *0.01);
                iFreq = Math.round(8000 * fPlaySpeed);

            }

        }

        public static String getPlaySpeedText() {
            return String.format(java.util.Locale.US,"%.2f",fPlaySpeed);
        }




    }


}
