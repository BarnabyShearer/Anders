package is.zi.anders;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.util.Log;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class BluetoothLeService extends Service {
    final static String ACTION_SCAN_RESULT = "RESULT";
    private final static String ACTION_SNOOZE = "SNOOZE";
    private final static String ACTION_INC = "+";
    private final static String ACTION_DEC = "-";
    private final static String CHANNEL_ID = "42";
    private static final int NOTIFICATION_ID = 42;
    private int a, b = 0;
    private int target = 62;
    private BluetoothGatt mBluetoothGatt;
    private NotificationManager notificationManager;
    private Notification.Builder notification;
    private Notification.Builder alarm;
    private long snooze = 0;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelName = getResources().getString(R.string.app_name);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        Intent decIntent = new Intent(this, ScanBroadcastReceiver.class);
        decIntent.setAction(ACTION_DEC);
        PendingIntent decPendingIntent = PendingIntent.getBroadcast(this, 0, decIntent, 0);

        Intent incIntent = new Intent(this, ScanBroadcastReceiver.class);
        incIntent.setAction(ACTION_INC);
        PendingIntent incPendingIntent = PendingIntent.getBroadcast(this, 0, incIntent, 0);

        Intent snoozeIntent = new Intent(this, ScanBroadcastReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);

        notification = new Notification.Builder(BluetoothLeService.this, CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setContentText(getString(R.string.connecting))
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_icon_min)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_notification_clear_all),
                        getString(R.string.snooze),
                        snoozePendingIntent
                ).build())
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                        getString(R.string.dec),
                        decPendingIntent
                ).build())
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_next),
                        getString(R.string.inc),
                        incPendingIntent
                ).build());

        alarm = new Notification.Builder(BluetoothLeService.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_icon_min)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_notification_clear_all),
                        getString(R.string.snooze),
                        snoozePendingIntent
                ).build())
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                        getString(R.string.dec),
                        decPendingIntent
                ).build())
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(this, android.R.drawable.ic_media_next),
                        getString(R.string.inc),
                        incPendingIntent
                ).build());
        startForeground(NOTIFICATION_ID, notification.build());
    }

    private void update(Notification.Builder notification) {
        if (notification == null) {
            return;
        }
        if (new Date().getTime() <= snooze) {
            notification.setContentText(a + "°C " + b + "°C");
            Notification built = notification.build();
            built.actions[0].title = getString(R.string.unsnooze);
            notificationManager.notify(BluetoothLeService.NOTIFICATION_ID, built);
        } else {
            notification.setContentText(a + "°C " + b + "°C target: " + target + "°C");
            Notification built = notification.build();
            notificationManager.notify(BluetoothLeService.NOTIFICATION_ID, built);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return Service.START_REDELIVER_INTENT;
        }
        Log.d("OnStart", "" + intent);
        switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_SCAN_RESULT:
                Log.w("BLE", "New device detected");
                BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(getString(R.string.mac));
                mBluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.i("BLE", "Connected");
                            gatt.discoverServices();
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.w("BLE", "Disconnected");
                            notificationManager.cancel(BluetoothLeService.NOTIFICATION_ID);
                            stopSelf();
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        BluetoothGattService service = gatt.getService(
                                UUID.fromString("00001000-0000-1000-8000-00805F9B34FB")
                        );
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                                UUID.fromString("00001002-0000-1000-8000-00805F9B34FB")
                        );
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        );
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt,
                                                        BluetoothGattCharacteristic characteristic) {
                        byte[] data = characteristic.getValue();
                        synchronized (BluetoothLeService.this) {
                            a = (data[0] ^ data[2] ^ data[8]) * 10 + (data[0] ^ data[2] ^ data[9]);
                            b = (data[0] ^ data[2] ^ data[15]) * 10 + (data[0] ^ data[2] ^ data[16]);

                            if (a < target) {
                                update(notification);
                            } else {
                                update(alarm);
                            }
                        }
                    }
                });
                break;
            case ACTION_SNOOZE:
                synchronized (this) {
                    if (new Date().getTime() <= snooze) {
                        snooze = 0;
                    } else {
                        snooze = new Date().getTime() + 300000; //5min
                    }
                    update(notification);
                }
                break;
            case ACTION_INC:
                synchronized (this) {
                    target++;
                    update(notification);
                }
                break;
            case ACTION_DEC:
                synchronized (this) {
                    target--;
                    update(notification);
                }
                break;
        }
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

}
