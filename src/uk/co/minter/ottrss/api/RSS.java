package uk.co.minter.ottrss.api;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.co.minter.ottrss.utils.HTTPDownload;

public class RSS {
	private Context context;

	private URL url;
	private String auth;
	public boolean verify = true;

	private long cookie = 0;

	public RSS(Context context, URL url, String user, String password) {
		this.context = context;
		this.url = url;
		auth = user + ":" + password;
	}

	private JSONObject send(String op) throws IOException, JSONException {
		String s = HTTPDownload.downloadString(new URL(url, op), null, verify, 0, auth);
		return (JSONObject)new JSONTokener(s).nextValue();
	}

	private void sendNoReply(String op, String data) throws IOException, JSONException {
		HTTPDownload.downloadString(new URL(url, op), data, verify, 0, auth);
	}

	public Collection<Feed> feeds() throws IOException, JSONException {
		JSONArray a = send("/feeds").getJSONArray("feeds");

		LinkedList<Feed> rv = new LinkedList<Feed>();
		for(int i = 0; i < a.length(); i++)
			rv.add(new Feed(a.getJSONObject(i)));

		return rv;
	}

	public Collection<Article> headlines(int limit) throws IOException, JSONException {
		JSONObject o = send("/headlines/" + limit);

		cookie = o.getLong("cookie");
		JSONArray a = o.getJSONArray("headlines");

		LinkedList<Article> rv = new LinkedList<Article>();
		for(int i = 0; i < a.length(); i++)
			rv.add(new Article(context, a.getJSONObject(i)));

		return rv;
	}

	public InputStream blob(Article article) throws IOException, JSONException {
		return HTTPDownload.download(new URL(url, "/posts/" + article.id + "/blob"), null, verify, 0, auth);
	}

	public void update(Collection<Integer> unread, Collection<Integer> read, Collection<Integer> unmarked, Collection<Integer> marked) throws IOException, JSONException {
		JSONObject o = new JSONObject();
		o.put("unread", new JSONArray(unread));
		o.put("read", new JSONArray(read));
		o.put("unstarred", new JSONArray(unmarked));
		o.put("starred", new JSONArray(marked));

		sendNoReply("/update", o.toString());
	}

	public void clean() throws IOException, JSONException {
		sendNoReply("/clean/" + cookie, "{}");
	}
}
