package com.phonegap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;

import com.appMobi.appMobiLib.AppMobiActivity;
import com.appMobi.appMobiLib.AppMobiCommand;
import com.appMobi.appMobiLib.AppMobiModule;
import com.appMobi.appMobiLib.AppMobiWebView;
import com.phonegap.api.IPlugin;
import com.phonegap.api.PhonegapActivity;
import com.phonegap.api.PluginManager;

public class PhoneGapModule extends AppMobiModule {
	//private static final String LOG_TAG = "DroidGap";

	@Override
	public void setup(AppMobiActivity activity, AppMobiWebView webview) {
		super.setup(activity, webview);		

		CallbackServer callbackServer = new CallbackServer();
		WrapperActivity wa = new WrapperActivity(activity, callbackServer);
		PluginManager pm = new PluginManager(webview, wa);
		PluginManagerWrapper pmw = new PluginManagerWrapper(pm, activity, webview);
		webview.registerCommand(pmw, "phonegapProxy");
		webview.callbackServer = callbackServer;
		webview.pluginManager = pm;
	}
	
	@Override
	public void initialize(AppMobiActivity activity, AppMobiWebView webview)
	{
		super.initialize(activity, webview);
		
		webview.loadUrl("javascript:PhoneGapLoaded();");
	}
	private class PluginManagerWrapper extends AppMobiCommand {
		private PluginManager pm;
		PluginManagerWrapper(PluginManager pm, AppMobiActivity activity, AppMobiWebView webview) {
			super(activity, webview);
			this.pm = pm;
		}
		@SuppressWarnings("unchecked")
		public String exec(final String service, final String action, final String callbackId, final String jsonArgs, final boolean async) {
			return pm.exec(service, action, callbackId, jsonArgs, async);
		}
	}
	
	private class WrapperActivity extends PhonegapActivity {
		
		private AppMobiActivity activity;
		private CallbackServer callbackServer;
		
		WrapperActivity(AppMobiActivity ama, CallbackServer cs) {
			this.activity = ama;
			this.callbackServer = cs;
		}
		
		public Activity getWrappedActivity() {
			return activity;
		}
		
		@Override
		public void addService(String serviceType, String className) {
			//do nothing
			Log.e("", "PhoneGapModule.addService called", new Exception());
		}

		@Override
		public void sendJavascript(String statement) {
			callbackServer.sendJavascript(statement);
		}

		@Override
		public void setActivityResultCallback(IPlugin plugin) {
			//do nothing
			Log.e("", "PhoneGapModule.setActivityResultCallback called", new Exception());
		}

		@Override
		public void startActivityForResult(IPlugin command, Intent intent, int requestCode) {
			activity.startActivityForResult(command, intent, requestCode);
		}

		@Override
		public Resources getResources() {
			return activity.getResources();
		}

		@Override
		public String getPackageName() {
			return activity.getPackageName();
		}
		
		@Override
		public ContentResolver getContentResolver() {
			return activity.getContentResolver();
		}

		@Override
		public void addContentView(View view, LayoutParams params) {
			
			activity.addContentView(view, params);
		}

		@Override
		public void closeContextMenu() {
			
			activity.closeContextMenu();
		}

		@Override
		public void closeOptionsMenu() {
			
			activity.closeOptionsMenu();
		}

		@Override
		public PendingIntent createPendingResult(int requestCode, Intent data,
				int flags) {
			
			return activity.createPendingResult(requestCode, data, flags);
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			
			return activity.dispatchKeyEvent(event);
		}

		@Override
		public boolean dispatchPopulateAccessibilityEvent(
				AccessibilityEvent event) {
			
			return activity.dispatchPopulateAccessibilityEvent(event);
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			
			return activity.dispatchTouchEvent(ev);
		}

		@Override
		public boolean dispatchTrackballEvent(MotionEvent ev) {
			
			return activity.dispatchTrackballEvent(ev);
		}

		@Override
		public View findViewById(int id) {
			
			return activity.findViewById(id);
		}

		@Override
		public void finish() {
			
			activity.finish();
		}

		@Override
		public void finishActivity(int requestCode) {
			
			activity.finishActivity(requestCode);
		}

		@Override
		public void finishActivityFromChild(Activity child, int requestCode) {
			
			activity.finishActivityFromChild(child, requestCode);
		}

		@Override
		public void finishFromChild(Activity child) {
			
			activity.finishFromChild(child);
		}

		@Override
		public ComponentName getCallingActivity() {
			
			return activity.getCallingActivity();
		}

		@Override
		public String getCallingPackage() {
			
			return activity.getCallingPackage();
		}

