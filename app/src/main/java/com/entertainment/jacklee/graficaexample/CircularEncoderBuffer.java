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
import android.util.Log;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Holds encoded video data in a circular buffer.
 * <p/>
 * This is actually a pair of circular buffers, one for the raw data and one for the meta-data
 * (flags and PTS).
 * <p/>
 * Not thread-safe.
 */
public class CircularEncoderBuffer {

    /* LogCat
    D/CircularEncoder: frameAvailableSoon() - 1
    V/CircularEncoder: EncoderHandler: what=1
    D/CircularEncoder: frameAvailableSoon() - 2
    D/CircularEncoder: frameAvailableSoon() - drainEncoder()
    D/CircularEncoder: ByteBuffer bbEncodedData = bbArEncodedData[encoderStatus] 0
    D/CircularEncoder: mediaCodecBufferInfo.offset, mediaCodecBufferInfo.size 0,34263
    D/CircularEncoderBuffer: add(ByteBuffer bbTotal, int flags, long ptsUsec) 0,3172162036
    D/CircularEncoderBuffer: int size = bbTotal.limit() - bbTotal.position()  34263=34263-0
    D/CircularEncoderBuffer: canAdd()
    D/CircularEncoderBuffer: getHeadStart()
    D/CircularEncoderBuffer: dataLen metaLen 22500000,900
    D/CircularEncoderBuffer: getHeadStart()
    D/CircularEncoderBuffer: intMetaHead : 640
    D/CircularEncoderBuffer: flags ptsUsec packetStart size 0 3172162036 19978810 34263
    D/CircularEncoder: sent 34263 bytes to muxer, ts=3172162036
    D/ContiCaptureActivity: JHE Recorded Frames : 1061
     */


    private static final String TAG = "CircularEncoderBuffer";
    private static final boolean EXTRA_DEBUG = true;
    private static final boolean VERBOSE = true;

    // Raw data (e.g. AVC NAL units) held here.
    //
    // The MediaMuxer writeSampleData() function takes a ByteBuffer.  If it's a "direct"
    // ByteBuffer it'll access the data directly, if it's a regular ByteBuffer it'll use
    // JNI functions to access the backing byte[] (which, in the current VM, is done without
    // copying the data).
    //
    // It's much more convenient to work with a byte[], so we just wrap it with a ByteBuffer
    // as needed.  This is a bit awkward when we hit the edge of the buffer, but for that
    // we can just do an allocation and data copy (we know it happens at most once per file
    // save operation).
    private ByteBuffer bbTotal;
    private byte[] baTotal;

    // Meta-data held here.  We're using a collection of arrays, rather than an array of
    // objects with multiple fields, to minimize allocations and heap footprint.
    private int[] mPacketFlags;
    private long[] mPacketPtsUsec;
    private int[] mPacketStart;
    private int[] mPacketLength;


    // Data is added at head and removed from tail.  Head points to an empty node, so if
    // head==tail the list is empty.
    private int intMetaHead;

    public int getBaTotalSize() {
        return baTotalSize;
    }

    int baTotalSize = 0;

    /**
     * Allocates the circular buffers we use for encoded data and meta-data.
     */
    public CircularEncoderBuffer(int bitRate, int frameRate, int desiredSpanSec) {
        Log.d(TAG, "CircularEncoderBuffer(int bitRate, int frameRate, int desiredSpanSec)");
        // For the encoded data, we assume the encoded bit rate is close to what we request.
        //
        // There would be a minor performance advantage to using a power of two here, because
        // not all ARM CPUs support integer modulus.
        baTotalSize = bitRate * desiredSpanSec / 8;
        Log.d(TAG, "int baTotalSize " + String.valueOf(baTotalSize));
        baTotal = new byte[baTotalSize]; // Always 22,500,000
        bbTotal = ByteBuffer.wrap(baTotal);

        // Meta-data is smaller than encoded data for non-trivial frames, so we over-allocate
        // a bit.  This should ensure that we drop packets because we ran out of (expensive)
        // data storage rather than (inexpensive) metadata storage.
        int metaBufferCount = frameRate * desiredSpanSec * 2;
        mPacketFlags = new int[metaBufferCount];
        mPacketPtsUsec = new long[metaBufferCount];
        mPacketStart = new int[metaBufferCount];
        mPacketLength = new int[metaBufferCount];

        if (VERBOSE) {
            Log.d(TAG, "CBE: bitRate=" + bitRate + " frameRate=" + frameRate +
                    " desiredSpan=" + desiredSpanSec + ": baTotalSize=" + baTotalSize +
                    " metaBufferCount=" + metaBufferCount);
        }
        //CBE: bitRate=6000000 frameRate=15 desiredSpan=30: baTotalSize=22500000 metaBufferCount=900
    }

