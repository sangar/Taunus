package no.master.Taunus;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

public class TaunusLaunch extends Activity {
	
	static final String TAG = "TaunusLaunch";
	
	static Intent createIntent(Activity activity) {
		Display display = activity.getWindowManager().getDefaultDisplay();
		int maxExtent = Math.max(display.getWidth(), display.getHeight());
		
		Intent intent;
		if (maxExtent > 1200) {
			Log.i(TAG, "Screen is large...");
			// launch table class here
		} // else
		// launch phone
		intent = new Intent(activity, TaunusPhone.class);
		
		return intent;
	}
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = createIntent(this);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
        		| Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.e(TAG, "unable to launch Taunus activity", e);
		}
        finish();
    }
}