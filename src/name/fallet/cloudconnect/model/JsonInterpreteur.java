package name.fallet.cloudconnect.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import name.fallet.cloudconnect.MobileDevicesApiHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Pour interpréter un flux JSON et le convertir en objets du modèle
 * 
 * @author lfallet
 */
public class JsonInterpreteur {

	private static final String TAG = JsonInterpreteur.class.getSimpleName();

	private static final SimpleDateFormat ISO8601_DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	/**
	 * 
	 * @param json
	 *            doit contenir des éléments 'unit'
	 * @throws JSONException
	 */
	public static Collection<? extends LocatedDevice> traiter(JSONArray json) throws JSONException {
		final List<LocatedDevice> vehiculesLocalises = new ArrayList<LocatedDevice>();

		for (int i = 0; i < json.length(); i++) {
			JSONObject jsonUnit = json.getJSONObject(i);
			// Log.v(TAG, jsonUnit.toString());

			JSONObject jsonObj = jsonUnit.getJSONObject("unit");
			int id = jsonObj.getInt(MobileDevicesApiHelper.ID);
			long modid = jsonObj.getLong(MobileDevicesApiHelper.MODID);

			String dateStr = jsonObj.getString(MobileDevicesApiHelper.TIME);
			if (dateStr == null || "null".equals(dateStr)) {
				continue;
			}

			// les dates sont au format ISO 8601 (UTC)
			Date dateInfo = null;

			try {
				// Log.d(TAG, "Réception de " + dateStr);
				dateInfo = ISO8601_DATEFORMAT.parse(dateStr.replace("Z", "+0000"));
			} catch (ParseException e) {
				Log.w(TAG, "Erreur lors du parsing de la date " + dateStr + " : " + e.getMessage());
			}
			LocatedDevice vehLoc = new LocatedDevice(id, modid, dateInfo);

			int lat = jsonObj.optInt(MobileDevicesApiHelper.LAT) * MobileDevicesApiHelper.COEF_LAT_LNG, lng = jsonObj
					.getInt(MobileDevicesApiHelper.LNG) * MobileDevicesApiHelper.COEF_LAT_LNG;
			vehLoc.setLatLng(lat, lng);
			vehiculesLocalises.add(vehLoc);
		}

		return vehiculesLocalises;
	}

}
