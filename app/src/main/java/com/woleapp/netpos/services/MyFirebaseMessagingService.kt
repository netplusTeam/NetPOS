package com.woleapp.netpos.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        Timber.e("new token")
        Timber.e(p0)
        super.onNewToken(p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
       Timber.e("From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Timber.e("Message data payload: ${remoteMessage.data}")
        }

        remoteMessage.notification?.let {
            Timber.e("Message Notification Body: ${it.body}")
        }
    }

}