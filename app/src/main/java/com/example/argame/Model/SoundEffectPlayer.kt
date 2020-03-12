package com.example.argame.Model

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

class SoundEffectPlayer(context: Context): MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private val context = context
    lateinit var player: MediaPlayer

    fun initSoundEffectPlayer() {
        player = MediaPlayer()
        player.isLooping = false
        player.setVolume(100f, 100f)
    }

    fun playSound(file: Int) {
        try {
            player.apply {
                stop()
                reset()
                setDataSource(context, Uri.parse("android.resource://com.example.argame/" + file))
                setOnPreparedListener(this@SoundEffectPlayer)
                prepareAsync()
            }
        } catch (e: Error) {
            Log.d("SOUNDERR", "Caught error: ${e.localizedMessage}")
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        player.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        mp?.reset()
        return true
    }

}