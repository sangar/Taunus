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

	public SensorStreamController(BufferedOutputStream output,
			TaunusActivity hostActivity) {
		this.output = output;
		this.hostActivity = hostActivity;
	}

	private void sendString(String str) throws IOException {

		ByteArrayOutputStream byteout = new ByteArrayOutputStream(
				str.length() + 1);

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
				// Get message from message queue in TaunusActivity
				ServerMsg s = hostActivity.getMessage();
				String msgToSrv = "";
				switch (s.getType()) {
					case START:
						// Start sending recorded data
						msgToSrv = String.format("101:%d:%d", 203, 401);
						break;
					case STOP:
						// Stop sending recorded data
						msgToSrv = String.format("102:%d:%d", 203, 401);
						break;
					case EXIT:
						// Send exit message
						msgToSrv = "102:301";
						running = false;
						break;
					case SENSOR_DATA:
					default:
						// Send normal sensor message
						SensorMsg o = s.getPayload();
						msgToSrv = String.format("401:%d:%d", o.getSId(), o.getLevel());
				}
				
				if (output != null) {
					sendString(msgToSrv);
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
