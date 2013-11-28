package uk.co.minter.ottrss.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.json.JSONException;
import uk.co.minter.ottrss.App;
import uk.co.minter.ottrss.api.Article;
import uk.co.minter.ottrss.api.Feed;
import uk.co.minter.ottrss.api.RSS;
import uk.co.minter.ottrss.utils.BoundedThreadPoolExecutor;
import uk.co.minter.ottrss.utils.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private Context context;
	private SharedPreferences sp;
	private RSS rss;

	private long sync_start;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.context = context;
		sp = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
		try {
			Log.i("Sync", "Starting");

			init(account.name, AccountManager.get(context).getPassword(account));

			if(!sp.getBoolean("dont_sync_local_state", false))
				updateArticles();

			readFeeds();

			readArticles();

			Editor e = sp.edit();
			e.putLong("last_sync", System.currentTimeMillis());
			e.commit();

			Log.i("Sync", "Done");

		} catch(Exception ex) {
			Log.e("SyncAdapter.onPerformSync", ex);
		}

		// TODO: refresh view
		// TODO: progress updates
	}

	private void init(String username, String password) throws IOException, JSONException {
		sync_start = System.currentTimeMillis();

		rss = new RSS(context, new URL(sp.getString("url", null)), username, password);
		rss.verify = sp.getBoolean("verify", true);
	}

	private void readFeeds() throws IOException, JSONException {
		for(Feed f : rss.feeds())
			f.insertOrUpdate();

		Feed.deleteOld(context, sync_start);
	}

	private void updateArticles() throws IOException, JSONException {
		ArrayList<Integer> unread = new ArrayList<Integer>(), read = new ArrayList<Integer>(), unmarked = new ArrayList<Integer>(), marked = new ArrayList<Integer>();
		Cursor c;

		c = App.db.rawQuery("SELECT id FROM articles WHERE unread = 0", null);
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
			read.add(c.getInt(0));
		c.close();

		c = App.db.rawQuery("SELECT id FROM articles WHERE unread != 0", null);
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
			unread.add(c.getInt(0));
		c.close();

		c = App.db.rawQuery("SELECT id FROM articles WHERE marked = 0", null);
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
			unmarked.add(c.getInt(0));
		c.close();

		c = App.db.rawQuery("SELECT id FROM articles WHERE marked != 0", null);
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
			marked.add(c.getInt(0));
		c.close();

		rss.update(unread, read, unmarked, marked);
	}

	private void readArticles() throws Exception {
		final int THREADS = 4;
		final int COUNT = Integer.parseInt(sp.getString("articles", null));

		BoundedThreadPoolExecutor btpe = new BoundedThreadPoolExecutor(THREADS);

		for(Article a : rss.headlines(COUNT))
			if(!a.existsInDatabase())
				btpe.execute(new Worker(a));
			else
				a.update();

		btpe.shutdown();
		btpe.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

		Article.deleteOld(context, sync_start);
	}

	class Worker implements Runnable {
		private Article article;

		Worker(Article article) {
			this.article = article;
		}

		@Override
		public void run() {
			try {
				ZipInputStream zis = rss.blob(article);
				ZipEntry ze;
				while((ze = zis.getNextEntry()) != null) {
					FileOutputStream o = new FileOutputStream(new File(article.getDir(), ze.getName()));

					byte[] buf = new byte[4096];
					int n;

					while((n = zis.read(buf)) != -1)
						o.write(buf, 0, n);

					o.close();
				}
				zis.close();

				article.insert();

			} catch(Exception ex) {
				Log.e("SyncAdapter.Worker.run", ex);
			}
		}
	}
}
