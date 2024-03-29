package app.bqlab.febtumbler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.security.AlgorithmConstraints;

public class MyService extends Service {

    public static int temp, ml, goalTemp, goalMl;
    public static boolean isConnected, isTempBuzzed, isMlBuzzed;

    NotificationManager notificationManager;
    NotificationChannel notificationChannel;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String content = intent.getStringExtra("content");
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent p = PendingIntent.getActivity(this, 0, i, 0);
        Notification notification = new NotificationCompat.Builder(this, "알림")
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(p)
                .build();
        startForeground(1, notification);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected) {
                    if (goalTemp != 0) {
                        if (temp == goalTemp && !isTempBuzzed) {
                            isTempBuzzed = true;
                            makeNotification("목표 온도에 도달했습니다.");
                        }
                        if (!(temp == goalTemp) && isTempBuzzed)
                            isTempBuzzed = false;
                    }
                    Log.d("무게", String.valueOf(ml));
                    if (goalMl != 0) {
                        if (ml >= goalMl && !isMlBuzzed) {
                            goalMl = 0;
                            isMlBuzzed = true;
                            makeNotification("목표 량에 도달했습니다.");
                        }
                        if (!(ml >= goalMl) && isMlBuzzed)
                            isMlBuzzed = false;
                    }
                }
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void init() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel("em", "알림", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("목표 온도에 도달했을 때 발생하는 알림입니다.");
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 100, 200});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public void makeNotification(String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.notify(0, new NotificationCompat.Builder(this, "em")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(content)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build());
        } else {
            notificationManager.notify(0, new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(content)
                    .setWhen(System.currentTimeMillis())
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build());
        }
    }
}
