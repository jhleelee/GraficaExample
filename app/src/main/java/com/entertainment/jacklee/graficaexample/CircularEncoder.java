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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.entertainment.jacklee.graficaexample.TryFragment.JStampAbsolute;
import com.entertainment.jacklee.graficaexample.Widget.Constant;
import com.entertainment.jacklee.graficaexample.Widget.Grab;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Encodes video in a fixed-size circular buffer.
 * <p/>
 * The obvious way to do this would be to store each packet in its own buffer and hook it
 * into a linked list.  The trouble with this approach is that it requires constant
 * allocation, which means we'll be driving the GC to distraction as the frame rate and
 * bit rate increase.  Instead we create fixed-size pools for video data and metadata,
 * which requires a bit more work for us but avoids allocations in the steady state.
 * <p/>
 * Video must always start with a sync frame (a/k/a key frame, a/k/a I-frame).  When the
 * circular buffer wraps around, we either need to delete all of the data between the frame at
 * the head of the list and the next sync frame, or have the file save function know that
 * it needs to scan forward for a sync frame before it can start saving data.
 * <p/>
 * When we're told to save a snapshot, we create a MediaMuxer, write all the frames out,
 * and then go back to what we were doing.
 */
public class CircularEncoder {
    private static final String TAG = "CircularEncoder";
    private static final boolean VERBOSE = true;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // sync frame every second

    private EncoderThread mEncoderThread;
    private Surface surfaceInput;
    private MediaCodec mediaCodec;

    /**
     * Callback function definitions.  CircularEncoder caller must provide one.
     */
    public interface Callback {
        /**
         * Called some time after saveVideo(), when all data has been written to the
         * output file.
         *
         * @param status Zero means success, nonzero indicates failure.
         */
        void fileSaveComplete(int status);

        /**
         * Called occasionally.
         *
         * @param totalTimeMsec Total length, in milliseconds, of buffered video.
         */
        void bufferStatus(long totalTimeMsec);
    }

    /**
     * Configures encoder, and prepares the input Surface.
     *
     * @param width          Width of encoded video, in pixels.  Should be a multiple of 16.
     * @param height         Height of encoded video, in pixels.  Usually a multiple of 16 (1080 is ok).
     * @param bitRate        Target bit rate, in bits.
     * @param frameRate      Expected frame rate.
     * @param desiredSpanSec How many seconds of video we want to have in our buffer at any time.
     */
    public CircularEncoder(int width, int height, int bitRate, int frameRate, int desiredSpanSec,
                           Callback cb) throws IOException {
        // The goal is to size the buffer so that we can accumulate N seconds worth of video,
        // where N is passed in as "desiredSpanSec".  If the codec generates data at roughly
        // the requested bit rate, we can compute it as time * bitRate / bitsPerByte.
        //
        // Sync frames will appear every (frameRate * IFRAME_INTERVAL) frames.  If the frame
        // rate is higher or lower than expected, various calculations may not work out right.
        //
        // Since we have to start muxing from a sync frame, we want to ensure that there's
        // room for at least one full GOP in the buffer, preferrably two.
        if (desiredSpanSec < IFRAME_INTERVAL * 2) {
            throw new RuntimeException("Requested time span is too short: " + desiredSpanSec +
                    " vs. " + (IFRAME_INTERVAL * 2));
        }
//        CircularEncoderBuffer encBuffer = new CircularEncoderBuffer(bitRate, frameRate,
//                desiredSpanSec);
        if (EncoderBufferHolder.encBuffer==null) {
            EncoderBufferHolder.encBuffer = new CircularEncoderBuffer(bitRate, frameRate,
                    desiredSpanSec);
        }
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surfaceInput = mediaCodec.createInputSurface();
        mediaCodec.start();

        // Start the encoder thread last.  That way we're sure it can see all of the state
        // we've initialized.
        mEncoderThread = new EncoderThread(mediaCodec,   cb);
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return surfaceInput;
    }

