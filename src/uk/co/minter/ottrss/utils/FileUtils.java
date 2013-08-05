package uk.co.minter.ottrss.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {
	public static void deleteRecursive(File f) {
		if(f.isDirectory())
			for(File ff : f.listFiles())
				deleteRecursive(ff);

		f.delete();
	}

	public static String readFile(File f) throws IOException {
		StringBuilder sb = new StringBuilder();
		FileReader r = new FileReader(f);
		char[] buf = new char[4096];
		int n;
		while((n = r.read(buf, 0, 4096)) != -1)
			sb.append(buf, 0, n);
		r.close();
		return sb.toString();
	}
}
