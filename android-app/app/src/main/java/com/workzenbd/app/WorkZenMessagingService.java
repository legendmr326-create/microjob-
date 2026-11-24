package com.workzenbd.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class WorkZenMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "workzen_updates";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = message.getNotification() != null
                ? message.getNotification().getTitle() : message.getData().get("title");
        String body = message.getNotification() != null
                ? message.getNotification().getBody() : message.getData().get("body");

        if (title == null || title.trim().isEmpty()) title = "WorkZen";
        if (body == null || body.trim().isEmpty()) body = "You have a new update.";

        showNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // Token upload to the WorkZen backend will be wired in the secure server integration step.
        getSharedPreferences("workzen_push", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .apply();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WorkZen updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("WorkZen job, deposit, withdrawal and account updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}