package com.example.argame.Model

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class SoundEffectPlayer(context: Context): MediaPlayer.OnPreparedListener {
    private val context = context
    lateinit var player: MediaPlayer

    fun initSoundEffectPlayer() {
        player = MediaPlayer()
        player.isLooping = false
        player.setVolume(100f, 100f)
    }

    fun playSound(file: Int) {
        player.apply {
            stop()
            reset()
            setDataSource(context, Uri.parse("android.resource://com.example.argame/" + file))
            setOnPreparedListener(this@SoundEffectPlayer)
            prepareAsync()
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        player.start()
    }

}