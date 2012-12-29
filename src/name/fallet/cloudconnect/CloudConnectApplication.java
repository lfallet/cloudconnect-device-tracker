package name.fallet.cloudconnect;

import java.util.ArrayList;
import java.util.Collection;

import name.fallet.cloudconnect.model.LocatedDevice;
import android.app.Application;
import android.util.Log;

/**
 * For centralizing singletons
 * 
 * @author laurent
 */
public class CloudConnectApplication extends Application {
	
	private static final String TAG = "CloudConnect";

	private final Collection<LocatedDevice> locatedDevices = new ArrayList<LocatedDevice>();

	public CloudConnectApplication() {
		super();
		Log.d(TAG, "CloudConnectApplication instanciated");
	}

	public Collection<LocatedDevice> getLocatedDevices() {
		return locatedDevices;
	}

}
