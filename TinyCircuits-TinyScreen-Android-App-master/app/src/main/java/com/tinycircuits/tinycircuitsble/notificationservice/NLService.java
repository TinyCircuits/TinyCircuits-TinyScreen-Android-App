package com.tinycircuits.tinycircuitsble.notificationservice;

import android.app.Notification;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.tinycircuits.tinycircuitsble.bluetoothservice.BluetoothLeService;
import com.tinycircuits.tinycircuitsble.tools.IntentData;

/**
 * Created by Nic on 8/27/2014.
 *
 * The nature of this service is such that if it is allowed to listen for notifications,
 * it will run. Since it will be running for a long time, the amount of actual processing
 * within the service is minimized. Data is send to the BluetoothLeService instead where
 * it will be processed because it is needed, not because there is a notification.
 */
public class NLService extends NotificationListenerService {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        return START_STICKY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn){
        Notification notification = sbn.getNotification();
        if(notification != null){
            String message = " ";
            String title = " ";
            try {
                message = notification.extras.getCharSequence(NotificationAttrs.EXTRA_TEXT).toString();
                title = notification.extras.getCharSequence(NotificationAttrs.EXTRA_TITLE).toString();
            } catch (NullPointerException e) {
                //return;
            }
            String contextName = sbn.getPackageName();

            Intent intent = new Intent(BluetoothLeService.NOTIFICATION_POSTED);
            intent.putExtra(IntentData.NOTIFICATION_SENDER,contextName);
            intent.putExtra(IntentData.NOTIFICATION_DATA, message);
            intent.putExtra(IntentData.NOTIFICATION_TITLE, title);
            sendBroadcast(intent);

        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn){

        /*
        Notification notification = sbn.getNotification();
        if(notification != null){
            String text = notification.extras.getCharSequence(NotificationAttrs.EXTRA_TEXT).toString();
            Debug.LogMessage(text);
        }
        */
    }

}
