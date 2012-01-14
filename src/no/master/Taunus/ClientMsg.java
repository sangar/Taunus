package no.master.Taunus;

public class ClientMsg {
	private int cmdId;
	private String msg;
	
	public ClientMsg(int cmdId, String msg) {
		this.cmdId = cmdId;
		this.msg = msg;
	}
	
	public String getMsg() {
		return msg;
	}
}