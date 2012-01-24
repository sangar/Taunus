package no.master.Taunus;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

import no.master.Taunus.TaunusActivity.SensorMsg;

public class SensorStreamController implements Runnable {

	private static final String TAG = "SensorStreamController";
	
	private BufferedOutputStream output;
	private TaunusActivity hostActivity;
	
	private boolean running = true;
	
	public SensorStreamController(BufferedOutputStream output, TaunusActivity hostActivity) {
		this.output = output;
		this.hostActivity = hostActivity;
	}
	
	private void sendString(String str) throws IOException {
		
		ByteArrayOutputStream byteout = 
				new ByteArrayOutputStream(str.length()+1);
		
		DataOutputStream out = new DataOutputStream(byteout);
		
		for (int i = 0; i < str.length(); i++) {
			out.write((char) str.charAt(i));
		}
		out.write((char) '\0');
		
		output.write(byteout.toByteArray(), 0, byteout.size());
		output.flush();
	}
	
	public void stop() {
		running = false;
	}
	
	@Override
	public void run() {
		Log.v(TAG, "Stream started...");
		try {
			while (running) {
//				Log.d(TAG, "Getting message...");
				SensorMsg o = hostActivity.getMessage();
				if (o.getSId() == -104 && o.getLevel() == -104) {
					// Start sending recorded data
					sendString(String.format("104:%d:%d", o.getSId(), o.getLevel()));
				} else if (o.getSId() == -105 && o.getLevel() == -105) {
					// Stop sending recorded data
					sendString(String.format("105:%d:%d", o.getSId(), o.getLevel()));
				} else if (o.getSId() == -110 && o.getLevel() == -110) {
					// Send exit message
					sendString("exit");
				} else {
					// Send normal sensor message
					sendString(String.format("101:%d:%d", o.getSId(), o.getLevel()));
				}	
			}
		} catch (InterruptedException e) {
			Log.d(TAG, "InterruptedException in run()" + e);
		} catch (IOException e) {
			Log.d(TAG, "IOException in run()" + e);
		}
		Log.v(TAG, "Stream ending...");
	}
}
