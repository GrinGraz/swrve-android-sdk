package com.swrve.sdk.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.swrve.sdk.SwrvePushSDK;
import com.swrve.sdk.SwrvePushService;
import com.swrve.sdk.qa.SwrveQAUser;

import java.util.Iterator;

public class GcmSwrvePushService implements SwrvePushService {

    private SwrvePushSDK pushSDK;

    public GcmSwrvePushService() {
        pushSDK = SwrvePushSDK.getInstance();
        if (pushSDK != null) {
            pushSDK.setService(this);
        }
    }

    /**
     * Override this function to process notifications in a different way.
     *
     * @param msg
     */
    @Override
    public void processNotification(final Bundle msg) {
        if (pushSDK != null) {
            pushSDK.processNotification(msg);

            // Notify bound clients
            Iterator<SwrveQAUser> iter = SwrveQAUser.getBindedListeners().iterator();
            String pushId = SwrvePushSDK.getPushId(msg);
            while (iter.hasNext()) {
                SwrveQAUser sdkListener = iter.next();
                sdkListener.pushNotification(pushId, msg);
            }
        }
    }

    /**
     * Override this function to decide when to show a notification.
     *
     * @return true when you want to display notifications.
     */
    @Override
    public boolean mustShowNotification() {
        return true;
    }

    /**
     * Override this function to change the way a notification is shown.
     *
     * @param notificationManager
     * @param notification
     * @return the notification id so that it can be dismissed by other UI elements.
     */
    @Override
    public int showNotification(NotificationManager notificationManager, Notification notification) {
        return pushSDK.showNotification(notificationManager, notification);
    }

    /**
     * Override this function to change the attributes of a notification.
     *
     * @param msgText
     * @param msg
     * @return the notification builder.
     */
    @Override
    public NotificationCompat.Builder createNotificationBuilder(String msgText, Bundle msg) {
        return pushSDK.createNotificationBuilder(msgText, msg);
    }

    /**
     * Override this function to change the way the notifications are created.
     *
     * @param msg
     * @param contentIntent
     * @return the notification that will be displayed.
     */
    @Override
    public Notification createNotification(Bundle msg, PendingIntent contentIntent) {
        return pushSDK.createNotification(msg, contentIntent);
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     *
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg push message payload
     * @return pending intent.
     */
    @Override
    public PendingIntent createPendingIntent(Bundle msg) {
        return pushSDK.createPendingIntent(msg);
    }

    /**
     * Override this function to change what the notification will do
     * once clicked by the user.
     *
     * Note: sending the Bundle in an extra parameter
     * "notification" is essential so that the Swrve SDK
     * can be notified that the app was opened from the
     * notification.
     *
     * @param msg
     * @return the notification intent.
     */
    @Override
    public Intent createIntent(Bundle msg) {
        return pushSDK.createIntent(msg);
    }
}
