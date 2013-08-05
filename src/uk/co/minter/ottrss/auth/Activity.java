package uk.co.minter.ottrss.auth;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import uk.co.minter.ottrss.R;

public class Activity extends AccountAuthenticatorActivity implements OnClickListener {
	private EditText username;
	private EditText password;
	private Button ok;

	// NOTE: ContentResolver.setSyncAutomatically(account, getString(R.string.provider_name), true);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_auth);
		final Intent intent = getIntent();

		username = (EditText)findViewById(R.id.username);
		password = (EditText)findViewById(R.id.password);
		ok = (Button)findViewById(R.id.ok);

		username.setText(intent.getStringExtra("username"));
		ok.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		AccountManager am = AccountManager.get(this);
		Account account = new Account(username.getText().toString(), getString(R.string.account_type));
		am.addAccountExplicitly(account, password.getText().toString(), null);

		Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username.getText().toString());
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, getString(R.string.account_type));
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}
}
