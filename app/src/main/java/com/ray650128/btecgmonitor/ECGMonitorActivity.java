package com.ray650128.btecgmonitor;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SurfaceView;

@SuppressLint("HandlerLeak")
public class ECGMonitorActivity extends Activity {
	
	ActionBar actionBar;
	
	private boolean enRead = false;
	private SurfaceView surface_ECG;
	
	private BTReadThread mReadThread = new BTReadThread(50);
	private Handler msgHandler;
	private DrawECGWaveForm mECGWF;
	
	private String revTmpStr = new String();
	public List<Float> ECGDataList = new ArrayList<Float>();
	public boolean ECGDataIsAvailable = true;
	private float ECGData = 0;
    
	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ecg_monitor);
		surface_ECG = (SurfaceView) findViewById(R.id.surface_ECG);

        mECGWF = new DrawECGWaveForm(surface_ECG);
        
        actionBar = this.getActionBar();
		
        actionBar.setTitle("No Device - Standby");
        actionBar.setSubtitle("Standby");
        actionBar.setIcon(R.drawable.ic_disconnected);
		
		try{
			if(MainActivity.mBTSocket.getInputStream() != null)
			{
				enRead = true;
				mReadThread.start();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		Looper mLooper = Looper.myLooper();
		msgHandler = new MsgHandler(mLooper);
		
		// Setting Timer to Draw and Save data
		Timer mDelayTimer = new Timer();
		TimerTask mDelayTask = new TimerTask() {
			public void run() {
				Message msg = Message.obtain();
				msg.what = 1;
				msgHandler.sendMessage(msg);
			}
		};

		mDelayTimer.schedule(mDelayTask, 1, 1);
	}
	
	@Override
	public void onDestroy()  
    {
		try {
			MainActivity.mBTSocket.close();
			enRead = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
        super.onDestroy();  
        System.exit(0);	
    }
	
	@SuppressLint("NewApi")
	// MsgHandler class to Update UI
	class MsgHandler extends Handler{
		public MsgHandler(Looper lp) {
			super(lp);
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case 0:
				actionBar.setSubtitle("狀態：" + (String)msg.obj);
				if(MainActivity.mBTSocket != null)
					try{
						actionBar.setTitle("藍芽裝置：" + MainActivity.mBTSocket.getRemoteDevice().getName());
						actionBar.setIcon(R.drawable.ic_connected);
					}
					catch(Exception e) {
						e.printStackTrace();
					}
					break;
			case 1:
				if (ECGDataList.size() > 1) {
					List<Float> ECGDataCache = new ArrayList<Float>();
					ECGDataCache.addAll(ECGDataList);
					ECGDataIsAvailable = false;
					ECGDataList.clear();
					ECGDataIsAvailable = true;
					mECGWF.DrawWave(ECGDataCache);
				}
				break;
			}

		}
	}			
	class BTReadThread extends Thread{
		private int wait = 1;
		public BTReadThread(int wait) {
			this.wait = wait;
		}
		
		public void run() {
			while(enRead) {
				try{
					if (MainActivity.mBTSocket.getInputStream() != null)
					{
						MainActivity.mBTSocket.getOutputStream().write(48);						
						byte[] tmp = new byte[1024];
						int len = MainActivity.mBTSocket.getInputStream().read(tmp, 0, 1024);
						if (len > 0) {
							byte[] tmp2 = new byte[len];
							tmp2 = tmp;
							String str = new String(tmp2);
							revTmpStr = revTmpStr + str;
							if(revTmpStr.indexOf(';') != -1) {
								try{
									String ECGDataStrs[] = revTmpStr.split(";");
									for (int i = 0; i < ECGDataStrs.length -1; i++) {
										try{
											ECGData = Float.parseFloat(ECGDataStrs[i].replace(';',' '));
											ECGDataList.add(ECGData);											
										}catch(Exception e) {
											e.printStackTrace();
											continue;
										}
									}
									if ((ECGDataStrs[ECGDataStrs.length - 1].length() == 6) ||
										((ECGDataStrs[ECGDataStrs.length - 1].length() == 7)&&
										 (ECGDataStrs[ECGDataStrs.length - 1].indexOf('-') == 0))) {
										try{
											ECGData = Float.parseFloat(
													ECGDataStrs[ECGDataStrs.length - 1].replace(';', ' '));
											ECGDataList.add(ECGData);
										}catch(Exception e) {
											e.printStackTrace();
										}
										revTmpStr = "";
									}
									else{
										revTmpStr = ECGDataStrs[ECGDataStrs.length - 1];
									}									
								}
								catch(Exception e) {
									e.printStackTrace();
								}
							}
							
							Message msg = Message.obtain();
							msg.what = 0;
							//msg.obj = new String("正在接收");
							msg.obj = getResources().getString(R.string.stat_btReceving);
							msgHandler.sendMessage(msg);
						}
					}
					Thread.sleep(wait);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
    @SuppressLint("NewApi")
	public boolean onCreateOptionsMenu(Menu menu) {  
        // Inflate the menu; this adds items to the action bar if it is present.  
        super.onCreateOptionsMenu(menu);
        MenuItem exit=menu.add(0, 0, 0, "離開");
        exit.setIcon(R.drawable.ic_action_exit);
        exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM); 
        exit.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				System.exit(0);
				return false; 
			}});
        return true;  
    }
}