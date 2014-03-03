/**
 * Copyright (C) 2014 Martin Nordholts.
 * All rights reserved.
 */

package com.chromecode.android.metronome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class MetronomeActivity extends Activity {
	private final static String BPM_KEY = "bpm";

	private SoundPool mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
	private int mBeatSoundId = -1;
	private MetronomeView mMetronomeView;
	private TextView mBpmText;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
    	mBeatSoundId = mSoundPool.load(this, R.raw.beat, 1);

        mBpmText = (TextView) findViewById(R.id.bpm_text);

    	mMetronomeView = (MetronomeView) findViewById(R.id.metronome_view);
        mMetronomeView.setBeatRunnable(new PlayBeat());
        mMetronomeView.setBpmChangedRunnable(new UpdateBpmText());
        restoreBpm(mMetronomeView);
    	
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }    

    @Override
    protected void onDestroy() {
    	mSoundPool.release();
    	mSoundPool = null;
    	
    	super.onDestroy();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	storeBpm(mMetronomeView);
    }
    
    private void storeBpm(MetronomeView source) {
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor e = sp.edit();
		e.putFloat(BPM_KEY, source.getBpm());
		e.commit();
    }
    
    private void restoreBpm(MetronomeView destination) {
		SharedPreferences sp = getPreferences(MODE_PRIVATE);
		destination.setBpm(sp.getFloat(BPM_KEY, destination.getDefaultBpm()));
    }
    
    class UpdateBpmText implements Runnable {
    	@Override
    	public void run() {
    		int bpm = (int)(mMetronomeView.getBpm() + 0.5f);
    		mBpmText.setText(bpm + " " + getString(R.string.bpm_unit_text));
    	}
    }
    
   class PlayBeat implements Runnable {
    	@Override
    	public void run() {
    		if (mSoundPool != null && mBeatSoundId != -1) {
    			mSoundPool.play(mBeatSoundId, 1.0f /*leftVolume*/, 1.0f /*rightVolume*/, 0 /*priority*/, 0 /*loop*/, 1.0f /*rate*/);
    		}
    	}
    }
}