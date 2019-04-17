package is.zi.anders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScanBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, BluetoothLeService.class);
        service.setAction(intent.getAction());
        context.startForegroundService(service);
    }
}