package uk.co.minter.ottrss.auth;

import android.accounts.AccountManager;
import android.content.Intent;
import android.os.IBinder;

public class Service extends android.app.Service {
	private static final Object lock = new Object();
	private static Authenticator authenticator = null;

	@Override
	public void onCreate() {
		synchronized(lock) {
			if(authenticator == null)
				authenticator = new Authenticator(this);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if(intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT))
			return authenticator.getIBinder();
		else
			return null;
	}
}
