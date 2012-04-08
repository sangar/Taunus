package no.master.Taunus;

import no.master.Taunus.TaunusActivity.SensorMsg;

enum Type {
	START, STOP, SENSOR_DATA, EXIT
};

public class ServerMsg {
	
	private Type type;
	private SensorMsg payload;
	
	public ServerMsg(Type type) {
		this.type = type;
	}
	
	public ServerMsg(Type type, SensorMsg o) {
		this.type = type;
		this.setPayload(o);
	}
	
	public Type getType() {
		return type;
	}
	
	public void setPayload(SensorMsg msg) {
		payload = msg;
	}
	
	public SensorMsg getPayload() {
		return payload;
	}
}
