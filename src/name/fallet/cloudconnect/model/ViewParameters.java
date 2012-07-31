package name.fallet.cloudconnect.model;

/**
 * Stocke les paramètres de la vue (coordonnées extrêmes à visualiser)
 * 
 * @author lfallet
 */
public class ViewParameters {

	public int minLat = Integer.MAX_VALUE, maxLat = Integer.MIN_VALUE, minLng = Integer.MAX_VALUE, maxLng = Integer.MIN_VALUE;

	public boolean centrerVueSuiteRafraichissement = false;

	public boolean displayInactiveDevices = true;

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
