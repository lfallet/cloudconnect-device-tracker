package name.fallet.cloudconnect.model;

import java.util.Date;

/**
 * 
 * @author lfallet
 */
public class LocatedDevice {

	private final int id;
	private final long modid;

	/** mesuré en microdegrés (degrés * 1E6) */
	private int lat = NOT_INITIALIZED;
	/** mesuré en microdegrés (degrés * 1E6) */
	private int lng = NOT_INITIALIZED;

	private String statut;
	private Date dateInformations;

	private static final int NOT_INITIALIZED = 0;

	public LocatedDevice(int id, long modid, Date dateInformations) {
		super();
		this.id = id;
		this.modid = modid;
		this.dateInformations = dateInformations;
	}

	public int getLat() {
		return lat;
	}

	/**
	 * 
	 * @param lat
	 *            exprimé en microdegrés (degrés * 1E6)
	 * @param lng
	 *            exprimé en microdegrés (degrés * 1E6)
	 */
	public void setLatLng(int lat, int lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public int getLng() {
		return lng;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public Date getDateInformations() {
		return dateInformations;
	}

	public void setDateInformations(Date dateInformations) {
		this.dateInformations = dateInformations;
	}

	public int getId() {
		return id;
	}

	public long getModid() {
		return modid;
	}

	public boolean isLocalise() {
		return lng != NOT_INITIALIZED || lat != NOT_INITIALIZED;
	}

}
