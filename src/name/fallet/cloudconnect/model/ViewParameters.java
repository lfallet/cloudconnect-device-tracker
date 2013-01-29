package name.fallet.cloudconnect.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Stocke les paramètres de la vue (coordonnées extrêmes à visualiser)
 * 
 * @author lfallet
 */
public class ViewParameters {

	public static final Integer DEFAULT_RECENT_VALUE = Integer.valueOf(2);

	public int minLat = Integer.MAX_VALUE, maxLat = Integer.MIN_VALUE, minLng = Integer.MAX_VALUE, maxLng = Integer.MIN_VALUE;

	// pas encore implémenté pour le moment
	public boolean centrerVueSuiteRafraichissement = false;

	public boolean displayInactiveDevices = true;

	public List<Integer> highlightedUnitIds = new ArrayList<Integer>();

	// information older than that is considered as not recent
	public Integer relativeTimeRecentDevicesInMinutes;

	/**
	 * Etend les coordonnées limites si celles en entrées les dépassent
	 */
	public void extendsBoundariesIfNecessary(int lat, int lng) {
		minLat = Math.min(minLat, lat);
		maxLat = Math.max(maxLat, lat);
		minLng = Math.min(minLng, lng);
		maxLng = Math.max(maxLng, lng);
	}

}
