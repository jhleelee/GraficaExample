//package com.entertainment.jacklee.graficaexample.Widget;
//
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.util.AttributeSet;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.View;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//
//public class ProgressView extends View
//{
//	String TAG = "ProgressView";
//	public ProgressView(Context context) {
//		super(context);
//		init(context);
//	}
//
//	public ProgressView(Context paramContext, AttributeSet paramAttributeSet) {
//		super(paramContext, paramAttributeSet);
//		init(paramContext);
//
//	}
//
//	public ProgressView(Context paramContext, AttributeSet paramAttributeSet,
//						int paramInt) {
//		super(paramContext, paramAttributeSet, paramInt);
//		init(paramContext);
//	}
//
//	private Paint progressPaint, firstPaint, threePaint,breakPaint;//三个颜色的画笔
//	private float firstWidth = 4f, threeWidth = 1f;//断点的宽度
//
//	/**
//	 * LinkedList is an implementation of {@link List}, backed by a doubly-linked list.
//	 * All optional operations including adding, removing, and replacing elements are supported.
//	 *
//	 * <p>All elements are permitted, including null.
//	 *
//	 * <p>This class is primarily useful if you need queue-like behavior. It may also be useful
//	 * as a list if you expect your lists to contain zero or one element, but still require the
//	 * ability to scale to slightly larger numbers of elements. In general, though, you should
//	 * probably use {@link ArrayList} if you don't need the queue-like behavior.
//	 *
//	 * @since 1.2
//	 */
//	private LinkedList<Integer> linkedList = new LinkedList<Integer>();
//	private float perPixel = 0l;
//	private float countRecorderTime = Constant.intMaxRecordSec;//总的录制时间
//
//	public void setTotalTime(float time){
//		countRecorderTime = time;
//	}
//
//	private void init(Context paramContext) {
//		Log.d(TAG, "init()");
//		progressPaint = new Paint();
//		firstPaint = new Paint();
//		threePaint = new Paint();
//		breakPaint = new Paint();
//
//		// Background
//		setBackgroundColor(Color.parseColor("#19000000"));
//
//		// 主要进度的颜色
//		progressPaint.setStyle(Paint.Style.FILL);
//		progressPaint.setColor(Color.parseColor("#19e3cf"));
//
//		// 一闪一闪的黄色进度
//		firstPaint.setStyle(Paint.Style.FILL);
//		firstPaint.setColor(Color.parseColor("#ffcc42"));
//
//		// 3秒处的进度
//		threePaint.setStyle(Paint.Style.FILL);
//		threePaint.setColor(Color.parseColor("#12a899"));
//
//		breakPaint.setStyle(Paint.Style.FILL);
//		breakPaint.setColor(Color.parseColor("#000000"));
//
//		DisplayMetrics dm = new DisplayMetrics();
//		((Activity)paramContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
//		perPixel = dm.widthPixels/countRecorderTime;
//
//		perSecProgress = perPixel;
//
//	}
//
//	/**
//	 * 绘制状态
//	 * @author QD
//	 *
//	 */
//	public static enum State {
//		START(0x1),PAUSE(0x2);
//
//		static State mapIntToValue(final int stateInt) {
//			for (State value : State.values()) {
//				if (stateInt == value.getIntValue()) {
//					return value;
//				}
//			}
//			return PAUSE;
//		}
//
//		private int mIntValue;
//
//		State(int intValue) {
//			mIntValue = intValue;
//		}
//
//		int getIntValue() {
//			return mIntValue;
//		}
//	}
//
//
//	private volatile State currentState = State.PAUSE;//Current state
//
//	private boolean isVisible = true;//一闪一闪的黄色区域是否可见
//	private float countWidth = 0;//When a finger is pressed , the length of each progress bar grow
//	private float perProgress = 0;//Yellow areas are visible sparking
//	private float perSecProgress = 0;//	Corresponding pixels per millisecond
//	private long initTime;//绘制完成时的时间戳
//	private long drawFlashTime = 0;//闪动的黄色区域时间戳
//
//	protected void onDraw(Canvas canvas) {
//		super.onDraw(canvas);
////		Log.d(TAG, "onDraw");
//		long currentTimemillies = System.currentTimeMillis();
//		//Log.i("recorder", currentTimemillies  - initTime + "");
//		countWidth = 0;
//		//Each drawing will be chronological queue breakpoints , drawn
//
//		if(!linkedList.isEmpty()){
////			Log.d(TAG, "!linkedList.isEmpty()");
//			float frontTime = 0;
//			Iterator<Integer> iterator = linkedList.iterator();
//
//			while(iterator.hasNext()){
//				int time = iterator.next();
////				Log.d(TAG, "time :"+ String.valueOf(time));
////				Log.d(TAG, "frontTime : "+ String.valueOf(frontTime));
//
//				// The calculated start position to draw a rectangle
//				float left = countWidth;
//
//				//The draw determined the rectangular end position
//				countWidth += (time-frontTime)*perPixel;
//				//绘制进度条
//				canvas.drawRect(left, 0,countWidth,getMeasuredHeight(),progressPaint);
//				//绘制断点
//				canvas.drawRect(countWidth, 0,countWidth + threeWidth,getMeasuredHeight(),breakPaint);
//				countWidth += threeWidth;
//
//				frontTime = time;
//			}
//
//			//Draw three seconds at the break
//			if(linkedList.getLast() <= 3000)
//				canvas.drawRect(perPixel*3000, 0,perPixel*3000+threeWidth,getMeasuredHeight(),threePaint);
//		}else//绘制三秒处的断点
//			canvas.drawRect(perPixel*3000, 0,perPixel*3000+threeWidth,getMeasuredHeight(),threePaint);//绘制三秒处的矩形
//
//		//When the finger touching the screen , a progress bar will increase
//		if(currentState == State.START){
//			Log.d(TAG, "currentState == State.START");
//			perProgress += perSecProgress*(currentTimemillies - initTime );
//			if(countWidth + perProgress <= getMeasuredWidth())
//
//			/**
//			 * Draw the specified Rect using the specified paint. The rectangle will
//			 * be filled or framed based on the Style in the paint.
//			 *
//			 * @param left   The left side of the rectangle to be drawn
//			 * @param top    The top side of the rectangle to be drawn
//			 * @param right  The right side of the rectangle to be drawn
//			 * @param bottom The bottom side of the rectangle to be drawn
//			 * @param paint  The paint used to draw the rect
//			 */
//
//				canvas.drawRect(
//						countWidth, //left
//						0, //top
//						countWidth + perProgress, //right
//						getMeasuredHeight(), // bottom
//						progressPaint
//				);
//
//			else
//				canvas.drawRect(countWidth, 0,getMeasuredWidth(),getMeasuredHeight(),progressPaint);
//		}
//
//		//Draw the yellow area twinkling , blinking once every 500ms
//		if(drawFlashTime==0 || currentTimemillies - drawFlashTime >= 500){
//			isVisible = !isVisible;
//			drawFlashTime = System.currentTimeMillis();
//		}
//		if(isVisible){
//			if(currentState == State.START)
//				canvas.drawRect(countWidth + perProgress, 0,countWidth + firstWidth + perProgress,getMeasuredHeight(),firstPaint);
//			else
//				canvas.drawRect(countWidth, 0,countWidth + firstWidth,getMeasuredHeight(),firstPaint);
//		}
//
//		// Finish drawing a glowing yellow area
//		initTime = System.currentTimeMillis();
//		invalidate();
//	}
//
//	/**
//	 *
//	 Setting the pace of the state
//	 * @param state
//	 */
//	public void setCurrentState(State state){
//		currentState = state;
//		if(state == State.PAUSE)
//			perProgress = perSecProgress;
//	}
//
//	/**
//	 * Lift a finger to save the time point to the queue
//	 * @param time:ms为单位
//	 */
//	public void putProgressList(int time) {
//		linkedList.add(time);
//		Log.d(TAG, "putProgressList "+String.valueOf(linkedList.size()));
//	}
//}