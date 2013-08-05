package uk.co.minter.ottrss.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.minter.ottrss.App;
import uk.co.minter.ottrss.api.Article;
import uk.co.minter.ottrss.api.Feed;
import uk.co.minter.ottrss.api.Feed.SyncMode;
import uk.co.minter.ottrss.api.TTRSS;
import uk.co.minter.ottrss.utils.BoundedThreadPoolExecutor;
import uk.co.minter.ottrss.utils.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private Context context;
	private SharedPreferences sp;
	private TTRSS t;

	private long sync_start;
	private boolean is_cat = false;
	private int cat = 0;

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

			setCategory();

			readFeeds();

			readArticles();

			t.logout();

			Log.i("Sync", "Done");

		} catch(Exception ex) {
			Log.w("SyncAdapter.onPerformSync", "Exception:" + ex);
		}

		// TODO: refresh view
		// TODO: progress updates
	}

	private void init(String username, String password) throws IOException, JSONException {
		sync_start = System.currentTimeMillis();

		t = new TTRSS(context, new URL(sp.getString("url", null)));
		t.verify = sp.getBoolean("verify", true);
		t.login(username, password);
	}

	private void setCategory() throws IOException, JSONException {
		String category_name = sp.getString("category", null);
		if(category_name.isEmpty())
			return;

		JSONArray a = t.getCategories();
		for(int i = 0; i < a.length(); i++) {
			JSONObject o = a.getJSONObject(i);
			if(o.getString("title").equalsIgnoreCase(category_name)) {
				is_cat = true;
				cat = o.getInt("id");
				break;
			}
		}
	}

	private void readFeeds() throws IOException, JSONException {
		ArrayList<Feed> feeds = t.getFeeds(is_cat, cat);

		for(Feed f : feeds)
			f.insertOrUpdate();

		Feed.deleteOld(context, sync_start);
	}

	private void updateArticles() throws IOException, JSONException {
		Cursor c;
		StringBuilder sb;
		String separator;

		c = App.db.rawQuery("SELECT id FROM articles WHERE unread = 0", null);
		sb = new StringBuilder();
		separator = "";
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			sb.append(separator).append(c.getInt(0));
			separator = ",";
		}
		if(!separator.equals(""))
			t.updateArticle(sb.toString(), 0, 2);
		c.close();

		c = App.db.rawQuery("SELECT id FROM articles WHERE unread != 0", null);
		sb = new StringBuilder();
		separator = "";
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			sb.append(separator).append(c.getInt(0));
			separator = ",";
		}
		if(!separator.equals(""))
			t.updateArticle(sb.toString(), 1, 2);
		c.close();

		c = App.db.rawQuery("SELECT id FROM articles WHERE marked = 0", null);
		sb = new StringBuilder();
		separator = "";
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			sb.append(separator).append(c.getInt(0));
			separator = ",";
		}
		if(!separator.equals(""))
			t.updateArticle(sb.toString(), 0, 0);
		c.close();

		c = App.db.rawQuery("SELECT id FROM articles WHERE marked != 0", null);
		sb = new StringBuilder();
		separator = "";
		for(c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			sb.append(separator).append(c.getInt(0));
			separator = ",";
		}
		if(!separator.equals(""))
			t.updateArticle(sb.toString(), 1, 0);
		c.close();
	}

	private void readArticles() throws Exception {
		final int THREADS = 4;
		final int COUNT = Integer.parseInt(sp.getString("articles", null));
		final int STEP = 50;

		BoundedThreadPoolExecutor btpe = new BoundedThreadPoolExecutor(THREADS);

		for(int skip = 0; skip < COUNT; skip += STEP) {
			ArrayList<Article> l = t.getHeadlines(is_cat, cat, skip, Math.min(COUNT - skip, STEP));

			for(int i = 0; i < l.size(); i++) {
				Log.i("Sync", Integer.toString(skip + i));
				Article a = l.get(i);

				if(!a.existsInDatabase())
					btpe.execute(new Worker(a));
				else
					a.update();
			}

			if(l.size() < STEP)
				break;
		}

		btpe.shutdown();
		btpe.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

		Article.deleteOld(context, sync_start);
	}

	class Worker implements Runnable {
		private Article article;
		private Feed feed;

		Worker(Article article) {
			this.article = article;
			feed = new Feed(article.feed_id);
		}

		@Override
		public void run() {
			try {
				AssetManager am = new AssetManager(article);
				Asset asset;

				if(feed.syncmode == SyncMode.LINKEDPAGE)
					asset = am.download(new URI(article.link), article.getIndexHtml());
				else {
					asset = am.getIndexHtml();
					asset.write(t.getArticle(article));
				}

				asset.walkHtml();
				article.insert();

			} catch(Exception ex) {
				Log.w("SyncAdapter.Worker.run", "Exception:" + ex);
			}
		}
	}
}
