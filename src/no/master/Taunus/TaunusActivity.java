package no.master.Taunus;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * 
 * This class handles the filedescriptor connection to the microconroller.
 * It sends commands to the microcontroller through the sendCommand method.
 * It receives commands from the microcontroller through the run() thread method.
 * 
 * */

public class TaunusActivity extends Activity implements Runnable {

	private static final String TAG = "TaunusActivity";
	
	private static final String ACTION_USB_PERMISSION = "no.master.Taunus.action.USB_PERMISSION";
	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	
	private static final int MESSAGE_SENSOR = 1;
	public static final int MESSAGE_CLIENT = 2;
	public static final byte RELAY_COMMAND = 3;
	
	boolean isSending = false;
	boolean isRecording = false;
	boolean sendRecording = false;
	
	protected class SensorMsg {
		private int sId;
		private int level;
		
		public SensorMsg(int sId, int level) {
			this.sId = sId;
			this.level = level;
		}
		
		public int getSId() {
			return sId;
		}
		
		public int getLevel() {
			return level;
		}
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// setup usb receiver
		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
		
		// Set main layout view
		setContentView(R.layout.main);
		
		enableControls(false);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
//		Intent intent = getIntent();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}
		
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory, mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}
	
	// Connect/Disconnect usb accessory
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "Action: " + action);
			if (ACTION_USB_PERMISSION.equals(action)) {
				// open accessory
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "Permission denied for accessory" + accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				// close accessory
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};
	
	// open connection to accessory
	public void openAccessory(UsbAccessory accessory) {
		// open a file descriptor to arduino board
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "TaunusThread");
			thread.start();
			Log.d(TAG, "Accessory opened.");
			enableControls(true);
		} else {
			Log.d(TAG, "Accessory failed to open.");
		}
	}
	
	public void closeAccessory() {
		enableControls(false);
		
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	protected void enableControls(boolean enable) {}
	
	
	/** This method sends commands to the microcontroller */
	public void sendCommand(byte command, byte target, int value) {
		byte[] buffer = new byte[3];
		if (value > 255)
			value = 255;
		
		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;
		
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Write to microcontroller failed...", e);
			}
		}
	}
	
	private int composeInt(byte hi, byte lo) {
		int val = (int) hi & 0xff;
		val *= 256;
		val += (int) lo & 0xff;
		return val;
	}
	
	/** This is the thread that listens for data from microcontroller */
	@Override
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;
		
		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}
			
			i = 0;
			while (i < ret) {
				int len = ret - i;
				
				switch (buffer[i]) {
					case 0x1:case 0x2:
					case 0x3:case 0x4:
						if (len >= 3) {
							Message m = Message.obtain(mHandler, MESSAGE_SENSOR);
							m.obj = new SensorMsg(buffer[i], composeInt(buffer[i+1], buffer[i+2]));
							mHandler.sendMessage(m);
						}
						i += 3;
						break;
				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}
		}
	} // end run()
/*	
	public Handler getHandler() {
		return this.mHandler;
	}
*/	
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MESSAGE_SENSOR:
					SensorMsg o = (SensorMsg) msg.obj;
					handleSensorMessage(o);
					break;
				case MESSAGE_CLIENT:
					// handle messages from the client application
					ClientMsg c = (ClientMsg) msg.obj;
					handleClientMessage(c);
					Log.d(TAG, String.format("Message received from client...: %s", c.getMsg()));
					break;
			}
		}
	};
	
	// protected handle message methods
	protected void handleSensorMessage(SensorMsg o) {}
	
	protected void handleClientMessage(ClientMsg c) {
		if (c.getMsg().equalsIgnoreCase("start")) {
			if (isSending) {
				Log.d(TAG, "Is sending sensor data...");
				return;
			}
			Log.d(TAG, "Start sending sensor data...");
			isSending = true;
		} else if (c.getMsg().equalsIgnoreCase("stop")) {
			if (isSending) {
				Log.d(TAG, "Stop sending sensor data...");
				isSending = false;
			}
		} else if (c.getMsg().equalsIgnoreCase("rstart")) {
			if (isRecording) {
				Log.d(TAG, "Is recording sensor data");
				return;
			}
			Log.d(TAG, "Start record sensor data");
			isRecording = true;
		} else if (c.getMsg().equalsIgnoreCase("rstop")) {
			if (isRecording) {
				Log.d(TAG, "Stop record sensor data");
				isRecording = false;
				sendRecording = true;
			}
		}
	}
	
	/** Thread message queue */
	static final int MAXQUEUE = 100;
	private ArrayList<SensorMsg> messages = new ArrayList<SensorMsg>();
	
	public synchronized void putMessage(SensorMsg o) throws InterruptedException {
		while (messages.size() == MAXQUEUE) {
			wait();
		}
		messages.add(o);
		notify();
	}
	
	public synchronized SensorMsg getMessage() throws InterruptedException {
//		Log.d(TAG, "Getting message from vector...");
		notify();
		while (messages.size() == 0) {
			wait();
		}
		SensorMsg o = (SensorMsg) messages.get(0);
		messages.remove(o);
		return o;
	}
	
} // end Taunus activity
