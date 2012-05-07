package no.master.Taunus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
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
	
//	private TaunusActivity mHostActivity;
	private TaunusPhone mHostActivity;
	private Handler mHandler;
	
	private SensorStreamController streamController;
	
	public NetworkClientController(TaunusPhone hostActivity) {
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
		SocketAddress hostAddr = new InetSocketAddress(host, port);
		socket = new Socket();
		socket.connect(hostAddr, 5000);
		socket.setReuseAddress(true);
	}
	
	private void openStreams() throws IOException {
		input = new BufferedInputStream(socket.getInputStream());
		output = new BufferedOutputStream(socket.getOutputStream());
		output.flush();
	}
	
	private void closeConnection() throws IOException {
		sendString("102:301"); // stop:connection, needed by this.closeClient() and in run()
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
			
			sendString("101:301"); // send start:connection hello message
			
			boolean isRunning = true;
			
			while (isRunning) {
				// send/receive messages
				String str = recvString();
				Log.d(TAG, String.format("String received: %s", str));
				
//				if (str.equalsIgnoreCase("exit")) {
//				if (str.equalsIgnoreCase("102:301")) { // stop:connection
//					Log.v(TAG, "Thread exiting...");
//					break;
//				}
/*				
				if (str.charAt(0) == '1') {
					Log.v(TAG, "Sending message to mHandler");
					Message m = Message.obtain(mHandler, TaunusActivity.MESSAGE_SERVER);
					m.obj = new ClientMsg((int) str.charAt(0), str.substring(1));
					mHandler.sendMessage(m);
				}
*/
				try {
					String msg[] = str.split(":");
					int cmd = Integer.parseInt(msg[0]);
					int action = Integer.parseInt(msg[1]);
					
					Log.d(TAG, String.format("Message received from server: cmd: %d, action: %d", cmd, action));
					
					// Obtain new message from client to server 
					Message m = Message.obtain(mHandler, TaunusActivity.MESSAGE_SERVER);
					switch (cmd) {
						case 101: // start
							switch (action) {
								case 302: // ping
									Log.d(TAG, "Ping received, sending pong.");
									sendString("101:303"); // send pong
									continue;
								default:
									m.obj = new ClientMsg(cmd, action);
									break;
							}
							break;
						case 102: // stop
							switch (action) {
								case 301: // connection
									Log.v(TAG, "Stop connection received...");
									isRunning = false;
									continue;
								default:
									m.obj = new ClientMsg(cmd, action);
									break;
							}
							break;
					}
					
					mHandler.sendMessage(m);
				} catch (Exception e) {
					sendString("Error: invalid command format");
				}
				
				sendString(String.format("OK: %s", str)); 
			}
			
			Log.v(TAG, "Closing connection...");
			closeConnection();
			
		} catch (UnknownHostException e) {
			Log.d(TAG, "UnknownHostException in run(): " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "IOException in run(): " + e.getMessage());
//			Log.d(TAG, String.format("host: %s, port: %d", mHost, mPort));
//			mHostActivity.reconnectToServer();
		}
	} // end run()
} // end class
