package uk.co.minter.ottrss.utils;

import android.annotation.SuppressLint;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	private static FileWriter w = null;

	public static void close() {
		if(w != null) {
			try {
				w.close();
			} catch(IOException e) {
			}

			w = null;
		}

		Thread.setDefaultUncaughtExceptionHandler(null);
	}

	public static void init(File f) {
		close();

		try {
			w = new FileWriter(f, true);
		} catch(IOException e) {
		}

		Thread.setDefaultUncaughtExceptionHandler(new UEH());
	}

	@SuppressLint("SimpleDateFormat")
	private static void write(String level, String tag, String msg) {
		if(w == null)
			return;

		try {
			String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date());
			w.write(date + " " + level + " " + tag + " " + msg + "\n");
			w.flush();
		} catch(IOException e) {
		}
	}

	public static void i(String tag, String msg) {
		android.util.Log.i(tag, msg);
		write("INFO", tag, msg);
	}

	public static void w(String tag, String msg) {
		android.util.Log.w(tag, msg);
		write("WARN", tag, msg);
	}

	public static void e(String tag, Throwable ex) {
		android.util.Log.e(tag, "Exception:", ex);
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		write("ERROR", tag, sw.toString());
	}

	private static class UEH implements UncaughtExceptionHandler {
		@Override
		public void uncaughtException(Thread thread, Throwable ex) {
			e("Log.UEH.uncaughtException", ex);
			Runtime.getRuntime().exit(1);
		}
	}
}
