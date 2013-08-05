package uk.co.minter.ottrss;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import uk.co.minter.ottrss.utils.DBOpenHelper;
import uk.co.minter.ottrss.utils.Log;

public class App extends Application {
	public static SQLiteDatabase db;

	@Override
	public void onCreate() {
		super.onCreate();
		System.setProperty("http.keepAlive", "false");
		db = new DBOpenHelper(this).getWritableDatabase();

		Log.init(new File(getExternalFilesDir(null), "log.txt"));
	}
}
