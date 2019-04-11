package app.bqlab.multipairingex;

import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

public class ExitService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ExitService.this, "수업이 종료되지 않은 상태에서 앱을 종료할 경우 장치가 오작동할 수 있습니다.", Toast.LENGTH_LONG).show();
            }
        });
        stopSelf();
    }
}