    /**
     * Computes the amount of time spanned by the buffered data, based on the presentation
     * time stamps.
     */
    public long computeTimeSpanUsec() {
        Log.d(TAG, "computeTimeSpanUsec()");
        Log.d(TAG, "Computes the amount of time spanned by the buffered data, based on the presentation time stamps.");


        final int metaLen = mPacketStart.length;

        if (intMetaHead == 0) {
            // empty list
            return 0;
        }

        // head points to the next available node, so grab the previous one
        int beforeHead = (intMetaHead + metaLen - 1) % metaLen;
        return mPacketPtsUsec[beforeHead] - mPacketPtsUsec[0];
    }

    /**
     * Adds a new encoded data packet to the buffer.
     *
     * @param byteBuffer The data.  Set position() to the start offset and limit() to position+size.
     *                   The position and limit may be altered by this method.
     * @param size       Number of bytes in the packet.
     * @param flags      MediaCodec.BufferInfo flags.
     * @param ptsUsec    Presentation time stamp, in microseconds.
     */
    // JACK - byteBuffer == bbEncodedData
    public void add(ByteBuffer byteBuffer, int flags, long ptsUsec) {
        Log.d(TAG, "add(ByteBuffer bbEncodedData, int flags, long ptsUsec) " + String.valueOf(flags) + "," + String.valueOf(ptsUsec));

        int size = byteBuffer.limit() - byteBuffer.position();
        Log.d(TAG, "int size = bbTotal.limit() - bbTotal.position() "
                + " " + String.valueOf(size)
                + "=" + String.valueOf(byteBuffer.limit())
                + "-" + String.valueOf(byteBuffer.position()));

        if (VERBOSE) {
            Log.d(TAG, "add size=" + size + " flags=0x" + Integer.toHexString(flags) +
                    " pts=" + ptsUsec);
        }

        if (canAdd(size)) {

            final int dataLen = baTotal.length; //  baTotal.length : 22500000
            final int metaLen = mPacketStart.length; // mPacketStart.length : 900

            int dxPacketStart = getHeadStart();
            dxLaskPacketStart = getHeadStart();


            mPacketFlags[intMetaHead] = flags;
            mPacketPtsUsec[intMetaHead] = ptsUsec;
            mPacketStart[intMetaHead] = dxPacketStart;
            mPacketLength[intMetaHead] = size;
            Log.d(TAG, "intMetaHead : " + String.valueOf(intMetaHead));
            Log.d(TAG, "flags ptsUsec dxPacketStart size"
                    + " " + String.valueOf(flags)
                    + " " + String.valueOf(ptsUsec)
                    + " " + String.valueOf(dxPacketStart)
                    + " " + String.valueOf(size));

            Log.d(TAG, "Copy the data in.  Take care if it gets split in half");
            // Copy the data in.  Take care if it gets split in half.
            if (dxPacketStart + size < dataLen) {
                // one chunk
                Log.d(TAG, "one chunk - dxPacketStart + size < dataLen");


                // byteBuffer.position() : 0;
                byteBuffer.get(
                        baTotal, //the target byte array(// JACK : Total ByteArray).
                        dxPacketStart, // the offset of the target byte array.
                        size //the number of bytes to read.
                );
                Log.d(TAG, "byteBuffer.get(baTotal," + "" + String.valueOf(dxPacketStart) + "," + String.valueOf(size));
                // byteBuffer.position() : 63976, 53122, 22230, etc;

            } else {
                // two chunks

                Log.d(TAG, "two chunks");

                int firstSize = dataLen - dxPacketStart;
                if (VERBOSE) {
                    Log.v(TAG, "split, firstsize=" + firstSize + " size=" + size);
                }

                Log.d(TAG, "byteBuffer.position() : " + String.valueOf(byteBuffer.position()));
                byteBuffer.get(baTotal, dxPacketStart, firstSize);
                Log.d(TAG, "byteBuffer.position() after get 1 : " + String.valueOf(byteBuffer.position()));
                byteBuffer.get(baTotal, 0, size - firstSize);
                Log.d(TAG, "byteBuffer.position() after get 2 : " + String.valueOf(byteBuffer.position()));

            }

            intMetaHead = (intMetaHead + 1) % metaLen; // JHE : metaLen==900
            Log.d(TAG,
                    "intMetaHead : " + String.valueOf(intMetaHead) + // 0 -> intRecordedFrameNum-1
                            " metaLen : " + String.valueOf(metaLen) // 900
            );

            if (EXTRA_DEBUG) {
                // The head packet is the next-available spot.
                mPacketFlags[intMetaHead] = 0x77aaccff;
                mPacketPtsUsec[intMetaHead] = -1000000000L;
                mPacketStart[intMetaHead] = -100000;
                mPacketLength[intMetaHead] = Integer.MAX_VALUE;
            }


        } else {
            Log.d(TAG, "cannot add");
        }
    }

