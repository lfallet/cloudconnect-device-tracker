package name.fallet.cloudconnect.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Stocke les paramètres de la vue (coordonnées extrêmes à visualiser)
 * 
 * @author lfallet
 */
public class ViewParameters {

    public static final Integer DEFAULT_RECENT_VALUE = Integer.valueOf(2);

    public double minLat = -90, maxLat = +90, minLng = -180, maxLng = +180;

    // pas encore implémenté pour le moment
    public boolean centrerVueSuiteRafraichissement = false;

    public boolean displayInactiveDevices = true;

    public List<Integer> highlightedUnitIds = new ArrayList<Integer>();

    // information older than that is considered as not recent
    public Integer relativeTimeRecentDevicesInMinutes;

    private Date lastMidnightDate;
    private Date borderForRecentDate;

    /** */
    public void refreshDateBorders() {
        final long nowInMs = System.currentTimeMillis();
        // la notion de récent est un paramètre de visualisation
        final long borderForRecentInMs = nowInMs - relativeTimeRecentDevicesInMinutes * 60 * 1000L;
        final long lastMidnightMs = nowInMs - (nowInMs % (24 * 60 * 60 * 1000L));
        lastMidnightDate = new Date(lastMidnightMs);
        borderForRecentDate = new Date(borderForRecentInMs);
    }

    /**
     * Etend les coordonnées limites si celles en entrées les dépassent
     */
    public void extendsBoundariesIfNecessary(double lat, double lng) {
        minLat = Math.min(minLat, lat);
        maxLat = Math.max(maxLat, lat);
        minLng = Math.min(minLng, lng);
        maxLng = Math.max(maxLng, lng);
    }

    public Date getLastMidnightDate() {
        return lastMidnightDate;
    }

    public Date getBorderForRecentDate() {
        return borderForRecentDate;
    }

}
