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
	private ArrayList<SensorMsg> recordedSensorData;
	
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
 			recordedSensorData = new ArrayList<SensorMsg>();
			for (int i=0; i<600; i++) {
				recordedSensorData.add(new SensorMsg(-104, -104)); // indicate start recorded data sending
				recordedSensorData.add(new SensorMsg(1, i));
				recordedSensorData.add(new SensorMsg(2, (i*2) % 1024));
				recordedSensorData.add(new SensorMsg(3, (i*3) % 1024));
				recordedSensorData.add(new SensorMsg(4, (i*4) % 1024));
			}
			recordedSensorData.add(new SensorMsg(-105, -105)); // indicate stop recorded data sending
			sendRecordedData(recordedSensorData);
			
			showControls();
		} else if (item.getTitle() == "Reconnect") {
			reconnectToServer();
		} else if (item.getTitle() == "Quit") {
			try {
				putMessage(new SensorMsg(-110, -110));
				Thread.sleep(1000);
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
		if (mInputController != null)
			mInputController.setSensorValue(m.getSId(), m.getLevel());
		
		if (isSending) {
// 			Log.d(TAG, "handleSensorMessage isSending");
			try {
				putMessage(m);
			} catch (InterruptedException e) {
				Log.d(TAG, "InterruptedException isSending " + e);
			}
		} else if (isRecording) {
			if (recordedSensorData == null) {
				Log.v(TAG, "Setting up recorded sensor data Vector");
				recordedSensorData = new ArrayList<SensorMsg>();
			}
			if (recordedSensorData.size() == 0) {
				Log.v(TAG, "Adding -104 start sending recorded data message");
				// Indicate recorded data sending
				recordedSensorData.add(new SensorMsg(-104, -104));
			}
			recordedSensorData.add(m);
		} else if (sendRecording) {
			sendRecordedData(recordedSensorData);
			sendRecording = false;
		}
	}
	
	protected void reconnectToServer() {}
	
	/** 
	 * Private methods
	 * */
	private void sendRecordedData(ArrayList<SensorMsg> recordedData) {
		while (recordedData.size() != 0) {
			SensorMsg o = recordedData.get(0);
			recordedData.remove(o);
			try {
				putMessage(o);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// Indicate stop for recorded data sending
		try {
			Log.v(TAG, "Putting -105 stop sending recorded data message");
			putMessage(new SensorMsg(-105, -105));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
