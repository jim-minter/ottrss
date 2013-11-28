package uk.co.minter.ottrss.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {
	public DBOpenHelper(Context context) {
		super(context, "ttrss", null, 1);
	}

	@Override
	public void onConfigure(SQLiteDatabase db) {
		db.setForeignKeyConstraintsEnabled(true);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE feeds (id INTEGER PRIMARY KEY, title TEXT COLLATE NOCASE, articles INTEGER NOT NULL DEFAULT 0, unread INTEGER NOT NULL DEFAULT 0, synctoken INTEGER NOT NULL)");
		db.execSQL("CREATE TABLE articles (id INTEGER PRIMARY KEY, feed_id INTEGER NOT NULL REFERENCES feeds(id) ON DELETE CASCADE, title TEXT, author TEXT, link TEXT, updated INTEGER, unread INTEGER, marked INTEGER, synctoken INTEGER NOT NULL)");
		db.execSQL("CREATE INDEX i_articlesactivity ON articles (feed_id, updated DESC)");
		db.execSQL("CREATE TRIGGER t_articles_delete AFTER DELETE ON articles BEGIN UPDATE feeds SET articles = articles - 1, unread = unread - (OLD.unread > 0) WHERE id = OLD.feed_id; END");
		db.execSQL("CREATE TRIGGER t_articles_insert AFTER INSERT ON articles BEGIN UPDATE feeds SET articles = articles + 1, unread = unread + (NEW.unread > 0) WHERE id = NEW.feed_id; END");
		db.execSQL("CREATE TRIGGER t_articles_update AFTER UPDATE ON articles BEGIN UPDATE feeds SET unread = unread - (OLD.unread > 0) + (NEW.unread > 0) WHERE id = NEW.feed_id; END");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
}
