package com.gpride.player

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.media3.exoplayer.ExoPlayer

/** Pemutar milik PlaybackService, dipakai widget untuk mengendalikan musik selama layanan hidup. */
object PlayerHolder {
    @Volatile
    var player: ExoPlayer? = null
}

/** Info terakhir yang ditampilkan widget (diisi PlaybackService). */
object WidgetState {
    @Volatile
    var title: String = ""

    @Volatile
    var artist: String = ""

    @Volatile
    var playing: Boolean = false
}

/** Widget layar utama: judul, artis, serta tombol sebelumnya, putar/jeda, dan berikutnya. */
class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetManager.updateAppWidget(appWidgetIds, buildViews(context))
    }

    companion object {
        /** Menggambar ulang semua widget dengan [WidgetState] terbaru. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PlayerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, WidgetState.title.ifEmpty { "Belum ada lagu" })
            views.setTextViewText(R.id.widget_artist, WidgetState.artist.ifEmpty { "Ketuk untuk membuka GPRIDE" })
            views.setImageViewResource(
                R.id.widget_play,
                if (WidgetState.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            )
            views.setOnClickPendingIntent(R.id.widget_play, actionIntent(context, WidgetActionReceiver.ACTION_TOGGLE))
            views.setOnClickPendingIntent(R.id.widget_next, actionIntent(context, WidgetActionReceiver.ACTION_NEXT))
            views.setOnClickPendingIntent(R.id.widget_prev, actionIntent(context, WidgetActionReceiver.ACTION_PREV))
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            return views
        }

        private fun actionIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, WidgetActionReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

/** Menerima ketukan tombol widget dan meneruskannya ke pemutar bila layanan sedang hidup. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val player = PlayerHolder.player ?: return
        when (intent.action) {
            ACTION_TOGGLE -> if (player.isPlaying) player.pause() else player.play()
            ACTION_NEXT -> player.seekToNext()
            ACTION_PREV -> player.seekToPrevious()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.gpride.player.widget.TOGGLE"
        const val ACTION_NEXT = "com.gpride.player.widget.NEXT"
        const val ACTION_PREV = "com.gpride.player.widget.PREV"
    }
}
