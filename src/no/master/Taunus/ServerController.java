package no.master.Taunus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.util.Log;

/**
 * @deprecated See: NetworkClientController.java
 * */
public class ServerController implements Runnable {

	private static final String TAG = "ServerController";
	
	private final int MAX_CON = 10;
	private int port = 14253;
	private TaunusActivity hostActivity;
	
	
	private ServerSocket server;
	
	private ArrayList<ClientConnection> connlist;
	
	public ServerController(TaunusActivity hostActivity) {
		this.hostActivity = hostActivity;
	}
	
	public void runServer(String serverIP) {
		connlist = new ArrayList<ClientConnection>();
		
		// setup socket, (input/output streams is done in connections)
		try {
			server = new ServerSocket(port);
			server.setReuseAddress(true);
		} catch (IOException e) {
			Log.e(TAG, "Could not create server socket.");
			Log.e(TAG, "exception: " + e.getMessage());
		}
		
		// start listening thread
		Thread t = new Thread(null, this, "ServerThread");
		t.start();

		if(serverIP == null)
			serverIP = server.getInetAddress().getHostAddress();
		
		Log.v(TAG, "InetAddress hostAddress: " + server.getInetAddress().getHostAddress());
		Log.v(TAG, String.format("Server is running at: %s, %s", serverIP, server.getLocalPort()));
	}
	
	public ArrayList<ClientConnection> getConnectionList() {
		return connlist;
	}
	
	private void listenForConnection() throws IOException {
		Log.v(TAG, "Waiting for connections...");
		while (true) {
			Socket cli = server.accept();
			if (connlist.size() < MAX_CON) {
				initConnection(cli);
			}
		}
	}
	
	private void initConnection(Socket cli) throws IOException {
		ClientConnection conn = new ClientConnection(cli, hostActivity);
		conn.setConnlist(connlist);
		connlist.add(conn);
		
		// Start the connection thread
		Thread t = new Thread(conn);
		t.start();

		Log.v(TAG, "Connection started...");
	}
	
	@Override
	public void run() {
		try {
			listenForConnection();
		} catch (IOException e) {
			Log.e(TAG, "IOException in ServerController run()");
		}
	}
}
