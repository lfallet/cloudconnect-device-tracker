package name.fallet.cloudconnect.model;

import android.text.TextUtils;

/**
 * Stocke les identifiants
 * 
 * @author lfallet
 */
public class ConnectionParameters {

	private String user, password, client, url;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return true if one mandatory field (user, password, client, url) is blank
	 */
	public boolean isMissingAtLeastOneMandatoryValue() {
		return TextUtils.isEmpty(user) || TextUtils.isEmpty(password) || TextUtils.isEmpty(client) || TextUtils.isEmpty(url);
	}

}
