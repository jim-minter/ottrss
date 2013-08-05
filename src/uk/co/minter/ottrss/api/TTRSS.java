package uk.co.minter.ottrss.api;

import android.content.Context;
import java.io.EOFException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.co.minter.ottrss.utils.HTTPDownload;
import uk.co.minter.ottrss.utils.Log;

public class TTRSS {
	private Context context;

	private String sid;
	private URL url;
	public boolean verify = true;

	public TTRSS(Context context, URL url) {
		this.context = context;
		this.url = url;
	}

	private Object send(String op) throws IOException, JSONException {
		return send(op, null);
	}

	private Object send(String op, Map<String, Object> data) throws IOException, JSONException {
		if(data == null)
			data = new HashMap<String, Object>();

		data.put("op", op);

		if(sid != null)
			data.put("sid", sid);

		String s = null;
		final int RETRIES = 5;
		for(int retries = 0; retries < RETRIES; retries++)
			try {
				s = HTTPDownload.downloadString(url, new JSONObject(data).toString(), verify, 0);
				break;
			} catch(EOFException ex) {
				Log.w("TTRSS.send", "EOFException:" + ex);
				if(retries == RETRIES - 1)
					throw ex;
			}

		JSONObject o = (JSONObject)new JSONTokener(s).nextValue();

		if(o.getInt("status") != 0)
			throw new IOException("Unexepected ttrss status code " + o.getInt("status"));

		return o.get("content");
	}

	public void login(String user, String password) throws IOException, JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("user", user);
		m.put("password", password);

		JSONObject o = (JSONObject)send("login", m);
		sid = o.getString("session_id");
	}

	public void logout() throws IOException, JSONException {
		send("logout");
	}

	public ArrayList<Feed> getFeeds(boolean is_cat, int cat) throws IOException, JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("cat_id", is_cat ? cat : -3); // All feeds, excluding virtual feeds (e.g. labels and such)
		m.put("unread_only", false);

		JSONArray a = (JSONArray)send("getFeeds", m);

		ArrayList<Feed> rv = new ArrayList<Feed>();
		for(int i = 0; i < a.length(); i++)
			rv.add(new Feed(a.getJSONObject(i)));

		return rv;
	}

	public ArrayList<Article> getHeadlines(boolean is_cat, int cat, int skip, int limit) throws IOException, JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("feed_id", is_cat ? cat : -4); // All articles
		m.put("limit", limit);
		m.put("skip", skip);
		m.put("is_cat", is_cat);
		m.put("show_excerpt", false);
		m.put("show_content", false);
		m.put("view_mode", "all_articles");
		m.put("include_attachments", false);
		m.put("order_by", "feed_dates");

		JSONArray a = (JSONArray)send("getHeadlines", m);

		ArrayList<Article> rv = new ArrayList<Article>();
		for(int i = 0; i < a.length(); i++)
			rv.add(new Article(context, a.getJSONObject(i)));

		return rv;
	}

	public String getArticle(Article article) throws IOException, JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("article_id", article.id);

		JSONObject o = ((JSONArray)send("getArticle", m)).getJSONObject(0);
		return o.getString("content");
	}

	public JSONArray getCategories() throws IOException, JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("unread_only", false);

		return (JSONArray)send("getCategories", m);
	}

	public void updateArticle(String article_ids, int mode, int field) throws IOException, JSONException {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("article_ids", article_ids);
		m.put("mode", mode);
		m.put("field", field);

		send("updateArticle", m);
	}
}
