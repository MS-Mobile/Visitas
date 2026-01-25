package com.msmobile.visitas

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.msmobile.visitas.extension.toClockString
import com.msmobile.visitas.util.DispatcherProvider
import com.msmobile.visitas.util.TimerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class VisitasService : Service() {
    private val timerScope by lazy { CoroutineScope(provider.io) }

    @Inject
    lateinit var timerManager: TimerManager

    @Inject
    lateinit var provider: DispatcherProvider

    private val notificationManager: NotificationManager
        get() {
            return getSystemService(NotificationManager::class.java)
        }

    private val notificationBuilder: Notification.Builder by lazy {
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.visitas_small_icon)
            .setContentTitle(getString(R.string.field_service_in_progress))
            .setContentText(getString(R.string.app_name))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
    }

    override fun onCreate() {
        super.onCreate()

        // Create the notification channel for Android Oreo and above
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        val notification = notificationBuilder.build()
        notificationManager.createNotificationChannel(channel)
        startForeground(NOTIFICATION_ID, notification)

        timerManager.timer
            .onEach { state ->
                val fieldServiceInProgressTime = state.elapsedTime.toClockString()
                val updatedNotification = notificationBuilder
                    .setContentText(fieldServiceInProgressTime)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
            .map { state -> state is TimerManager.TimerState.Running }
            .distinctUntilChanged()
            .onEach { isFieldServiceInProgress ->
                val updatedNotification = notificationBuilder
                    .setNotificationAction(applicationContext, isFieldServiceInProgress)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
            .launchIn(timerScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle the play and pause actions
        when (intent?.action) {
            PLAY_ACTION -> {
                timerManager.resume()
            }

            PAUSE_ACTION -> {
                timerManager.pause()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        timerScope.cancel()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun Notification.Builder.setNotificationAction(
        context: Context,
        isFieldServiceInProgress: Boolean
    ): Notification.Builder {
        val (intentAction, actionText) = if (isFieldServiceInProgress) {
            PAUSE_ACTION to getString(R.string.pause_timer)
        } else {
            PLAY_ACTION to getString(R.string.start_timer)
        }
        val actionIntent = Intent(context, VisitasService::class.java).apply {
            action = intentAction
        }
        val pendingIntent = PendingIntent.getService(context, 0, actionIntent, FLAG_IMMUTABLE)
        val notificationAction =
            Notification.Action.Builder(null, actionText, pendingIntent).build()

        return this.setActions(notificationAction)
    }

    companion object {
        private const val CHANNEL_NAME = "visitas_channel"
        private const val CHANNEL_ID = "visitas_channel"
        private const val NOTIFICATION_ID = 1
        private const val PLAY_ACTION = "play"
        private const val PAUSE_ACTION = "pause"
    }
}