		@Override
		public int getChangingConfigurations() {
			
			return activity.getChangingConfigurations();
		}

		@Override
		public ComponentName getComponentName() {
			
			return activity.getComponentName();
		}

		@Override
		public View getCurrentFocus() {
			
			return activity.getCurrentFocus();
		}

		@Override
		public Intent getIntent() {
			
			return activity.getIntent();
		}

		@Override
		public Object getLastNonConfigurationInstance() {
			
			return activity.getLastNonConfigurationInstance();
		}

		@Override
		public LayoutInflater getLayoutInflater() {
			
			return activity.getLayoutInflater();
		}

		@Override
		public String getLocalClassName() {
			
			return activity.getLocalClassName();
		}

		@Override
		public MenuInflater getMenuInflater() {
			
			return activity.getMenuInflater();
		}

		@Override
		public SharedPreferences getPreferences(int mode) {
			
			return activity.getPreferences(mode);
		}

		@Override
		public int getRequestedOrientation() {
			
			return activity.getRequestedOrientation();
		}

		@Override
		public Object getSystemService(String name) {
			
			return activity.getSystemService(name);
		}

		@Override
		public int getTaskId() {
			
			return activity.getTaskId();
		}

		@Override
		public int getWallpaperDesiredMinimumHeight() {
			
			return activity.getWallpaperDesiredMinimumHeight();
		}

		@Override
		public int getWallpaperDesiredMinimumWidth() {
			
			return activity.getWallpaperDesiredMinimumWidth();
		}

		@Override
		public Window getWindow() {
			
			return activity.getWindow();
		}

		@Override
		public WindowManager getWindowManager() {
			
			return activity.getWindowManager();
		}

		@Override
		public boolean hasWindowFocus() {
			
			return activity.hasWindowFocus();
		}

		@Override
		public boolean isFinishing() {
			
			return activity.isFinishing();
		}

		@Override
		public boolean isTaskRoot() {
			
			return activity.isTaskRoot();
		}

		@Override
		public boolean moveTaskToBack(boolean nonRoot) {
			
			return activity.moveTaskToBack(nonRoot);
		}

		@Override
		public void onAttachedToWindow() {
			
			activity.onAttachedToWindow();
		}

		@Override
		public void onBackPressed() {
			
			activity.onBackPressed();
		}

		@Override
		public void onConfigurationChanged(Configuration newConfig) {
			
			activity.onConfigurationChanged(newConfig);
		}

		@Override
		public void onContentChanged() {
			
			activity.onContentChanged();
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
			
			return activity.onContextItemSelected(item);
		}

		@Override
		public void onContextMenuClosed(Menu menu) {
			
			activity.onContextMenuClosed(menu);
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			
			activity.onCreate(savedInstanceState);
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			
			activity.onCreateContextMenu(menu, v, menuInfo);
		}

		@Override
		public CharSequence onCreateDescription() {
			
			return activity.onCreateDescription();
		}

		@Override
		public boolean onCreateOptionsMenu(Menu menu) {
			
			return activity.onCreateOptionsMenu(menu);
		}

		@Override
		public boolean onCreatePanelMenu(int featureId, Menu menu) {
			
			return activity.onCreatePanelMenu(featureId, menu);
		}

		@Override
		public View onCreatePanelView(int featureId) {
			
			return activity.onCreatePanelView(featureId);
		}

		@Override
		public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
			
			return activity.onCreateThumbnail(outBitmap, canvas);
		}

		@Override
		public View onCreateView(String name, Context context,
				AttributeSet attrs) {
			
			return activity.onCreateView(name, context, attrs);
		}

