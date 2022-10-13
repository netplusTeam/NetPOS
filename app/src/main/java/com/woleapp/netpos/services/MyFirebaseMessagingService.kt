package com.woleapp.netpos.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.woleapp.netpos.R
import com.woleapp.netpos.model.TransactionResponseFromFireBase
import com.woleapp.netpos.ui.activities.MainActivity
import com.woleapp.netpos.util.*
import com.woleapp.netpos.util.RandomNumUtil.formatCurrencyAmountUsingCurrentModule
import com.woleapp.netpos.worker.SaveTransactionFromFirebaseMessagingServiceToDbWorker
import timber.log.Timber

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        Timber.e("new token")
        Timber.e(p0)
        sendRegistrationToServer(p0)
        super.onNewToken(p0)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG_NOTIFICATION_RECEIVED, "From: ${remoteMessage.from}")
        Log.d(TAG_NOTIFICATION_RECEIVED_2, "From: $remoteMessage")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val transactionNotificationFromFirebase = remoteMessage.data["TransactionNotification"]
            Log.d(TAG_NOTIFICATION_RECEIVED, "Message data payload: ${remoteMessage.data}")
            Log.d(
                TAG_NOTIFICATION_RECEIVED_2,
                "Message data Key: $transactionNotificationFromFirebase"
            )

            transactionNotificationFromFirebase?.let {
                scheduleJobToSaveTransactionToDatabase(it)
            }
        }

        // Check if message contains a notification payload.
//        remoteMessage.notification?.let {
//            Log.d(TAG_NOTIFICATION_RECEIVED, "Message Notification Body: ${it.body}")
//        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        remoteMessage.data["TransactionNotification"]?.let {
            val transaction = Gson().fromJson(it, TransactionResponseFromFireBase::class.java)
            val transactionAmount = transaction.amount ?: 0
            Log.d("1234567890", it)
            sendNotification("${transactionAmount.formatCurrencyAmountUsingCurrentModule()} Received \nFrom: ${transaction.depositorAccountName}   (${transaction.depositorBankName})")
        }

        Timber.e("From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Timber.e("Message data payload: ${remoteMessage.data}")
        }

        remoteMessage.notification?.let {
            Timber.e("Message Notification Body: ${it.body}")
        }
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.action = STRING_FIREBASE_INTENT_ACTION
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(TAG_NOTIFICATION_RECEIVED_FROM_BACKEND, true)
        val pendingIntent = PendingIntent.getActivity(
            this,
            INT_FIREBASE_PENDING_INTENT_REQUEST_CODE /* Request code */,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.transacion_received))
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setSmallIcon(R.drawable.ic_netpos_logo)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.transacion_received),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    private fun sendRegistrationToServer(token: String?) {
        // Implement this method to send token to your app server.
        Log.d(TAG_NEW_TOKEN_RECEIVED, "sendRegistrationTokenToServer($token)")
    }

    private fun scheduleJobToSaveTransactionToDatabase(transactionFromFireBaseInStringFormat: String) {
        val inputData: Data = Data.Builder()
            .putString(WORKER_INPUT_PBT_TRANSACTION_TAG, transactionFromFireBaseInStringFormat)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work =
            OneTimeWorkRequest.Builder(SaveTransactionFromFirebaseMessagingServiceToDbWorker::class.java)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
        WorkManager.getInstance(this)
            .beginWith(work)
            .enqueue()
    }
}
