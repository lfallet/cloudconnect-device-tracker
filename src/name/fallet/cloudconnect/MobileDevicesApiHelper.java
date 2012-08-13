package name.fallet.cloudconnect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import name.fallet.cloudconnect.model.ApiException;
import name.fallet.cloudconnect.model.ConnectionParameters;
import name.fallet.cloudconnect.model.JsonInterpreteur;
import name.fallet.cloudconnect.model.LocatedDevice;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Permet d'accéder à la g8teway de Mobile Devices
 * 
 * @author lfallet
 */
public class MobileDevicesApiHelper {

	private static final String TAG = MobileDevicesApiHelper.class.getSimpleName();

	/**
	 * Partial URL to use when requesting the API. Use {@link String#format(String, Object...)} to insert the desired page title after
	 * escaping it as needed.
	 */

	public static final String ID = "id", MODID = "modid", LAT = "lat", LNG = "lng", TIME = "time";

	// IDEA : mettre la limite max, ou pas de limite ? (25 par défaut ?)
	private static final String LIMIT_NB_DEVICE = "100";

	/** pour passer de 1E5 de Mobile Devices à la précision 1E6 d'Android */
	public static final int COEF_LAT_LNG = 10;

	public final SimpleDateFormat ISO8601_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	private DefaultHttpClient httpClient;
	private Cookie cookie;

	/**
	 * Enable the user to login using credentials in constants at the beginning of this file
	 * 
	 * @param connectionParameters
	 * 
	 * @param login
	 *            your login
	 * @param password
	 *            your password
	 * @param client
	 *            your instance name
	 */
	public void login(ConnectionParameters connectionParameters) throws IOException {
		HttpPost httpPost = new HttpPost(connectionParameters.getUrl() + "sessions.json");

		// ajouter les éléments d'identification
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", connectionParameters.getUser()));
		nvps.add(new BasicNameValuePair("password", connectionParameters.getPassword()));
		nvps.add(new BasicNameValuePair("client", connectionParameters.getClient()));
		httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		Log.i(TAG, "Identification avec " + Arrays.toString(nvps.toArray()));

		HttpResponse response = httpClient.execute(httpPost);
		if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
			List<Cookie> cookies = httpClient.getCookieStore().getCookies();
			if (cookies.isEmpty()) {
				Log.w(TAG, "pas de cookie en retour de l'identification");
			} else {
				cookie = cookies.get(0);
				Log.i(TAG, "cookie : " + cookie.toString());
			}
			response.getEntity().consumeContent(); // pour désallouer il faut consommer
		}
	}

	/**
	 * 
	 * @param connectionParameters
	 * @return
	 * @throws IOException
	 */
	public synchronized Collection<LocatedDevice> recupererPositionVehicules(ConnectionParameters connectionParameters,
			boolean restreindreJourJ) throws ApiException {
		final List<LocatedDevice> vehiculesLocalises = new ArrayList<LocatedDevice>();

		try {
			// pour ouvrir une session si nécessaire
			if (httpClient == null) {
				httpClient = new DefaultHttpClient();
				// first login
				login(connectionParameters);
			}

			JSONArray json = requestUnitsLocation(connectionParameters, restreindreJourJ);
			vehiculesLocalises.addAll(JsonInterpreteur.traiter(json));

		} catch (IOException e) {
			Log.e(TAG, "Erreur IO : " + e.getMessage());
			throw new ApiException("Erreur IO", e);
		} catch (JSONException e) {
			Log.e(TAG, "Erreur JSON : " + e.getMessage());
			throw new ApiException("Erreur JSON", e);
		} finally {
			// on ne ferme pas la socket pour la réutiliser (refresh fréquents possibles)
		}
		return vehiculesLocalises;
	}

	// TODO : check internet availability
	public boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

	/**
	 * Attention, cette méthode est réentrante afin de se relogguer si besoin.
	 * 
	 * @param connectionParameters
	 * 
	 * @param restreindreIntervalleTemps
	 *            limite les données retournées selon la date de la dernière info reçue du device par la gateway
	 * @return A JSONArray representing the result of the request
	 */
	private JSONArray requestUnitsLocation(ConnectionParameters connectionParameters, boolean restreindreIntervalleTemps)
			throws IOException, JSONException {
		// the parameters of the request (including the id_min)
		StringBuffer requestParameters = new StringBuffer("?ret=");
		requestParameters.append(ID + "," + MODID + "," + LAT + "," + LNG + "," + TIME);
		requestParameters.append("&limit=" + LIMIT_NB_DEVICE);

		// FIXME : le paramètre from n'est pas correct (retourne un HTTP 422)
		// ajout d'une restriction sur la date
		if (restreindreIntervalleTemps) {
			final long msDansUneJournee = 1000 * 60 * 60 * 24;
			long jourJms = System.currentTimeMillis() - (System.currentTimeMillis() % msDansUneJournee);
			Date dateDuJour = new Date(jourJms);
			String dateDuJourStr = ISO8601_DATEFORMAT.format(dateDuJour);
			requestParameters.append("&from=");
			requestParameters.append(dateDuJourStr);
		}

		Log.d(TAG, "Paramètres de la requête : " + requestParameters.toString());
		HttpGet httpGet = new HttpGet(connectionParameters.getUrl() + "units.json" + requestParameters);

		HttpResponse httpResponse = httpClient.execute(httpGet);
		Log.d(TAG, "Requête des units : " + httpResponse.getStatusLine());

		if (HttpStatus.SC_OK == httpResponse.getStatusLine().getStatusCode()) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));

			// decode the json data
			JSONArray res = new JSONArray(reader.readLine());
			// Log.d(TAG, res.toString());

			httpResponse.getEntity().consumeContent();
			return res;
		} else {
			// erreur, peut être session expirée, on se relogge et on réessaie
			login(connectionParameters);

			// attention, réentrant !
			// return requestUnitsLocation(restreindreIntervalleTemps);
			return new JSONArray();
		}
	}

	/**
	 * Pour fermer la socket
	 */
	public void closeConnection() {
		if (httpClient != null) {
			httpClient.getConnectionManager().shutdown();
			httpClient = null;
			cookie = null;
		}
	}

}
