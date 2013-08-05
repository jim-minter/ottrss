package uk.co.minter.ottrss.utils;

import android.view.MotionEvent;

public class Fling {
	public static int isFling(MotionEvent e1, MotionEvent e2) {
		float dx = e2.getX() - e1.getX(), dy = e2.getY() - e1.getY();
		if(Math.abs(dx) < 100 || Math.abs(dx) < 2 * Math.abs(dy))
			return 0;

		return (int)Math.signum(dx);
	}
}
