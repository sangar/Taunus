package no.master.Taunus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This activity receives USB_DEVICE_ATTACHED events from 
 * usb service and starts the application
 * */

public class UsbAccessoryActivity extends Activity {

	static final String TAG = "UsbAccessoryActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = TaunusLaunch.createIntent(this);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "unable to start Taunus activity", e);
		}
		finish();
	}
}
