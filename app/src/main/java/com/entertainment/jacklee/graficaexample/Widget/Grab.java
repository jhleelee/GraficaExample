package com.entertainment.jacklee.graficaexample.Widget;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by Jacklee on 16. 4. 2..
 */
public class Grab {

   public static long longGrabBeginTimeOfBlock;
    public static long longFinalGrabTimeOfBlock;
    public static ArrayList<Grab> arlGrabbedBbEncodedData;
    public static ArrayList<Long> arlTimesOfBlocks;


    private long longGrabBeginTime, ptsUsec;
    private ByteBuffer byteBuffer;
    private int flags;


    public Grab (
            long longGrabBeginTime, ByteBuffer byteBuffer, int flags, long ptsUsec
    ){
        this.longGrabBeginTime = longGrabBeginTime;
        this.byteBuffer = byteBuffer;
        this.flags = flags;
        this.ptsUsec = ptsUsec;

    }

}
