package uk.co.minter.ottrss.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import uk.co.minter.ottrss.utils.Fling;

public class ListViewFling extends ListView {
	private GestureDetector gestureDetector;
	private OnFlingListener onFlingListener;

	public ListViewFling(Context context) {
		super(context);
		init(context);
	}

	public ListViewFling(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ListViewFling(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		gestureDetector = new GestureDetector(context, new GestureListener());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(onFlingListener != null && gestureDetector.onTouchEvent(event)) {
			MotionEvent cancel = MotionEvent.obtain(event);
			cancel.setAction(MotionEvent.ACTION_CANCEL);
			super.onTouchEvent(cancel);
			cancel.recycle();
			return true;
		} else
			return super.onTouchEvent(event);
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			int position = pointToPosition((int)e1.getX(), (int)e1.getY());
			long id = getItemIdAtPosition(position);
			View view = getChildAt(position - getFirstVisiblePosition());

			switch(Fling.isFling(e1, e2)) {
				case -1:
					onFlingListener.onFlingLeft(ListViewFling.this, view, position, id);
					return true;
				case 1:
					onFlingListener.onFlingRight(ListViewFling.this, view, position, id);
					return true;
				default:
					return false;
			}
		}
	}

	public void setOnFlingListener(OnFlingListener onFlingListener) {
		this.onFlingListener = onFlingListener;
	}

	public interface OnFlingListener {
		public void onFlingLeft(AdapterView<?> parent, View view, int position, long id);

		public void onFlingRight(AdapterView<?> parent, View view, int position, long id);
	}
}
