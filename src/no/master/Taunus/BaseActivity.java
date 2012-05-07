package no.master.Taunus;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * 
 * The base activity class relays/handles all input from 
 * microcontroller to GUI via the InputController.
 *  
 * */

public class BaseActivity extends TaunusActivity {
	
	private static final String TAG = "BaseActivity";
	
	private InputController mInputController;
	private ArrayList<ServerMsg> recordedSensorData;
	
	public BaseActivity() {
		super();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mAccessory != null) {
			showControls();
		} else {
			hideControls();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Simulate");
		menu.add("Reconnect");	
		menu.add("Quit");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle() == "Simulate") {
/* Generate simulated data */
			if (mAccessory == null) {
				recordedSensorData = new ArrayList<ServerMsg>();
				recordedSensorData.add(new ServerMsg(Type.START)); // indicate start recorded data sending
 				for (int i=0; i<600; i++) {
					recordedSensorData.add(new ServerMsg(Type.SENSOR_DATA, new SensorMsg(1, i)));
					recordedSensorData.add(new ServerMsg(Type.SENSOR_DATA, new SensorMsg(2, (i*2) % 1024)));
					recordedSensorData.add(new ServerMsg(Type.SENSOR_DATA, new SensorMsg(3, (i*3) % 1024)));
					recordedSensorData.add(new ServerMsg(Type.SENSOR_DATA, new SensorMsg(4, (i*4) % 1024)));
				}
				recordedSensorData.add(new ServerMsg(Type.STOP)); // indicate stop recorded data sending
				sendRecordedData(recordedSensorData);
			}
/* End simulated data */
			showControls();
		} else if (item.getTitle() == "Reconnect") {
			reconnectToServer();
		} else if (item.getTitle() == "Quit") {
			try {
				putMessage(new ServerMsg(Type.EXIT));
				Thread.sleep(250);
			} catch (InterruptedException e) {}
			finish();
			System.exit(0);
		}
		return true;
	}
	
	protected void enableControls(boolean enable) {
		if (enable) {
			showControls();
		} else {
			hideControls();
		}
	}
	
	protected void hideControls() {
		setContentView(R.layout.no_device);

		mInputController = null;		
	}
	
	protected void showControls() {
		setContentView(R.layout.main);
		
		mInputController = new InputController(this);
		mInputController.accessoryAttached();
	}
	
	/**
	 * Handle sensor message
	 * */
	protected void handleSensorMessage(SensorMsg m) {
		// Set value to balance view
		if (mInputController != null)
			mInputController.setSensorValue(m.getSId(), m.getLevel());
		
		// Either streaming or recording
		if (isSending) {
// 			Log.d(TAG, "handleSensorMessage isSending");
			ServerMsg msg = new ServerMsg(Type.SENSOR_DATA);
			msg.setPayload(m);
			try {
				putMessage(msg);
			} catch (InterruptedException e) {
				Log.d(TAG, "InterruptedException isSending " + e);
			}
		} else if (isRecording) {
			if (recordedSensorData == null) {
				Log.v(TAG, "Setting up recorded sensor data array");
				recordedSensorData = new ArrayList<ServerMsg>();
			}
			if (recordedSensorData.size() == 0) {
				Log.v(TAG, "Adding START sending recorded data message");
				// Indicate recorded data sending
				recordedSensorData.add(new ServerMsg(Type.START));
			}
			ServerMsg msg = new ServerMsg(Type.SENSOR_DATA);
			msg.setPayload(m);
			recordedSensorData.add(msg);
		} else if (sendRecording) {
			recordedSensorData.add(new ServerMsg(Type.STOP));
			sendRecordedData(recordedSensorData);
			sendRecording = false;
		}
	}
	
	// Used in TaunusPhone class
	protected void reconnectToServer() {}
	
	/** 
	 * Private methods
	 * */
	private void sendRecordedData(ArrayList<ServerMsg> recordedData) {
		while (recordedData.size() != 0) {
			ServerMsg o = recordedData.get(0);
			recordedData.remove(o);
			try {
				putMessage(o);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
