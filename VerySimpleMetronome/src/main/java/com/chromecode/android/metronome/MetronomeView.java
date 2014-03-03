/**
 * Copyright (C) 2014 Martin Nordholts.
 * All rights reserved.
 */

package com.chromecode.android.metronome;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Paint.Cap;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * The metronome works like this:
 * 
 * |---------------|---------------|---------------|---------------|
 * 
 *  \_____________/
 *         |
 *         |
 *     one cycle    
 *     
 * One cycle is normalized to [0..1]. Since the metronome is swing based, there are *two* beats per cycle.
 *
 */
public class MetronomeView extends View {
	private static final float SWING_PART_FRACTION_OF_VISIBLE_HEIGHT = 7/8f;		    
	private static final float SWING_PART_WIDTH_FRACTION_OF_WIDTH = 1/20f;
	private static float SWINGING_FRACTION_OF_WIDTH = 4/5f;

	private static float WEIGHT_WIDTH_FRACTION_OF_SWING_PART_WIDTH = 5.7f;
	private static float WEIGHT_HEIGHT_FRACTION_OF_WIDTH = 1/1.6f;
	

	
    // The swing plate is scaled to match the screen
	final float SWING_PLATE_WIDTH = 10;
	final float SWING_PLATE_HEIGHT = 20;

	
	private long mLastTick;

	private float mMsPerCycle;
	
	private int mMaxBpm;
	private int mMinBpm;
	
	/**
	 * Where in a cycle, [0..1] the swing currently is.
	 */
	private float mSwingPos;
	
	/**
	 * [0..1].
	 */
	private float mNormalizedBpm;
	
	private Paint mSwingPaint;
	
	private final PlateScaler mPlateScaler = new PlateScaler();
	
	/**
	 * The {@link Runnable} to run when a beat is made.
	 */
	private Runnable mBeatRunnable;

	private Paint mWeightPaint;
	
	private RoundRectShape mWeightRect;
	
	private Runnable mBpmChangedRunnable;
	private float mBpm;

	private int mDefaltBpm;
	private float mSwingPartWidth;
	
	public MetronomeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mLastTick = SystemClock.elapsedRealtime();

		readAttrs(attrs);
		sanityCheck();
		
		// Overriden by activity later
		setBpm(mDefaltBpm);
		
		mPlateScaler.setNormalizedPlateSize(SWING_PLATE_WIDTH, SWING_PLATE_HEIGHT);
		
