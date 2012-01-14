package no.master.Taunus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class BalancePointView extends View {

	private Drawable mBalancePointBackground;
	private Drawable mPoint;
	
	private int fX;
	private int fY;
	private Paint mLabelPaint;
	private String mLabelText;
	
	public BalancePointView(Context context) {
		super(context);
		initBalancePointView(context);
	}

	public BalancePointView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBalancePointView(context);
	}
	
	public void setPosition(int x, int y) {
		fX = x;
		fY = y;
		mLabelText = String.format("%d,%d", fX, fY);
		invalidate();
	}
	
	public int getX() {
		return fX;
	}

	public int getY() {
		return fY;
	}

	private void centerAround(int x, int y, Drawable d) {
		int w = d.getIntrinsicWidth();
		int h = d.getIntrinsicHeight();
		int left = x - w / 2;
		int top = y - h / 2;
		int right = left + w;
		int bottom = top + h;
		d.setBounds(left, top, right, bottom);
	}
	
	private void initBalancePointView(Context context) {
		fX = fY = 0;
		Resources r = context.getResources();
		mBalancePointBackground = r.getDrawable(R.drawable.joystick_background);
		int w = mBalancePointBackground.getIntrinsicWidth();
		int h = mBalancePointBackground.getIntrinsicHeight();
		mBalancePointBackground.setBounds(0, 0, w, h);
		mPoint = r.getDrawable(R.drawable.joystick_normal_holo_dark);
		centerAround(w / 2 - 4, h / 2 + 4, mPoint);
		mLabelPaint = new Paint();
		mLabelPaint.setColor(Color.WHITE);
		mLabelPaint.setTextSize(24);
		mLabelPaint.setAntiAlias(true);
		mLabelPaint.setShadowLayer(1, 2, 2, Color.BLACK);
		setPosition(0, 0);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		mBalancePointBackground.draw(canvas);
		Drawable indicator = mPoint;
		int w = mBalancePointBackground.getIntrinsicWidth();
		int h = mBalancePointBackground.getIntrinsicHeight();
		int x = w / 2 - 4 + fX;
		int y = h / 2 + 4 + fY;
		centerAround(x, y, indicator);
		indicator.draw(canvas);
		canvas.drawText(mLabelText, x + 12, y + 8, mLabelPaint);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(mBalancePointBackground.getIntrinsicWidth(), 
				mBalancePointBackground.getIntrinsicHeight());
	}
}
