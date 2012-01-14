package no.master.Taunus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NetworkClientController implements Runnable {

	private static final String TAG = "NetworkClientController";
	
	private String mHost;
	private int mPort;
	
	private Socket socket;
	private BufferedInputStream input;
	private BufferedOutputStream output;
	
	private TaunusActivity mHostActivity;
	private Handler mHandler;
	
	private SensorStreamController streamController;
	
	public NetworkClientController(TaunusActivity hostActivity) {
		this.mHostActivity = hostActivity;
		this.mHandler = hostActivity.mHandler;
	}
	
	/**
	 * Public methods
	 * */
	public void runClient(String host, int port) {
		this.mHost = host;
		this.mPort = port;
		
		Thread t = new Thread(null, this, "ClientThread");
		t.start();
	}
	
	public void closeClient() {
		try {
			closeConnection();
		} catch (IOException e) {
			Log.e(TAG, "Exception in closeClient(): " + e.getMessage());
		}
	}
	
	/**
	 * Private methods
	 * */
	private void setupSensorStream(BufferedOutputStream bos, TaunusActivity activity) {
		Log.d(TAG, "Setting up sensor stream...");
		streamController = new SensorStreamController(bos, activity);
		
		Thread t = new Thread(streamController);
		t.start();
	}
	
	private void initConnection(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		socket.setReuseAddress(true);
	}
	
	private void openStreams() throws IOException {
		input = new BufferedInputStream(socket.getInputStream());
		output = new BufferedOutputStream(socket.getOutputStream());
		output.flush();
	}
	
	private void closeConnection() throws IOException {
		sendString("exit");
		streamController.stop();
		input.close();
		output.flush();
		output.close();
		socket.close();
		Log.v(TAG, String.format("Connection closed..."));
	}
	
	private String recvString() throws IOException {
		
		char c = ' ';
		StringBuilder sb = new StringBuilder();
		
		while(true) {
			c = (char) input.read();
			if (c == '\0') {
				break;
			}
			sb.append(c);
		}
		
		return sb.toString();
	}
	
	private void sendString(String str) throws IOException {
		
		Log.d(TAG, String.format("Sending string to server: %s", str));
		
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
	
	/**
	 * Threads run method
	 * */
	@Override
	public void run() {
		try {
			initConnection(mHost, mPort);
			openStreams();
			setupSensorStream(output, mHostActivity);
			
			sendString("hello");
			
			while (true) {
				// send/receive messages
				String str = recvString();
				Log.d(TAG, String.format("String received: %s", str));
				
				if (str.equalsIgnoreCase("exit")) {
					Log.v(TAG, "Thread exiting...");
					break;
				}
				
				if (str.charAt(0) == '1') {
					Log.v(TAG, "Sending message to mHandler");
					Message m = Message.obtain(mHandler, TaunusActivity.MESSAGE_CLIENT);
					m.obj = new ClientMsg((int) str.charAt(0), str.substring(1));
					mHandler.sendMessage(m);
				}
				
				sendString(str);
			}
			
			closeConnection();
			
		} catch (UnknownHostException e) {
			Log.d(TAG, "UnknownHostException in run(): " + e.getMessage());
		} catch (IOException e) {
			Log.d(TAG, "IOException in run(): " + e.getMessage());
		}

	}
}
