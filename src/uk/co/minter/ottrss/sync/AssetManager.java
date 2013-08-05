package uk.co.minter.ottrss.sync;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import uk.co.minter.ottrss.api.Article;

public class AssetManager {
	private HashMap<URI, Asset> hashmap = new HashMap<URI, Asset>();
	private Article article;
	private int i = -1;

	public AssetManager(Article article) {
		this.article = article;
	}

	private File allocateFile(URI uri) {
		i++;

		String filename = uri.getPath();
		filename = filename.substring(filename.lastIndexOf("/") + 1);
		String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf(".")) : "";

		return new File(article.getDir(), Integer.toString(i) + ext);
	}

	public Asset download(URI uri, File f) throws IOException {
		if(!hashmap.containsKey(uri)) {
			if(f == null)
				f = allocateFile(uri);

			Asset a = new Asset(this, uri, f);
			a.download();
			hashmap.put(uri, a);
		}
		return hashmap.get(uri);
	}

	public Asset getIndexHtml() {
		URI uri = null;
		try {
			uri = new URI("");
		} catch(URISyntaxException ex) {
		}
		return new Asset(this, uri, article.getIndexHtml());
	}
}
