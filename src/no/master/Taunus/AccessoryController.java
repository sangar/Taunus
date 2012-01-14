package no.master.Taunus;

import android.content.res.Resources;
import android.view.View;

public abstract class AccessoryController {
	
	protected TaunusActivity mHostActivity;
	
	public AccessoryController(TaunusActivity hostActivity) {
		mHostActivity = hostActivity;
	}
	
	protected View findViewById(int id) {
		return mHostActivity.findViewById(id);
	}
	
	protected Resources getResources() {
		return mHostActivity.getResources();
	}
	
	void accessoryAttached() {
		onAccessoryAttached();
	}

	abstract protected void onAccessoryAttached();
}