    /**
     * Shuts down the encoder thread, and releases encoder resources.
     * <p/>
     * Does not return until the encoder thread has stopped.
     */
    public void shutdown() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");

        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN));
        try {
            mEncoderThread.join();
        } catch (InterruptedException ie) {
            Log.w(TAG, "Encoder thread join() was interrupted", ie);
        }

        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    /**
     * Notifies the encoder thread that a new frame will shortly be provided to the encoder.
     * <p/>
     * There may or may not yet be data available from the encoder output.  The encoder
     * has a fair mount of latency due to processing, and it may want to accumulate a
     * few additional buffers before producing output.  We just need to drain it regularly
     * to avoid a situation where the producer gets wedged up because there's no room for
     * additional frames.
     * <p/>
     * If the caller sends the frame and then notifies us, it could get wedged up.  If it
     * notifies us first and then sends the frame, we guarantee that the output buffers
     * were emptied, and it will be impossible for a single additional frame to block
     * indefinitely.
     */
    public void frameAvailableSoon() {
        Log.d(TAG, "frameAvailableSoon() - 1");

        if (Grab.arlGrabbedBbEncodedData.size() <= (Constant.DESIRED_PREVIEW_FPS * Constant.intMaxRecordSec)) {
            Handler handler = mEncoderThread.getHandler();
            handler.sendMessage(handler.obtainMessage(
                    EncoderThread.EncoderHandler.MSG_FRAME_AVAILABLE_SOON));
        }


    }

    /**
     * Initiates saving the currently-buffered frames to the specified output file.  The
     * data will be written as a .mp4 file.  The call returns immediately.  When the file
     * save completes, the callback will be notified.
     * <p/>
     * The file generation is performed on the encoder thread, which means we won't be
     * draining the output buffers while this runs.  It would be wise to stop submitting
     * frames during this time.
     */
    public void saveVideo(File outputFile) {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_SAVE_VIDEO, outputFile));
    }


    public EncoderThread getEncoderThread() {
        return mEncoderThread;
    }





    public int getEncoderThreadCircularEncoderBufferIndex() {
        return mEncoderThread.getCircularEncoderBufferIndex();
    }


    public ByteBuffer getEncoderThreadCircularEncoderByteBufferWithIndex() {
        Log.d(TAG, "getEncoderThreadCircularEncoderByteBufferWithIndex()");
        return mEncoderThread.getCircularEncoderByteBufferWithIndex(
                getEncoderThreadCircularEncoderBufferIndex()

        );
    }

    /**
     * Object that encapsulates the encoder thread.
     * <p/>
     * We want to sleep until there's work to do.  We don't actually know when a new frame
     * arrives at the encoder, because the other thread is sending frames directly to the
     * input surface.  We will see data appear at the decoder output, so we can either use
     * an infinite timeout on dequeueOutputBuffer() or wait() on an object and require the
     * calling app wake us.  It's very useful to have all of the buffer management local to
     * this thread -- avoids synchronization -- so we want to do the file muxing in here.
     * So, it's best to sleep on an object and do something appropriate when awakened.
     * <p/>
     * This class does not manage the MediaCodec encoder startup/shutdown.  The encoder
     * should be fully started before the thread is created, and not shut down until this
     * thread has been joined.
     */
    private static class EncoderThread extends Thread {

        private MediaCodec mediaCodec;
        private MediaCodec.BufferInfo mediaCodecBufferInfo;
        private MediaFormat videoFormat;
        private MediaFormat audioFormat;

        private EncoderHandler encoderHandler;
        private Callback mCallback;

        private volatile boolean isReady = false;
        private int mFrameNum;
        private final Object mLock = new Object();



        public EncoderThread(
                MediaCodec mediaCodec,

                Callback callback) {
            this.mediaCodec = mediaCodec;

            mCallback = callback;
            mediaCodecBufferInfo = new MediaCodec.BufferInfo();
        }

        /**
         * Thread entry point.
         * <p/>
         * Prepares the Looper, Handler, and signals anybody watching that we're ready to go.
         */
        @Override
        public void run() {
            Looper.prepare();
            encoderHandler = new EncoderHandler(this);    // must create on encoder thread
            Log.d(TAG, "encoder thread ready");
            synchronized (mLock) {
                isReady = true;
                mLock.notify();    // signal waitUntilReady()
            }

            Looper.loop();

            synchronized (mLock) {
                isReady = false;
                encoderHandler = null;
            }
            Log.d(TAG, "looper quit");
        }

        /**
         * Waits until the encoder thread is ready to receive messages.
         * <p/>
         * Call from non-encoder thread.
         */
        public void waitUntilReady() {
            synchronized (mLock) {
                while (!isReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Returns the Handler used to send messages to the encoder thread.
         */
        public EncoderHandler getHandler() {
            synchronized (mLock) {
                // Confirm ready state.
                if (!isReady) {
                    throw new RuntimeException("not ready");
                }
            }
            return encoderHandler;
        }

        /**
         * Drains all pending output from the decoder, and adds it to the circular buffer.
         */
        public void drainEncoder() {
            final int TIMEOUT_USEC = 0;     // no timeout -- check for buffers, bail if none

            /**
             * Retrieve the set of output buffers.  Call this after start()
             * returns and whenever dequeueOutputBuffer signals an output
             * buffer change by returning {@link
             * #INFO_OUTPUT_BUFFERS_CHANGED}. After calling this method, any
             * ByteBuffers previously returned by an earlier call to this
             * method MUST no longer be used.
             *
             * @deprecated Use the new {@link #getOutputBuffer} method instead
             * each time an output buffer is dequeued.  This method is not
             * supported if codec is configured in asynchronous mode.
             *
             * <b>Note:</b> As of API 21, the position and limit of output
             * buffers that are dequeued will be set to the valid data
             * range.
             *
             * <em>Do not use this method if using an output surface.</em>
             *
             * @throws IllegalStateException if not in the Executing state,
             *         or codec is configured in asynchronous mode.
             * @throws MediaCodec.CodecException upon codec error.
             */
            ByteBuffer[] bbArEncodedData = mediaCodec.getOutputBuffers();

            while (true) {
                int encoderStatus = mediaCodec.dequeueOutputBuffer(
                        mediaCodecBufferInfo,
                        TIMEOUT_USEC);

                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    break;

                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    bbArEncodedData = mediaCodec.getOutputBuffers();

                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Should happen before receiving buffers, and should only happen once.
                    // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                    // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                    // rather than extract the codec-specific data and reconstruct a new
                    // MediaFormat later, we just grab it here and keep it around.
                    videoFormat = mediaCodec.getOutputFormat();
                    Log.d(TAG, "encoder output format changed: " + videoFormat);

                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer bbEncodedData = bbArEncodedData[encoderStatus]; //encoderStatus : 0 or 1 or 2 or 3

                    if (bbEncodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    if ((mediaCodecBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // The codec config data was pulled out when we got the
                        // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                        // a single big blob -- it wants separate csd-0/csd-1 chunks --
                        // so simply saving this off won't work.
                        if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        mediaCodecBufferInfo.size = 0;
                    }

                    if (mediaCodecBufferInfo.size != 0) {
                        Log.d(TAG, "mediaCodecBufferInfo.offset, mediaCodecBufferInfo.size " + String.valueOf(mediaCodecBufferInfo.offset) + "," + String.valueOf(mediaCodecBufferInfo.size));
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)

                        // The start-offset of the data in the buffer.
                        bbEncodedData.position(mediaCodecBufferInfo.offset);
                        // mediaCodecBufferInfo.offset : 0

                        //Sets the limit of this buffer.
                        bbEncodedData.limit(mediaCodecBufferInfo.offset + mediaCodecBufferInfo.size);
                        // mediaCodecBufferInfo.offset : 0 ; mediaCodecBufferInfo.size 54243, 66419, 87404, etc random

                        /**************************************************************************************************
                         * JHE - GRAP bbEncodedData
                         *************************************************************************************************/

//                        /**
//                         * Adds a new encoded data packet to the buffer.
//                         * Adds a new encoded data packet to the buffer.
//                         */
                        EncoderBufferHolder.encBuffer.add(
                                bbEncodedData, //The data.
                                mediaCodecBufferInfo.flags, // MediaCodec.BufferInfo flags.
                                JStampAbsolute.getTimeStamp()//long : Presentation time stamp, in microseconds.  JTimeStamp.getRelativeStamp(mediaCodecBufferInfo.presentationTimeUs)
                        );

                        if (VERBOSE) {
                            Log.d(TAG, "sent " + mediaCodecBufferInfo.size + " bytes to muxer, ts=" + ""
                            );
                        }
                    }

                    /**
                     * If you are done with a buffer, use this call to return the buffer to the codec
                     * or to render it on the output surface. If you configured the codec with an
                     * output surface, setting {@code render} to {@code true} will first send the buffer
                     * to that output surface. The surface will release the buffer back to the codec once
                     * it is no longer used/displayed.
                     *
                     * Once an output buffer is released to the codec, it MUST NOT
                     * be used until it is later retrieved by {@link #getOutputBuffer} in response
                     * to a {@link #dequeueOutputBuffer} return value or a
                     * {@link Callback#onOutputBufferAvailable} callback.
                     *
                     * @param index The index of a client-owned output buffer previously returned
                     *              from a call to {@link #dequeueOutputBuffer}.
                     * @param render If a valid surface was specified when configuring the codec,
                     *               passing true renders this output buffer to the surface.
                     * @throws IllegalStateException if not in the Executing state.
                     * @throws MediaCodec.CodecException upon codec error.
                     */
                    mediaCodec.releaseOutputBuffer(encoderStatus, false);

                    if ((mediaCodecBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                        break;      // out of while
                    }
                }
            }
        }

        /**
         * Drains the encoder output.
         * <p/>
         * See notes for {@link CircularEncoder#frameAvailableSoon()}.
         */
        void frameAvailableSoon() {
            Log.d(TAG, "frameAvailableSoon() - 2");
            if (VERBOSE) Log.d(TAG, "frameAvailableSoon() - drainEncoder()");
            drainEncoder();

            mFrameNum++;
            if ((mFrameNum % 10) == 0) {        // TODO: should base off frame rate or clock?
                mCallback.bufferStatus(EncoderBufferHolder.encBuffer.computeTimeSpanUsec());
            }
        }

        /**
         * Saves the encoder output to a .mp4 file.
         * <p/>
         * We'll drain the encoder to get any lingering data, but we're not going to shut
         * the encoder down or use other tricks to try to "flush" the encoder.  This may
         * mean we miss the last couple of submitted frames if they're still working their
         * way through.
         * <p/>
         * We may want to reset the buffer after this -- if they hit "capture" again right
         * away they'll end up saving video with a gap where we paused to write the file.
         */
        int circularEncoderBufferIndex = 0;

        public int getCircularEncoderBufferIndex() {
            return circularEncoderBufferIndex;
        }

        public ByteBuffer getCircularEncoderByteBufferWithIndex(int circularEncoderBufferIndex) {
            Log.d(TAG, "getCircularEncoderByteBufferWithIndex(int circularEncoderBufferIndex) " + String.valueOf(circularEncoderBufferIndex));
            return EncoderBufferHolder.encBuffer.getChunk(circularEncoderBufferIndex, new MediaCodec.BufferInfo());
        }


        void saveVideo(File outputFile) {
            if (VERBOSE) Log.d(TAG, "saveVideo " + outputFile);

            circularEncoderBufferIndex = EncoderBufferHolder.encBuffer.getFirstIndex();
            Log.d(TAG, "int index = circularEncoderBuffer.getFirstIndex() index:" + String.valueOf(circularEncoderBufferIndex));
            if (circularEncoderBufferIndex < 0) {
                Log.w(TAG, "Unable to get first index");
                mCallback.fileSaveComplete(1);
                return;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            Log.d(TAG, "bufferInfo :\n" +
                    "bufferInfo.flags : " + bufferInfo.flags + "\n" + // 0
                    "bufferInfo.presentationTimeUs : " + bufferInfo.presentationTimeUs + "\n" + // 0
                    "bufferInfo.offset : " + bufferInfo.offset + "\n" + // 0
                    "bufferInfo.size : " + bufferInfo.size // 0
            );

            MediaMuxer muxer = null;
            int result = -1;
            try {
                muxer = new MediaMuxer(
                        outputFile.getPath(),
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                );

                /**
                 * Adds a track with the specified format.
                 * @param format The media format for the track.  This must not be an empty
                 *               MediaFormat.
                 * @return The track index for this newly added track, and it should be used
                 * in the {@link #writeSampleData}.
                 */
                int videoTrack = muxer.addTrack(videoFormat);
                muxer.start();


                do {
                    ByteBuffer buf = EncoderBufferHolder.encBuffer
                            .getChunk(circularEncoderBufferIndex, bufferInfo);
                    if (VERBOSE) {
                        Log.d(TAG, "SAVE " + circularEncoderBufferIndex + " flags=0x" + Integer.toHexString(bufferInfo.flags));
                    }

                    muxer.writeSampleData(videoTrack, buf, bufferInfo);

                    circularEncoderBufferIndex = EncoderBufferHolder.encBuffer
                            .getNextIndex(circularEncoderBufferIndex);
                } while (circularEncoderBufferIndex >= 0);
                result = 0;
            } catch (IOException ioe) {
                Log.w(TAG, "muxer failed", ioe);
                result = 2;
            } finally {
                if (muxer != null) {
                    muxer.stop(); //an't stop due to wrong state.
                    muxer.release();
                }
            }

            if (VERBOSE) {
                Log.d(TAG, "muxer stopped, result=" + result);
            }
            mCallback.fileSaveComplete(result);
        }

        /**
         * Tells the Looper to quit.
         */
        void shutdown() {
            if (VERBOSE) Log.d(TAG, "shutdown");
            Looper.myLooper().quit();
        }

        /**
         * Handler for EncoderThread.  Used for messages sent from the UI thread (or whatever
         * is driving the encoder) to the encoder thread.
         * <p/>
         * The object is created on the encoder thread.
         */
        private static class EncoderHandler extends Handler {
            public static final int MSG_FRAME_AVAILABLE_SOON = 1;
            public static final int MSG_SAVE_VIDEO = 2;
            public static final int MSG_SHUTDOWN = 3;

            // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
            // but no real harm in it.
            private WeakReference<EncoderThread> mWeakEncoderThread;

            /**
             * Constructor.  Instantiate object from encoder thread.
             */
            public EncoderHandler(EncoderThread encoderThread) {
                mWeakEncoderThread = new WeakReference<EncoderThread>(encoderThread);
            }

            @Override  // runs on encoder thread
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (VERBOSE) {
                    Log.v(TAG, "EncoderHandler: what=" + what);
                }

                EncoderThread encoderThread = mWeakEncoderThread.get();
                if (encoderThread == null) {
                    Log.w(TAG, "EncoderHandler.handleMessage: weak ref is null");
                    return;
                }

                switch (what) {
                    case MSG_FRAME_AVAILABLE_SOON:
                        encoderThread.frameAvailableSoon();
                        break;
                    case MSG_SAVE_VIDEO:
                        encoderThread.saveVideo((File) msg.obj);
                        break;
                    case MSG_SHUTDOWN:
                        encoderThread.shutdown();
                        break;
                    default:
                        throw new RuntimeException("unknown message " + what);
                }
            }
        }



        public CircularEncoderBuffer getCircularEncoderBuffer(){
            return EncoderBufferHolder.encBuffer;
        }
    }
}
