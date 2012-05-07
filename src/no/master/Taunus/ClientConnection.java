package no.master.Taunus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @deprecated See: NetworkClientController.java
 * */
public class ClientConnection implements Runnable {
	
	private static final String TAG = "ClientConnection";
	
	private Socket socket;
	private TaunusActivity hostActivity;
	private Handler mHandler;
	
	private ArrayList<ClientConnection> connlist;
	
	private SensorStreamController streamController;
	
	// buffered Input/Output stream
	private BufferedInputStream input;
	private BufferedOutputStream output;
	
	public ClientConnection(Socket socket, TaunusActivity hostActivity) {
		this.socket = socket;
		this.hostActivity = hostActivity;
		this.mHandler = hostActivity.mHandler;
	}
	
	private void setupSensorStream(BufferedOutputStream bos, TaunusActivity activity) {
		Log.d(TAG, "Setting up sensor stream...");
		streamController = new SensorStreamController(bos, activity);
		
		Thread t = new Thread(streamController);
		t.start();
	} 
	
	private void getStreams() throws IOException {
		input = new BufferedInputStream(socket.getInputStream());
		output = new BufferedOutputStream(socket.getOutputStream());
		output.flush();
	}
	
	private void closeConnection() throws IOException {
		streamController.stop();
		input.close();
		output.flush();
		output.close();
		socket.close();
		connlist.remove(this);
		Log.v(TAG, String.format("Connection closed..."));
	}
	
	public void setConnlist(ArrayList<ClientConnection> connlist) {
		this.connlist = connlist;
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

	@Override
	public void run() {
		try {
			getStreams();
			setupSensorStream(output, hostActivity);
			
			// Recv hello
			String hello = recvString();
			Log.d(TAG, String.format("received message: %s", hello));
			
			sendString("Connection initiated...");
			
			while (true) {
				// send/receive messages
				String str = recvString();
				Log.d(TAG, String.format("String received: %s", str));
				
				if (str.equalsIgnoreCase("exit")) {
					sendString(str);
					Log.v(TAG, "Thread exiting...");
					break;
				}
				
				if (str.charAt(0) == '1') {
					Log.v(TAG, "Sending message to mHandler");
					Message m = Message.obtain(mHandler, TaunusActivity.MESSAGE_SERVER);
//					m.obj = new ClientMsg((int) str.charAt(0), str.substring(1));
					mHandler.sendMessage(m);
				}
				
				sendString(str);
			}
			
			// close connection and exit
			closeConnection();
			
		} catch (IOException e) {
			Log.d(TAG, "IOException in run(): " + e);
		}
	}
}
