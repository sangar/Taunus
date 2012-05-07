package no.master.Taunus;

public class ClientMsg {
	private int cmdId;
	private int action;
	
	public ClientMsg(int cmdId, int action) {
		this.cmdId = cmdId;
		this.action = action;
	}
	
	public int getCmdId() {
		return cmdId;
	}
	
	public int getAction() {
		return action;
	}
}