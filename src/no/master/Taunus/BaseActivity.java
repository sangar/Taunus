package no.master.Taunus;

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
			showControls();
		} else if (item.getTitle() == "Reconnect") {
			reconnectToServer();
		} else if (item.getTitle() == "Quit") {
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
		}
	}
	
	protected void reconnectToServer() {}
}
