package name.fallet.cloudconnect;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class CloudConnectService extends Service {
	
	public CloudConnectService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	// TODO : develop a service for handling data manipulation
	
}
