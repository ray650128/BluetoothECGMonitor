package com.ray650128.btecgmonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	ActionBar actionBar;
	
	// 藍芽元件
	public static final int REQUEST_ENABLE_BT = 8807;
	public BroadcastReceiver mBTReceiver;
	public static BluetoothSocket mBTSocket;
	public BluetoothAdapter mBTAdapter;
	
	// UI元件
	private Button btn_SearchDev;
	private ToggleButton btn_BTSW;
	private BluetoothDevice mBTDevice;
	private ArrayAdapter<String> adapt_Devs;
	private List<String> list_DevStr = new ArrayList<String>();	
	private ListView list_DevList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        
        actionBar = this.getActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name));
        actionBar.setSubtitle(getResources().getString(R.string.ab_subtitle));
        
        if (mBTAdapter == null) {
        	Toast.makeText(
					MainActivity.this,
					getResources().getString(R.string.stat_noDevice),
					Toast.LENGTH_SHORT).show();
        	this.finish();
        }

        final Dialog dialog = new AlertDialog.Builder(MainActivity.this)
        		.setCancelable(true)
        		.setTitle(getResources().getString(R.string.dialog_title))
        		.setMessage(getResources().getString(R.string.dialog_text))
				.setIcon(getResources().getDrawable(android.R.drawable.ic_dialog_info))
				.create();

        // Set up BroadCast Receiver
        mBTReceiver = new BroadcastReceiver() {
        	public void onReceive(Context context, Intent intent) {
        		String act = intent.getAction();
        		// To see whether the action is that already found devices
        		if(act.equals(BluetoothDevice.ACTION_FOUND)) {
        			// If found one device, get the device object
        			BluetoothDevice tmpDvc = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        			// Put the name & address into a string
        			String tmpDvcStr = tmpDvc.getName() + "|" + tmpDvc.getAddress();
        			if (list_DevStr.indexOf(tmpDvcStr) == -1) {
        				// Avoid duplicate add devices
        				list_DevStr.add(tmpDvcStr);
        				adapt_Devs.notifyDataSetChanged();
        			}
        		}
        		if(act.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
        			Toast.makeText(
							MainActivity.this,
							getResources().getString(R.string.stat_findComplete),
							Toast.LENGTH_SHORT).show();

        			dialog.dismiss();
        		}
       		
        		if (act.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
    				Toast.makeText(
							MainActivity.this,
							getResources().getString(R.string.stat_startFindDevice),
							Toast.LENGTH_SHORT).show();
        		}
         	}
        };
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBTReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(mBTReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBTReceiver, filter);
        
        btn_SearchDev = (Button)findViewById(R.id.btn_SearchDev);
        btn_BTSW = (ToggleButton)findViewById(R.id.btn_BTSW);
        list_DevList = (ListView)findViewById(R.id.list_DevList);
        
        if(mBTAdapter.getState() == BluetoothAdapter.STATE_OFF) {
			btn_BTSW.setChecked(false);
		}

        if(mBTAdapter.getState() == BluetoothAdapter.STATE_ON) {
			btn_BTSW.setChecked(true);
		}

        adapt_Devs = new ArrayAdapter<String>(
				this,
				android.R.layout.simple_list_item_1,
				list_DevStr);

        list_DevList.setAdapter(adapt_Devs);
        
        btn_BTSW.setOnClickListener(new OnClickListener() {
        	public void onClick(View view) {
        		if(btn_BTSW.isChecked()) {
        			if(!mBTAdapter.isEnabled()) {
						mBTAdapter.enable();
					}
        			btn_BTSW.setChecked(true);
        		}
        		
        		if(!btn_BTSW.isChecked()) {
        			if(mBTAdapter.isEnabled()) {
						mBTAdapter.disable();
					}
        			btn_BTSW.setChecked(false);
        		}
        	}
        });
        
        btn_SearchDev.setOnClickListener(new OnClickListener() {
        	public void onClick(View view) {
        		if (mBTAdapter.isDiscovering()) {
        			Toast.makeText(
							MainActivity.this,
							getResources().getString(R.string.stat_btSearching),
							Toast.LENGTH_SHORT
					).show();
        		} else {
    				dialog.show();
    				list_DevStr.clear();
    				adapt_Devs.notifyDataSetChanged();
    				mBTDevice = null;
    				mBTAdapter.startDiscovery();
    			}
        	}
        });
        
        list_DevList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        		if (mBTAdapter == null) {
        			Toast.makeText(
							MainActivity.this,
							getResources().getString(R.string.stat_noDevice),
							Toast.LENGTH_SHORT
					).show();
        		} else {
        			mBTAdapter.cancelDiscovery();			// 取消
        			String str = list_DevStr.get(arg2);
        			String[] dvcValues = str.split("\\|");
        			String dvcAddr = dvcValues[1];
        			UUID dvcUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");	// SPP
        			mBTDevice = mBTAdapter.getRemoteDevice(dvcAddr);
        			// Connect
        			try {
        				mBTSocket = mBTDevice.createRfcommSocketToServiceRecord(dvcUUID);
        				mBTSocket.connect();
        				Intent intent = new Intent(MainActivity.this, ECGMonitorActivity.class);
        				startActivity(intent);
        			} catch(IOException e) {
        				e.printStackTrace();
        			}
        		}        		
        	}
        });
	}

	@Override
	public void onActivityResult(int RequestCode, int ResultCode, Intent data) {
    	switch(RequestCode) {
    	case REQUEST_ENABLE_BT:
    		if(ResultCode == RESULT_OK) {
    			Toast.makeText(this.getApplicationContext(),
							getResources().getString(R.string.stat_btLaunched),
									Toast.LENGTH_SHORT
				).show();
    		} else  if (ResultCode == RESULT_CANCELED) {
					Toast.makeText(this.getApplicationContext(),
							getResources().getString(R.string.stat_btCancelLaunched),
							Toast.LENGTH_SHORT
					).show();
				}
    		break;
    	}
    }    
    @Override
	protected void onDestroy() {
	    this.unregisterReceiver(mBTReceiver);
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
    
    @SuppressLint("NewApi")
	public boolean onCreateOptionsMenu(Menu menu) {  
        // Inflate the menu; this adds items to the action bar if it is present.  
        super.onCreateOptionsMenu(menu);
        MenuItem exit=menu.add(0, 0, 0, getResources().getString(R.string.btn_Exit));
        exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM); 
        exit.setIcon(getResources().getDrawable(R.drawable.ic_action_exit));
        exit.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				System.exit(0);
				return false; 
			}});
        return true;  
    }
}
