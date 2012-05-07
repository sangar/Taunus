package no.master.Taunus;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 *  
 * The class TaunusPhone activity handles input from the
 * GUI through the OutputController and relays it to the
 * microcontroller.
 * 
 * */

public class TaunusPhone extends BaseActivity {

	static final String TAG = "TaunusPhone";
	
//	ServerController server;
	NetworkClientController clientConnection;
	private String serverIP = null;
	private int serverPort = -1;
	
	private class AsyncHttpRequest extends AsyncTask<URL, Integer, String> {

		@Override
		protected String doInBackground(URL... params) {
			String serverIP = null;
			try {
				InputStream is = params[0].openStream();
				serverIP = convertStreamToString(is);
				is.close();
			} catch (IOException e) {
				Log.d(TAG, "Exception getting IP address: " + e.getMessage());
			} catch (Exception e) {
				Log.e(TAG, "Seriously bad exception...: " + e.getMessage());
			}
			
			return serverIP;
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
		}
		
		private String convertStreamToString(InputStream is) {
			return new Scanner(is).useDelimiter("\\A").next();
		}
	}
	
	private void fetchHostAddrAndPort() {
		AsyncHttpRequest asd = null;
		
		try {
			asd = (AsyncHttpRequest) new AsyncHttpRequest().execute(new URL("http://folk.uio.no/gardbs/ipreq.php?mode=get"));
			String[] res = asd.get().trim().split(":");
			serverIP = res[0].replaceAll("[^0-9.]", "").trim(); // replace all but . and 0-9
			serverPort = Integer.parseInt(res[1]);
			Log.v(TAG, String.format("Trying server: %s:%d", serverIP, serverPort));
		} catch (InterruptedException e) {
			Log.e(TAG, "InterruptedException: " + e.getMessage());
		} catch (ExecutionException e) {
			Log.e(TAG, "ExecutionException: " + e.getMessage());
		} catch (MalformedURLException e) {
			Log.e(TAG, "MalformedURLException: " + e.getMessage());
		}
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fetchHostAddrAndPort();
		
//		server = new ServerController(this);
//		server.runServer(serverIP);
		clientConnection = new NetworkClientController(this);
		clientConnection.runClient(serverIP, serverPort);
	}
	
	protected void reconnectToServer() {
		// close old connection
		if (clientConnection != null) {
			clientConnection.closeClient();			
			clientConnection = null;
		}
		try {
			// SensorStreamController exit message to server
			putMessage(new ServerMsg(Type.EXIT));
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Start new connection
		fetchHostAddrAndPort();
		clientConnection = new NetworkClientController(this);
		clientConnection.runClient(serverIP, serverPort);
	}
}
