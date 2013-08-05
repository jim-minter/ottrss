package uk.co.minter.ottrss.sync;

import android.content.Intent;
import android.os.IBinder;

public class Service extends android.app.Service {
	private static final Object lock = new Object();
	private static SyncAdapter syncAdapter = null;

	@Override
	public void onCreate() {
		synchronized(lock) {
			if(syncAdapter == null)
				syncAdapter = new SyncAdapter(getApplicationContext(), false);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if(intent.getAction().equals("android.content.SyncAdapter"))
			return syncAdapter.getSyncAdapterBinder();
		else
			return null;
	}
}