    /**
     * Returns the index of the oldest sync frame.  Valid until the next add().
     * <p/>
     * When sending output to a MediaMuxer, start here.
     */
    public int getFirstIndex() {
        Log.d(TAG, "getFirstIndex()");

        final int metaLen = mPacketStart.length;
        int index = 0;

        Log.d(TAG,
                "getFirstIndex() metaLen : " + String.valueOf(mPacketStart.length) +
                        ", intMetaHead : " + String.valueOf(intMetaHead) +
                        ", index : " + String.valueOf(0));

        while (index != intMetaHead) {
            if ((mPacketFlags[index] & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
                break;
            }
            index = (index + 1) % metaLen;
        }

        if (index == intMetaHead) {
            Log.w(TAG, "HEY: could not find sync frame in buffer");
            index = -1;
        }
        Log.d(TAG, "index to return : " + String.valueOf(index));
        return index;
    }

    /**
     * Returns the index of the next packet, or -1 if we've reached the end.
     */
    public int getNextIndex(int index) {

        final int metaLen = mPacketStart.length;
        int next = (index + 1) % metaLen;
        if (next == intMetaHead) {
            next = -1;
        }
        Log.d(TAG, "getNextIndex(index) index :" + String.valueOf(index) + " return : " + String.valueOf(next));

        return next;
    }

    /**
     * Returns a reference to a "direct" ByteBuffer with the data, and fills in the
     * BufferInfo.
     * <p/>
     * The caller must not modify the contents of the returned ByteBuffer.  Altering
     * the position and limit is allowed.
     */
    //getCircularEncoderByteBufferWithIndex(), saveVideo()
    public ByteBuffer getChunk(int index, MediaCodec.BufferInfo info) {
        Log.d(TAG, "getChunk() int index : " + String.valueOf(index));

        final int dataLen = baTotal.length;
        int packetStart = mPacketStart[index];
        int length = mPacketLength[index];
        Log.d(TAG,
                "dataLen : " + String.valueOf(dataLen) +
                        ", packetStart : " + String.valueOf(packetStart) +
                        ", length : " + String.valueOf(length)
        );

        info.flags = mPacketFlags[index];
        info.offset = packetStart;
        info.presentationTimeUs = mPacketPtsUsec[index];
        info.size = length;


        if (packetStart + length <= dataLen) {
            // one chunk; return full buffer to avoid copying data
            return bbTotal;
        } else {
            // two chunks
            ByteBuffer tempBuf = ByteBuffer.allocateDirect(length);
            int firstSize = dataLen - packetStart;
            tempBuf.put(baTotal, mPacketStart[index], firstSize);
            tempBuf.put(baTotal, 0, length - firstSize);
            info.offset = 0;
            return tempBuf;
        }
    }

    /**
     * Computes the data buffer offset for the next place to store data.
     * <p/>
     * Equal to the start of the previous packet's data plus the previous packet's length.
     */
    public int getHeadStart() {
        Log.d(TAG, "getHeadStart()");
        Log.d(TAG, "intMetaHead : " + String.valueOf(intMetaHead));
        if (intMetaHead == 0) {
            // list is empty
            return 0;
        }

        final int dataLen = baTotal.length;
        final int metaLen = mPacketStart.length;

        int beforeHead = (intMetaHead + metaLen - 1) % metaLen;
        Log.d(TAG, "return (mPacketStart[beforeHead] + mPacketLength[beforeHead] + 1) % dataLen : " + String.valueOf((mPacketStart[beforeHead] + mPacketLength[beforeHead] + 1) % dataLen));
        return (mPacketStart[beforeHead] + mPacketLength[beforeHead] + 1) % dataLen;
    }

    /**
     * Determines whether this is enough space to fit "size" bytes in the data buffer, and
     * one more packet in the meta-data buffer.
     *
     * @return True if there is enough space to add without removing anything.
     */
    private boolean canAdd(int size) {
        Log.d(TAG, "canAdd()");

        final int dataLen = baTotal.length; //// JACK : Total ByteArray
        final int metaLen = mPacketStart.length;

        if (size > dataLen) {
            throw new RuntimeException("Enormous packet: " + size + " vs. buffer " +
                    dataLen);
        }
        if (intMetaHead == 0) {
            // empty list
            return true;
        }

        // Make sure we can advance head without stepping on the tail.
        int nextHead = (intMetaHead + 1) % metaLen;


        // Need the byte offset of the start of the "tail" packet, and the byte offset where
        // "head" will store its data.
        int headStart = getHeadStart();
        int tailStart = mPacketStart[0];
        int freeSpace = (tailStart + dataLen - headStart) % dataLen;
        if (size > freeSpace) {
            if (VERBOSE) {
                Log.v(TAG, "ran out of data (tailStart=" + tailStart + " headStart=" + headStart +
                        " req=" + size + " free=" + freeSpace + ")");
            }
            return false;
        }

        if (VERBOSE) {
            Log.v(TAG, "OK: baTotal.length:size=" + size + " free=" + freeSpace + " metaFree=" +
                    ((0 + metaLen - intMetaHead) % metaLen - 1));
        }

        return true;
    }


    public ByteBuffer getByteBufferTotal() {
        return bbTotal;
    }


    public byte[] getBaTotal() {
        return baTotal;
    }

    public void setBaTotal(byte[] _baTotal) {
        baTotal = _baTotal;
    }


    //ByJack
    public void setBbTotal(ByteBuffer _bbTotal) {
        bbTotal = _bbTotal;
    }


    int dxLaskPacketStart = 0;


    public int getDxLaskPacketStart() {
        return getHeadStart();
    }

    public void setDxLaskPacketStart(int dxLaskPacketStart) {
        this.dxLaskPacketStart = dxLaskPacketStart;
    }

    public int getIntMetaHead() {
        return intMetaHead;
    }

    public void setIntMetaHead(int intMetaHead) {
        Log.d(TAG, "setIntMetaHead() : "+String.valueOf(intMetaHead));
        this.intMetaHead = intMetaHead;
    }
}
