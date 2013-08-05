package uk.co.minter.ottrss.api;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.minter.ottrss.App;

public class Feed {
	public int id;
	public String title;
	public SyncMode syncmode;

	public enum SyncMode {
		CONTENT,
		LINKEDPAGE
	}

	public Feed(int id) {
		this.id = id;

		Cursor c = App.db.rawQuery("SELECT title, syncmode FROM feeds WHERE id = " + id, null);
		c.moveToFirst();
		title = c.getString(0);
		syncmode = SyncMode.values()[c.getInt(1)];
		c.close();
	}

	public Feed(JSONObject o) throws JSONException {
		id = o.getInt("id");
		title = o.getString("title");
	}

	boolean existsInDatabase() {
		Cursor c = App.db.rawQuery("SELECT id FROM feeds WHERE id = " + id, null);
		boolean rv = !c.isAfterLast();
		c.close();
		return rv;
	}

	public void insertOrUpdate() {
		ContentValues cv = new ContentValues();
		cv.put("id", id);
		cv.put("title", title);
		cv.put("synctoken", System.currentTimeMillis());

		if(!existsInDatabase())
			App.db.insert("feeds", null, cv);
		else
			App.db.update("feeds", cv, "id = " + id, null);
	}

	public void update() {
		App.db.execSQL("UPDATE feeds SET syncmode = " + syncmode.ordinal() + " WHERE id = " + id);
	}

	public static void deleteOld(Context context, long synctoken) {
		App.db.delete("feeds", "synctoken < " + synctoken, null);
	}
}
