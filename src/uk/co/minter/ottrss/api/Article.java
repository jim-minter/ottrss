package uk.co.minter.ottrss.api;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import java.io.File;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.minter.ottrss.App;
import uk.co.minter.ottrss.utils.FileUtils;

public class Article {
	private Context context;

	public int id;
	public int feed_id;
	public String title;
	public String author;
	public String link;
	public int updated;
	public UnreadState unread;
	public boolean marked;

	public enum UnreadState {
		READ,
		UNREAD,
		STICKY
	};

	public Article(Context context, JSONObject o) throws JSONException {
		this.context = context;

		id = o.getInt("id");
		feed_id = o.getInt("feed_id");
		title = StringEscapeUtils.unescapeHtml4(o.getString("title"));
		author = o.getString("author");
		link = o.getString("link");
		updated = o.getInt("updated");
		marked = o.getBoolean("marked");
		mergeUnreadState(o.getBoolean("unread") ? UnreadState.UNREAD : UnreadState.READ);
	}

	void mergeUnreadState(UnreadState remote) {
		Cursor c = App.db.rawQuery("SELECT unread FROM articles WHERE id = " + id, null);

		if(c.isAfterLast())
			unread = remote;
		else {
			c.moveToFirst();
			UnreadState local = UnreadState.values()[c.getInt(0)];
			unread = local == UnreadState.STICKY ? local : remote;
		}

		c.close();
	}

	public Article(Context context, Cursor c) {
		this.context = context;

		id = c.getInt(c.getColumnIndex("_id"));
		feed_id = c.getInt(c.getColumnIndex("feed_id"));
		title = c.getString(c.getColumnIndex("title"));
		author = c.getString(c.getColumnIndex("author"));
		// link
		updated = c.getInt(c.getColumnIndex("updated"));
		unread = UnreadState.values()[c.getInt(c.getColumnIndex("unread"))];
		marked = c.getInt(c.getColumnIndex("marked")) == 1;
	}

	public void advanceUnreadState() {
		unread = unread == UnreadState.READ ? UnreadState.UNREAD : UnreadState.STICKY;
	}

	public void reverseUnreadState() {
		unread = unread == UnreadState.STICKY ? UnreadState.UNREAD : UnreadState.READ;
	}

	public void markRead() {
		if(unread == UnreadState.UNREAD) {
			unread = UnreadState.READ;
			update();
		}
	}

	private static File getRootDir(Context context) {
		return context.getExternalFilesDir(null);
	}

	public File getDir() {
		File dir = new File(getRootDir(context), Integer.toString(id));
		dir.mkdir();

		return dir;
	}

	public File getIndexHtml() {
		return new File(getDir(), "index.html");
	}

	public String getURL() {
		return "file://" + getIndexHtml().getAbsolutePath();
	}

	private void broadcast() {
		Intent intent = new Intent("articles");
		intent.putExtra("feed_id", feed_id);
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	private static void broadcast(Context context) {
		Intent intent = new Intent("articles");
		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public void insert() {
		ContentValues cv = new ContentValues();
		cv.put("id", id);
		cv.put("feed_id", feed_id);
		cv.put("title", title);
		cv.put("author", author);
		cv.put("link", link);
		cv.put("updated", updated);
		cv.put("unread", unread.ordinal());
		cv.put("marked", marked ? 1 : 0);
		cv.put("synctoken", System.currentTimeMillis());

		App.db.insert("articles", null, cv);
		broadcast();
	}

	public void update() {
		App.db.execSQL("UPDATE articles SET marked = " + (marked ? 1 : 0) + ", unread = " + unread.ordinal() + ", synctoken = " + System.currentTimeMillis() + " WHERE id = " + id);
		broadcast();
	}

	static boolean existsInDatabase(int id) {
		Cursor c = App.db.rawQuery("SELECT id FROM articles WHERE id = " + id, null);
		boolean rv = !c.isAfterLast();
		c.close();
		return rv;
	}

	public boolean existsInDatabase() {
		return existsInDatabase(id);
	}

	public static Cursor getArticlesCursor(int feed_id, boolean show_all) {
		if(show_all)
			return App.db.rawQuery("SELECT id AS _id, feed_id, title, author, updated, unread, marked FROM articles WHERE feed_id = " + feed_id + " ORDER BY updated DESC", null);
		else
			return App.db.rawQuery("SELECT id AS _id, feed_id, title, author, updated, unread, marked FROM articles WHERE unread > 0 AND feed_id = " + feed_id + " ORDER BY updated DESC", null);
	}

	public static void deleteOld(Context context, long synctoken) {
		App.db.delete("articles", "(marked = 0 OR unread = 2) AND synctoken < " + synctoken, null);
		broadcast(context);

		for(File f : getRootDir(context).listFiles()) {
			if(f.getName().equals("log.txt"))
				continue;

			try {
				int id = Integer.parseInt(f.getName());
				if(existsInDatabase(id))
					continue;
			} catch(NumberFormatException ex) {
			}

			FileUtils.deleteRecursive(f);
		}
	}
}
