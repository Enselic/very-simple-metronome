package com.chromecode.android.metronome;

import android.content.res.Configuration;
import junit.framework.TestCase;

public class UPlateScalerTests extends TestCase {
	private static final int CANVAS_WIDTH = 90;
	private static final int CANVAS_HEIGHT = 160;
	private static final int NORMALIZED_WIDTH = 10;
	private static final int NORMALIZED_HEIGHT = 20;

	public void testPortrait() {
		PlateScaler plateScaler = newStandardPlateScaler(Configuration.ORIENTATION_PORTRAIT);
		
		assertEqualsf(CANVAS_WIDTH, plateScaler.getPlateWidth());
		assertEqualsf(180, plateScaler.getPlateHeight());
		assertEqualsf(160, plateScaler.getPlateVisibleHeight());		
		assertEqualsf(20, plateScaler.getPlateDescent());		
	}
	
	public void testLandscape() {
		PlateScaler plateScaler = newStandardPlateScaler(Configuration.ORIENTATION_LANDSCAPE);

		assertEqualsf(50.625f, plateScaler.getPlateWidth());
		assertEqualsf(101.25f, plateScaler.getPlateHeight());
		assertEqualsf(90, plateScaler.getPlateVisibleHeight());		
		assertEqualsf(11.25f, plateScaler.getPlateDescent());		
	}
	
	
	
	
	private PlateScaler newStandardPlateScaler(int orientation) {
		PlateScaler plateScaler = new PlateScaler();
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			plateScaler.setCanvasSize(CANVAS_WIDTH, CANVAS_HEIGHT);			
		} else {
			plateScaler.setCanvasSize(CANVAS_HEIGHT, CANVAS_WIDTH);			
		}
		plateScaler.setNormalizedPlateSize(NORMALIZED_WIDTH, NORMALIZED_HEIGHT);
		plateScaler.setOrientation(orientation);
		return plateScaler;
	}
	
	private void assertEqualsf(float expected, float actual) {
		assertEquals(expected, actual);
	}
}
