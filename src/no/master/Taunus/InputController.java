package no.master.Taunus;

import java.text.DecimalFormat;

import android.graphics.Point;
import android.util.Log;
import android.widget.TextView;

public class InputController extends AccessoryController {

	private static final String TAG = "InputController";
	
	private TextView mSensor1View;
	private TextView mSensor1RawView;
	private TextView mSensor2View;
	private TextView mSensor2RawView;
	private TextView mSensor3View;
	private TextView mSensor3RawView;
	private TextView mSensor4View;
	private TextView mSensor4RawView;
	
	private BalancePointView mBalanceView;
	
	private final DecimalFormat mSensorValueFormatter = new DecimalFormat("##.#");
	
	// points for balance view coordinate system
	private Point leftFront, leftBack, rightFront, rightBack;
	
	public InputController(TaunusActivity hostActivity) {
		super(hostActivity);
		mSensor1View = (TextView) findViewById(R.id.sensor1PercentValue);
		mSensor1RawView = (TextView) findViewById(R.id.sensor1RawValue);
		mSensor2View = (TextView) findViewById(R.id.sensor2PercentValue);
		mSensor2RawView = (TextView) findViewById(R.id.sensor2RawValue);
		mSensor3View = (TextView) findViewById(R.id.sensor3PercentValue);
		mSensor3RawView = (TextView) findViewById(R.id.sensor3RawValue);
		mSensor4View = (TextView) findViewById(R.id.sensor4PercentValue);
		mSensor4RawView = (TextView) findViewById(R.id.sensor4RawValue);
		
		mBalanceView = (BalancePointView) findViewById(R.id.balanceView);
		
		// check: leftFront = leftBack = rightFront = rightBack = new Point(0, 0);
		leftFront = new Point(0, 0);
		leftBack = new Point(0, 0);
		rightFront = new Point(0, 0);
		rightBack = new Point(0, 0);
	}

	@Override
	protected void onAccessoryAttached() {
		// sets up switches in DemoKit
	}
	
	public void onSensorChange(int sId, int value) {
		setSensorValue(sId, value);
	}

	public void setSensorValue(int sId, int sensorValueFromArduino) {
		
		int balancePoint = sensorValueFromArduino / 2;//(127 * sensorValueFromArduino / 1024);
		double percentValue = (100 * (double) sensorValueFromArduino / 1024.0);
		String rawInput = String.valueOf(sensorValueFromArduino);
		String percentInput = mSensorValueFormatter.format(percentValue);
		
		switch (sId) {
			case 0x1:
				mSensor1View.setText(percentInput);
				mSensor1RawView.setText(rawInput);
//				- -
				leftFront.set(-balancePoint, -balancePoint);
				break;
			case 0x2:
				mSensor2View.setText(percentInput);
				mSensor2RawView.setText(rawInput);
//				- +
				leftBack.set(-balancePoint, balancePoint);
				break;
			case 0x3:
				mSensor3View.setText(percentInput);
				mSensor3RawView.setText(rawInput);
//			 	+ -
				rightFront.set(balancePoint, -balancePoint);
				break;
			case 0x4:
				mSensor4View.setText(percentInput);
				mSensor4RawView.setText(rawInput);
//				+ +
				rightBack.set(balancePoint, balancePoint);
				break;
		}
		
		// midpoint front
		int x1 = (leftFront.x + rightFront.x) / 2;
		int y1 = (leftFront.y + rightFront.y) / 2;
		
		// midpoint back
		int x2 = (leftBack.x + rightBack.x) / 2;
		int y2 = (leftBack.y + rightBack.y) / 2;
		
		// midpoint front and back
		int x = (x1 + x2) / 2;
		int y = (y1 + y2) / 2;
		
		mBalanceView.setPosition(x, y);
	}
}
