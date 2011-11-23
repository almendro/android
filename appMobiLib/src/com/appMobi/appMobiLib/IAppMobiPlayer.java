package com.appMobi.appMobiLib;

public interface IAppMobiPlayer {

	public abstract void show();

	public abstract void hide();

	public abstract void playPodcast(final String strPodcastURL);

	public abstract void startStation(String strStationID, boolean resumeMode,
			boolean showPlayer);

	public abstract int loadSound(String strRelativeFileURL);

	public abstract void unloadSound(String strRelativeFileURL);

	public abstract void playSound(final String strRelativeFileURL);

	public abstract void startAudio(String strRelativeFileURL);

	public abstract void toggleAudio();

	public abstract void stopAudio();

	public abstract void play();

	public abstract void pause();

	public abstract void stop();

	public abstract void volume(int iVolumePercentage);

	public abstract void rewind();

	public abstract void ffwd();

	public abstract void setColors(String strBackColor, String strFillColor,
			String strDoneColor, String strPlayColor);

	// Merely stubbed for now -- Allows setting the position of the player screen on large tablets
	// May be not implemented on Android based on the way Activity and screens work
	public abstract void setPosition(int portraitX, int portraitY,
			int landscapeX, int landscapeY);

	public abstract void startShoutcast(String strShoutcastURL,
			boolean showPlayer);

}