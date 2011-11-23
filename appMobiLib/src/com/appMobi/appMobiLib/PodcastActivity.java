/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.appMobi.appMobiLib;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;
import fm.flycast.FlyCastPlayer;

public class PodcastActivity extends Activity {

    private VideoView mVideoView;
	private LinearLayout root;
	private String url;
	private AppMobiActivity appMobiActivity;

    @Override
    public void onCreate(Bundle icicle) {
    	super.onCreate(icicle);

    	appMobiActivity = AppMobiActivity.sharedActivity;
    	url = (String)this.getIntent().getExtras().get("url");
        Uri uri = Uri.parse(url);
        final MediaController mc = new MediaController(this);

        root = new LinearLayout(this);
        mVideoView = new VideoView(this);
        root.addView(mVideoView);
        setContentView(root);

    	mVideoView.setVideoURI(uri);
        mVideoView.setMediaController(mc);

		final ProgressDialog spinner = new ProgressDialog(this);
		spinner.setTitle(getApplication().getResources().getString(R.string.app_name));
		spinner.setMessage("Loading");
		spinner.show();

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

			//@Override
			public void onPrepared(MediaPlayer mp) {
		        spinner.dismiss();
				mVideoView.requestFocus();
				mVideoView.setOnErrorListener(MediaPlayerOnErrorListener);
				mVideoView.start();
		        if( isVideo(url) == false )
		        {
		        	mVideoView.setBackgroundResource(R.drawable.splash_screen);
		        	mc.show(1800000);
		        }
		        else
		        	mc.show();
			}
		});

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

        	//@Override
			public void onCompletion(MediaPlayer mp) {
		        finish();
			}
        });

		appMobiActivity.trackPageView("/appMobi.podcast." + getPodcastName(url) + ".start");
    }

    public boolean isVideo(String url)
    {
    	int index = url.lastIndexOf('.');
    	if( index == -1 ) return false;

    	String ext = url.substring(index+1);
    	if( ext.equals("mov") || ext.equals("mp4") || ext.equals("m4v") || ext.equals("mpeg") ) return true;
    	else return false;
    }

    private String getPodcastName(String url)
    {
    	String name = "";
    	int last = url.lastIndexOf("/");
    	if( last > 0 )
    	{
    		name = url.substring(last + 1);
    	}

    	return name;
    }

    private MediaPlayer.OnErrorListener MediaPlayerOnErrorListener = new MediaPlayer.OnErrorListener(){
		//@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {

				switch (what) {
				case FlyCastPlayer.MEDIA_ERROR_SERVER_DIED:
					if (appMobiActivity.appView.player.isPlayingPodcast()) {
						appMobiActivity.myGUIUpdateHandler.post(new Runnable() {
							public void run() {
								appMobiActivity.appView.loadUrl("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.podcast.error',true,true);document.dispatchEvent(ev);");
							}
						});
						appMobiActivity.trackPageView("/appMobi.podcast." + getPodcastName(url) + ".stop");

					}
		    		return true;
				case FlyCastPlayer.MEDIA_ERROR_UNKNOWN:

					return false;
				default:
		    		return true;
				}
		}
    };

    public void handleCompletion()
    {
		appMobiActivity.myGUIUpdateHandler.post(new Runnable() {
			public void run() {
		    	appMobiActivity.appView.loadUrl("javascript:var ev = document.createEvent('Events');ev.initEvent('appMobi.player.podcast.stop',true,true);document.dispatchEvent(ev);");
			}
		});
		appMobiActivity.appView.player.setPlayingPodcast(false);
    }

    @Override
	protected void onStop() {
		super.onStop(); // K3
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy(); // K4
	}

	@Override
	protected void onPause() {
		super.onPause(); // K4
		handleCompletion();
	}
}