		@Override
		public void onDetachedFromWindow() {
			
			activity.onDetachedFromWindow();
		}

		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			
			return activity.onKeyDown(keyCode, event);
		}

		@Override
		public boolean onKeyLongPress(int keyCode, KeyEvent event) {
			
			return activity.onKeyLongPress(keyCode, event);
		}

		@Override
		public boolean onKeyMultiple(int keyCode, int repeatCount,
				KeyEvent event) {
			
			return activity.onKeyMultiple(keyCode, repeatCount, event);
		}

		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			
			return activity.onKeyUp(keyCode, event);
		}

		@Override
		public void onLowMemory() {
			
			activity.onLowMemory();
		}

		@Override
		public boolean onMenuItemSelected(int featureId, MenuItem item) {
			
			return activity.onMenuItemSelected(featureId, item);
		}

		@Override
		public boolean onMenuOpened(int featureId, Menu menu) {
			
			return activity.onMenuOpened(featureId, menu);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			
			return activity.onOptionsItemSelected(item);
		}

		@Override
		public void onOptionsMenuClosed(Menu menu) {
			
			activity.onOptionsMenuClosed(menu);
		}

		@Override
		public void onPanelClosed(int featureId, Menu menu) {
			
			activity.onPanelClosed(featureId, menu);
		}


		@Override
		public boolean onPrepareOptionsMenu(Menu menu) {
			
			return activity.onPrepareOptionsMenu(menu);
		}

		@Override
		public boolean onPreparePanel(int featureId, View view, Menu menu) {
			
			return activity.onPreparePanel(featureId, view, menu);
		}

		@Override
		public Object onRetainNonConfigurationInstance() {
			
			return activity.onRetainNonConfigurationInstance();
		}

		@Override
		public boolean onSearchRequested() {
			
			return activity.onSearchRequested();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			
			return activity.onTouchEvent(event);
		}

		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			
			return activity.onTrackballEvent(event);
		}

		@Override
		public void onUserInteraction() {
			
			activity.onUserInteraction();
		}

		@Override
		public void onWindowAttributesChanged(
				android.view.WindowManager.LayoutParams params) {
			
			activity.onWindowAttributesChanged(params);
		}

		@Override
		public void onWindowFocusChanged(boolean hasFocus) {
			
			activity.onWindowFocusChanged(hasFocus);
		}

		@Override
		public void openContextMenu(View view) {
			
			activity.openContextMenu(view);
		}

		@Override
		public void openOptionsMenu() {
			
			activity.openOptionsMenu();
		}

		@Override
		public void overridePendingTransition(int enterAnim, int exitAnim) {
			
			activity.overridePendingTransition(enterAnim, exitAnim);
		}

		@Override
		public void registerForContextMenu(View view) {
			
			activity.registerForContextMenu(view);
		}

		@Override
		public void setContentView(int layoutResID) {
			
			activity.setContentView(layoutResID);
		}

		@Override
		public void setContentView(View view, LayoutParams params) {
			
			activity.setContentView(view, params);
		}

		@Override
		public void setContentView(View view) {
			
			activity.setContentView(view);
		}

		@Override
		public void setIntent(Intent newIntent) {
			
			activity.setIntent(newIntent);
		}

		@Override
		public void setPersistent(boolean isPersistent) {
			
			activity.setPersistent(isPersistent);
		}

		@Override
		public void setRequestedOrientation(int requestedOrientation) {
			
			activity.setRequestedOrientation(requestedOrientation);
		}

		@Override
		public void setTitle(CharSequence title) {
			
			activity.setTitle(title);
		}

		@Override
		public void setTitle(int titleId) {
			
			activity.setTitle(titleId);
		}

		@Override
		public void setTitleColor(int textColor) {
			
			activity.setTitleColor(textColor);
		}

		@Override
		public void setVisible(boolean visible) {
			
			activity.setVisible(visible);
		}

		@Override
		public void startActivity(Intent intent) {
			
			activity.startActivity(intent);
		}

		@Override
		public void startActivityForResult(Intent intent, int requestCode) {
			
			activity.startActivityForResult(intent, requestCode);
		}

		@Override
		public void startActivityFromChild(Activity child, Intent intent,
				int requestCode) {
			
			activity.startActivityFromChild(child, intent, requestCode);
		}

		@Override
		public boolean startActivityIfNeeded(Intent intent, int requestCode) {
			
			return activity.startActivityIfNeeded(intent, requestCode);
		}

		@Override
		public void startIntentSender(IntentSender intent, Intent fillInIntent,
				int flagsMask, int flagsValues, int extraFlags)
				throws SendIntentException {
			
			activity.startIntentSender(intent, fillInIntent, flagsMask, flagsValues,
					extraFlags);
		}

		@Override
		public void startIntentSenderForResult(IntentSender intent,
				int requestCode, Intent fillInIntent, int flagsMask,
				int flagsValues, int extraFlags) throws SendIntentException {
			
			activity.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask,
					flagsValues, extraFlags);
		}

		@Override
		public void startIntentSenderFromChild(Activity child,
				IntentSender intent, int requestCode, Intent fillInIntent,
				int flagsMask, int flagsValues, int extraFlags)
				throws SendIntentException {
			
			activity.startIntentSenderFromChild(child, intent, requestCode, fillInIntent,
					flagsMask, flagsValues, extraFlags);
		}

		@Override
		public void startManagingCursor(Cursor c) {
			
			activity.startManagingCursor(c);
		}

		@Override
		public boolean startNextMatchingActivity(Intent intent) {
			
			return activity.startNextMatchingActivity(intent);
		}

		@Override
		public void startSearch(String initialQuery,
				boolean selectInitialQuery, Bundle appSearchData,
				boolean globalSearch) {
			
			activity
					.startSearch(initialQuery, selectInitialQuery, appSearchData,
							globalSearch);
		}

		@Override
		public void stopManagingCursor(Cursor c) {
			
			activity.stopManagingCursor(c);
		}

		@Override
		public void takeKeyEvents(boolean get) {
			
			activity.takeKeyEvents(get);
		}

		@Override
		public void triggerSearch(String query, Bundle appSearchData) {
			
			activity.triggerSearch(query, appSearchData);
		}

		@Override
		public void unregisterForContextMenu(View view) {
			
			activity.unregisterForContextMenu(view);
		}

		@Override
		public boolean bindService(Intent service, ServiceConnection conn,
				int flags) {
			
			return activity.bindService(service, conn, flags);
		}

		@Override
		public int checkCallingOrSelfPermission(String permission) {
			
			return activity.checkCallingOrSelfPermission(permission);
		}

		@Override
		public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
			
			return activity.checkCallingOrSelfUriPermission(uri, modeFlags);
		}

		@Override
		public int checkCallingPermission(String permission) {
			
			return activity.checkCallingPermission(permission);
		}

		@Override
		public int checkCallingUriPermission(Uri uri, int modeFlags) {
			
			return activity.checkCallingUriPermission(uri, modeFlags);
		}

		@Override
		public int checkPermission(String permission, int pid, int uid) {
			
			return activity.checkPermission(permission, pid, uid);
		}

		@Override
		public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
			
			return activity.checkUriPermission(uri, pid, uid, modeFlags);
		}

		@Override
		public int checkUriPermission(Uri uri, String readPermission,
				String writePermission, int pid, int uid, int modeFlags) {
			
			return activity.checkUriPermission(uri, readPermission, writePermission, pid, uid,
					modeFlags);
		}

		@Override
		public void clearWallpaper() throws IOException {
			
			activity.clearWallpaper();
		}

		@Override
		public Context createPackageContext(String packageName, int flags)
				throws NameNotFoundException {
			
			return activity.createPackageContext(packageName, flags);
		}

		@Override
		public String[] databaseList() {
			
			return activity.databaseList();
		}

		@Override
		public boolean deleteDatabase(String name) {
			
			return activity.deleteDatabase(name);
		}

		@Override
		public boolean deleteFile(String name) {
			
			return activity.deleteFile(name);
		}

		@Override
		public void enforceCallingOrSelfPermission(String permission,
				String message) {
			
			activity.enforceCallingOrSelfPermission(permission, message);
		}

		@Override
		public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags,
				String message) {
			
			activity.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
		}

		@Override
		public void enforceCallingPermission(String permission, String message) {
			
			activity.enforceCallingPermission(permission, message);
		}

		@Override
		public void enforceCallingUriPermission(Uri uri, int modeFlags,
				String message) {
			
			activity.enforceCallingUriPermission(uri, modeFlags, message);
		}

		@Override
		public void enforcePermission(String permission, int pid, int uid,
				String message) {
			
			activity.enforcePermission(permission, pid, uid, message);
		}

		@Override
		public void enforceUriPermission(Uri uri, int pid, int uid,
				int modeFlags, String message) {
			
			activity.enforceUriPermission(uri, pid, uid, modeFlags, message);
		}

		@Override
		public void enforceUriPermission(Uri uri, String readPermission,
				String writePermission, int pid, int uid, int modeFlags,
				String message) {
			
			activity.enforceUriPermission(uri, readPermission, writePermission, pid, uid,
					modeFlags, message);
		}

		@Override
		public String[] fileList() {
			
			return activity.fileList();
		}

		@Override
		public Context getApplicationContext() {
			
			return activity.getApplicationContext();
		}

		@Override
		public ApplicationInfo getApplicationInfo() {
			
			return activity.getApplicationInfo();
		}

		@Override
		public AssetManager getAssets() {
			
			return activity.getAssets();
		}

		@Override
		public Context getBaseContext() {
			
			return activity.getBaseContext();
		}

		@Override
		public File getCacheDir() {
			
			return activity.getCacheDir();
		}

		@Override
		public ClassLoader getClassLoader() {
			
			return activity.getClassLoader();
		}

		@Override
		public File getDatabasePath(String name) {
			
			return activity.getDatabasePath(name);
		}

		@Override
		public File getDir(String name, int mode) {
			
			return activity.getDir(name, mode);
		}

		@Override
		public File getExternalCacheDir() {
			
			return activity.getExternalCacheDir();
		}

		@Override
		public File getExternalFilesDir(String type) {
			
			return activity.getExternalFilesDir(type);
		}

		@Override
		public File getFilesDir() {
			
			return activity.getFilesDir();
		}

		@Override
		public File getFileStreamPath(String name) {
			
			return activity.getFileStreamPath(name);
		}

		@Override
		public Looper getMainLooper() {
			
			return activity.getMainLooper();
		}

		@Override
		public String getPackageCodePath() {
			
			return activity.getPackageCodePath();
		}

		@Override
		public PackageManager getPackageManager() {
			
			return activity.getPackageManager();
		}

		@Override
		public String getPackageResourcePath() {
			
			return activity.getPackageResourcePath();
		}

		@Override
		public SharedPreferences getSharedPreferences(String name, int mode) {
			
			return activity.getSharedPreferences(name, mode);
		}

		@Override
		public Drawable getWallpaper() {
			
			return activity.getWallpaper();
		}

		@Override
		public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
			
			activity.grantUriPermission(toPackage, uri, modeFlags);
		}

		@Override
		public boolean isRestricted() {
			
			return activity.isRestricted();
		}

		@Override
		public FileInputStream openFileInput(String name)
				throws FileNotFoundException {
			
			return activity.openFileInput(name);
		}

		@Override
		public FileOutputStream openFileOutput(String name, int mode)
				throws FileNotFoundException {
			
			return activity.openFileOutput(name, mode);
		}

		@Override
		public SQLiteDatabase openOrCreateDatabase(String name, int mode,
				CursorFactory factory) {
			
			return activity.openOrCreateDatabase(name, mode, factory);
		}

		@Override
		public Drawable peekWallpaper() {
			
			return activity.peekWallpaper();
		}

		@Override
		public Intent registerReceiver(BroadcastReceiver receiver,
				IntentFilter filter, String broadcastPermission,
				Handler scheduler) {
			
			return activity.registerReceiver(receiver, filter, broadcastPermission, scheduler);
		}

		@Override
		public Intent registerReceiver(BroadcastReceiver receiver,
				IntentFilter filter) {
			
			return activity.registerReceiver(receiver, filter);
		}

		@Override
		public void removeStickyBroadcast(Intent intent) {
			
			activity.removeStickyBroadcast(intent);
		}

		@Override
		public void revokeUriPermission(Uri uri, int modeFlags) {
			
			activity.revokeUriPermission(uri, modeFlags);
		}

		@Override
		public void sendBroadcast(Intent intent, String receiverPermission) {
			
			activity.sendBroadcast(intent, receiverPermission);
		}

		@Override
		public void sendBroadcast(Intent intent) {
			
			activity.sendBroadcast(intent);
		}

		@Override
		public void sendOrderedBroadcast(Intent intent,
				String receiverPermission, BroadcastReceiver resultReceiver,
				Handler scheduler, int initialCode, String initialData,
				Bundle initialExtras) {
			
			activity.sendOrderedBroadcast(intent, receiverPermission, resultReceiver,
					scheduler, initialCode, initialData, initialExtras);
		}

		@Override
		public void sendOrderedBroadcast(Intent intent,
				String receiverPermission) {
			
			activity.sendOrderedBroadcast(intent, receiverPermission);
		}

		@Override
		public void sendStickyBroadcast(Intent intent) {
			
			activity.sendStickyBroadcast(intent);
		}

		@Override
		public void sendStickyOrderedBroadcast(Intent intent,
				BroadcastReceiver resultReceiver, Handler scheduler,
				int initialCode, String initialData, Bundle initialExtras) {
			
			activity.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler,
					initialCode, initialData, initialExtras);
		}

		@Override
		public void setWallpaper(Bitmap bitmap) throws IOException {
			
			activity.setWallpaper(bitmap);
		}

		@Override
		public void setWallpaper(InputStream data) throws IOException {
			
			activity.setWallpaper(data);
		}

		@Override
		public boolean startInstrumentation(ComponentName className,
				String profileFile, Bundle arguments) {
			
			return activity.startInstrumentation(className, profileFile, arguments);
		}

		@Override
		public ComponentName startService(Intent service) {
			
			return activity.startService(service);
		}

		@Override
		public boolean stopService(Intent name) {
			
			return activity.stopService(name);
		}

		@Override
		public void unbindService(ServiceConnection conn) {
			
			activity.unbindService(conn);
		}

		@Override
		public void unregisterReceiver(BroadcastReceiver receiver) {
			
			activity.unregisterReceiver(receiver);
		}
		
		
	}
}


