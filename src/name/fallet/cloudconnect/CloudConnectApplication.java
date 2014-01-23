package name.fallet.cloudconnect;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * For centralizing singletons
 * 
 * @author laurent
 */
public class CloudConnectApplication extends Application {

    private static final String TAG = "CloudConnect";

    private static Context appContext;
    
    public CloudConnectApplication() {
        super();
        Log.d(TAG, "CloudConnectApplication instanciated");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }

}