		setBackgroundColor(0xff000000);
	}
	
	private void readAttrs(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MetronomeView);
		mDefaltBpm = a.getInt(R.styleable.MetronomeView_defaultBpm, mDefaltBpm);
		mMinBpm = a.getInt(R.styleable.MetronomeView_minBpm, mMinBpm);
		mMaxBpm = a.getInt(R.styleable.MetronomeView_maxBpm, mMaxBpm);
		a.recycle();
	}
	
	private void sanityCheck() {
		if (mDefaltBpm == 0 || mMinBpm == 0 || mMaxBpm == 0) {
			throw new RuntimeException("Need defaultBpm, minBpm and max Bpm");
		}
		
		if (!(mMinBpm < mDefaltBpm && mDefaltBpm < mMaxBpm)) {
			throw new RuntimeException("minBpm < defaultBpm < maxBpm does not hold, but it must");
		}
	}
	
	public void setBeatRunnable(Runnable beatRunnable) {
		mBeatRunnable = beatRunnable;
	}
	
	public void setBpmChangedRunnable(Runnable bpmChangedRunnable) {
		mBpmChangedRunnable = bpmChangedRunnable;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		// Update swing
		updateSwing();
		
		// Then draw swing
		drawSwing(canvas);

		// Make it an animation
		invalidate();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		boolean handled = false;
		if (getHeight() > 0 && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)) {
			float normalizedBpm = (getHeight() - event.getY()) / getHeight();
			setNormalizedBpm(normalizedBpm);
			handled = true;
		}
		return handled;
	}
		
	private void setNormalizedBpm(float normalizedBpm) {
		final float bpm = mMinBpm + (1 - normalizedBpm) * (mMaxBpm - mMinBpm);
		setBpm(bpm);
	}

	private long progressTime() {
		long newTime = SystemClock.elapsedRealtime();
		long elapsedTime = newTime - mLastTick;
		mLastTick = newTime;
		return elapsedTime > 0 ? elapsedTime : 0;
	}
	
	public void setBpm(float bpm) {
		if (bpm < mMinBpm) {
			bpm = mMinBpm;
		} else if (bpm > mMaxBpm) {
			bpm = mMaxBpm;
		}
		final float beatsPerSeconds = bpm / 60;
		final float millisecondsPerBeat = 1000 / beatsPerSeconds;
		mMsPerCycle = millisecondsPerBeat * 2;
		
		mNormalizedBpm = (bpm - mMinBpm) / (mMaxBpm - mMinBpm);
		mBpm = bpm;
		
		if (mBpmChangedRunnable != null) {
			mBpmChangedRunnable.run();
		}
	}
	
	private void updateSwing() {
		// Move time forward
		long elapsedTime = progressTime();		
		final float swingProgression = elapsedTime / mMsPerCycle;
		
		// If we moved by the center, make a beat
		boolean makeBeat = false;
		if (mSwingPos < 0.5 && mSwingPos + swingProgression >= 0.5) {
			makeBeat = true;
		} else if (mSwingPos < 1.0 && mSwingPos + swingProgression >= 1.0) {
			makeBeat = true;
		}
		if (makeBeat && mBeatRunnable != null) {
			mBeatRunnable.run();
		}

		// Wrap around swing position
		mSwingPos += swingProgression;
		while (mSwingPos > 1.0) {
			mSwingPos -= 1.0;
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		// Update mPlateScaler
		mPlateScaler.setCanvasSize(w, h);
		mPlateScaler.setOrientation(getContext().getResources().getConfiguration().orientation);
		
		// The below depends on mSwingPartWidth
		final float plateWidth = mPlateScaler.getPlateWidth();
		mSwingPartWidth = SWING_PART_WIDTH_FRACTION_OF_WIDTH * plateWidth;
		
		// Setup mWeightRect
		final float weightWidth = WEIGHT_WIDTH_FRACTION_OF_SWING_PART_WIDTH * mSwingPartWidth;
		final float weightHeight = weightWidth * WEIGHT_HEIGHT_FRACTION_OF_WIDTH;
		final float RADIUS_FRACTION_OF_WIDTH = 1/3f;
		final float cornerRadius = RADIUS_FRACTION_OF_WIDTH * mSwingPartWidth;
		float[] outerRadii = new float[] {
				cornerRadius, cornerRadius,
				cornerRadius, cornerRadius,
				cornerRadius, cornerRadius,
				cornerRadius, cornerRadius
		};
		mWeightRect = new RoundRectShape(outerRadii, null, null);
		mWeightRect.resize(weightWidth, weightHeight);		
		
		// Setup mWeightPaint
		LinearGradient weightGradient = new LinearGradient(
				weightWidth / 4f, weightHeight / 4f, weightWidth * 5/4f , weightHeight * 5/4f, 
				0xffffffff, 0xffaaaaaa, Shader.TileMode.MIRROR);
		mWeightPaint = new Paint();
		mWeightPaint.setShader(weightGradient);
		mWeightPaint.setAntiAlias(true);

		// Setup mSwingPaint
		LinearGradient swingGradient = new LinearGradient(
				w / 2 + mSwingPartWidth / 3, 0, w / 2 + mSwingPartWidth * 4/3f, 0, 
				0xff444444, 0xffdddddd, Shader.TileMode.MIRROR);
		mSwingPaint = new Paint();
		mSwingPaint.setShader(swingGradient);			
		mSwingPaint.setAntiAlias(true);
		mSwingPaint.setStrokeCap(Cap.ROUND);
		mSwingPaint.setStrokeWidth(mSwingPartWidth);
	}
	
	private void drawSwing(Canvas canvas) {
		if (mWeightRect == null || mSwingPaint == null || mWeightPaint == null) {
			// not initialized yet
			return;
		}
		
		final float plateWidth = mPlateScaler.getPlateWidth();
		final float plateHeight = mPlateScaler.getPlateHeight();
		final float plateVisibleHeight = mPlateScaler.getPlateVisibleHeight();
		
		final float swingingWidth = plateWidth * SWINGING_FRACTION_OF_WIDTH;		
		final float swingPartLength = plateVisibleHeight * SWING_PART_FRACTION_OF_VISIBLE_HEIGHT;
		
		final float topOffset = (plateVisibleHeight - swingPartLength) / 2;
		final float pivotX = getWidth() / 2;
		final float pivotY = plateHeight;

		final double swingExtent = Math.atan((swingingWidth / 2) / plateHeight);		
		final double angularOffset = Math.sin(mSwingPos * Math.PI * 2);

		// Rotate canvas
		canvas.save();		
		canvas.rotate((float)((angularOffset * swingExtent) * 180 / Math.PI), pivotX, pivotY);
		
		// Draw swing
		canvas.drawLine(pivotX, topOffset, pivotX, topOffset + swingPartLength, mSwingPaint);
		
		// Draw weight
		canvas.translate(
				pivotX - mWeightRect.getWidth() / 2, 
				topOffset + mNormalizedBpm * swingPartLength - mWeightRect.getHeight() / 2);
		mWeightRect.draw(canvas, mWeightPaint);
		
		// Restore canvas
		canvas.restore();
	}

	public float getBpm() {
		return mBpm;
	}
	
	public int getDefaultBpm() {
		return mDefaltBpm;
	}
}
