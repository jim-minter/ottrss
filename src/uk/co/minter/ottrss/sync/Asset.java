package uk.co.minter.ottrss.sync;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import uk.co.minter.ottrss.utils.HTTPDownload;
import uk.co.minter.ottrss.utils.Log;

public class Asset {
	private AssetManager am;
	private URI uri;
	private File f;

	public Asset(AssetManager am, URI uri, File f) {
		this.am = am;
		this.uri = uri;
		this.f = f;
	}

	private String getLink(Element e) {
		if(e.tagName().equals("img"))
			return e.attr("src");
		else if(e.tagName().equals("link"))
			return e.attr("href");
		else
			return null;
	}

	private void setLink(Element e, String s) {
		if(e.tagName().equals("img"))
			e.attr("src", s);
		else if(e.tagName().equals("link"))
			e.attr("href", s);
	}

	private String getHref() {
		return f.getName();
	}

	public void download() throws IOException {
		Log.i("Asset.download", uri.toString());
		HTTPDownload.downloadFile(uri.toURL(), null, true, 10000, f);
	}

	public void write(String s) throws IOException {
		FileWriter w = new FileWriter(f);
		w.write(s);
		w.close();
	}

	public void walkHtml() throws IOException {
		Document doc = Jsoup.parse(f, null);
		boolean changed = false;

		for(Element e : doc.select("img[src], link[href][rel = stylesheet]")) {
			String href = getLink(e).replace(" ", "%20"); // TODO: this isn't right
			if(href.equals("#") || href.startsWith("data:"))
				continue;

			try {
				Asset a = am.download(uri.resolve(href), null);
				setLink(e, a.getHref());
				changed = true;

			} catch(Exception ex) {
				Log.w("Asset.walkHtml", "Exception:" + ex);
			}
		}

		if(changed)
			write(doc.toString());
	}
}
