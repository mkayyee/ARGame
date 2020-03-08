package com.example.argame.Model

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.util.Log
import com.example.argame.R

class BackgroundMusic : Service(), MediaPlayer.OnPreparedListener {
    internal lateinit var player: MediaPlayer
    var running = false
    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        Log.d("MUSIC", "START")
        player = MediaPlayer()
        player.setDataSource(this, Uri.parse("android.resource://com.example.argame/" + R.raw.background))
        player.isLooping = true // Set looping
        player.setVolume(100f, 100f)
    }

    fun isBackgroundMusicRunning () : Boolean {
        return running
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("MUSIC", "ONSTART")
        player.apply {
            setOnPreparedListener(this@BackgroundMusic)
            prepareAsync()
        }
        return START_STICKY
    }

    override fun onPrepared(player: MediaPlayer) {
        player.start()
    }

    override fun onDestroy() {
        player.stop()
        player.release()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        running = false
        stopSelf()
    }

}