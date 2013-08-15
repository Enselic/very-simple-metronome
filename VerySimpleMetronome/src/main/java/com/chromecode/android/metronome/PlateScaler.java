package com.chromecode.android.metronome;

import android.content.res.Configuration;

public class PlateScaler {
	
	private int mOrientation = Configuration.ORIENTATION_PORTRAIT;
	private float mNormalizedPlateWidth;
	private float mNormalizedPlateHeight;
	
	private int mCanvasWidth;
	private int mCanvasHeight;
	private int mPortraitCanvasWidth;
	private int mPortraitCanvasHeight;
	private float mPlateDescent;
	private float mPlateWidth;
	private float mPlateHeight;
	private boolean mInvalid;
	
	public PlateScaler() {
    }
	
	public void setCanvasSize(int canvasWidth, int canvasHeight) {
		if (canvasWidth <= 0 || canvasHeight <= 0) {
			throw new IllegalArgumentException("Both arguments must be > 0");
		}
		
		mCanvasWidth = canvasWidth;
		mCanvasHeight = canvasHeight;
		
		if (canvasWidth < canvasHeight) {
			mPortraitCanvasWidth = canvasWidth;
			mPortraitCanvasHeight = canvasHeight;
		} else {
			mPortraitCanvasWidth = canvasHeight;
			mPortraitCanvasHeight = canvasWidth;
		}
		
		invalidate();
	}
	
	public void setNormalizedPlateSize(float normalizedPlateWidth, float normalizedPlateHeight) {
		if (normalizedPlateWidth <= 0 || normalizedPlateHeight <= 0) {
			throw new IllegalArgumentException("Both arguments must be > 0");
		}

		mNormalizedPlateWidth = normalizedPlateWidth;
		mNormalizedPlateHeight = normalizedPlateHeight;

		invalidate();
	}
	
	public void setOrientation(int orientation) {
		if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
			// Default to portrait
			orientation = Configuration.ORIENTATION_PORTRAIT;
		}
		
		mOrientation = orientation;

		invalidate();
	}
	
	private void ensureFullyInitialized() {
		boolean fullyInitialized = 
			mNormalizedPlateWidth > 0 && 
			mNormalizedPlateHeight > 0 &&
			mCanvasWidth > 0 &&
			mCanvasHeight > 0 &&
			mPortraitCanvasWidth > 0 &&
			mPortraitCanvasHeight > 0 &&
			(mOrientation == Configuration.ORIENTATION_PORTRAIT ||
					mOrientation == Configuration.ORIENTATION_LANDSCAPE);
			
		if (!fullyInitialized) {
			throw new IllegalStateException("Not fully initialized");
		}
	}
	
	public float getPlateWidth() {
		ensureUpdated();
		return mPlateWidth;
	}
	
	public float getPlateHeight() {
		ensureUpdated();
		return mPlateHeight;
	}
	
	public float getPlateVisibleHeight() {
		ensureUpdated();
		return mPlateHeight - mPlateDescent;
	}

	public float getPlateDescent() {
		ensureUpdated();
		return mPlateDescent;
	}
	
	private void invalidate() {
		mInvalid = true;
	}
	
	private void ensureUpdated() {
		if (!mInvalid) {
			return;
		}
		
		ensureFullyInitialized();
		
		final float portraitPlateDescent = mNormalizedPlateHeight * mPortraitCanvasWidth / mNormalizedPlateWidth - mPortraitCanvasHeight;
		final float landscapePlateDescent = portraitPlateDescent * mPortraitCanvasWidth / mPortraitCanvasHeight;
		final float landscapePlateWidth = mNormalizedPlateWidth * (landscapePlateDescent + mPortraitCanvasWidth) / mNormalizedPlateHeight;
		
		final boolean portrait = mOrientation == Configuration.ORIENTATION_PORTRAIT;
		mPlateDescent = portrait ? portraitPlateDescent : landscapePlateDescent;
		mPlateWidth = portrait ? mPortraitCanvasWidth : landscapePlateWidth;
		mPlateHeight = mCanvasHeight + mPlateDescent;
		
		mInvalid = true;
	}
}
