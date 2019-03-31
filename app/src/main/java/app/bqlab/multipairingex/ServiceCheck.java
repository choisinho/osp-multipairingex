package app.bqlab.multipairingex;

import android.app.ActivityManager;
import android.content.Context;

class ServiceCheck {
    static boolean isRunning(Context context, String name) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceInfo.service.getClassName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
