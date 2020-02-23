package com.example.argame.Model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/***
 *   An instance of NPCSpawnHandler will be created
 *   at the start of every level.
 *
 *   This class handles notifying the context it was
 *   created in, providing the type of NPC that should be
 *   spawned, after a delay defined in the level's data array.
 */

class NPCSpawnHandler(context: Context, level: Int) : Runnable {

    private var spawnCallback: NPCSpawnCallback
    private lateinit var npcs: Array<NPCSpawnData>
    private var timer: Long = 0
    private var paused = false

    interface NPCSpawnCallback {
        fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int, isLast: Boolean = false)
    }

    init {
        spawnCallback = context as NPCSpawnCallback
        when (level) {
            1 -> npcs = NPCDataForLevels.LevelOne.npcs
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false }

    override fun run() {
        Looper.prepare()
        while (true) {
            if (!paused) {
                Log.d("SHANDLER", "Timer value: $timer")
                // scan through the array every 1 sec
                Handler().postDelayed({
                    npcs.forEach {
                        if (it.spawnTime >= timer) {
                            // notify that the last NPC was spawned, so GameActivity
                            // can tell that the level is over when the last NPC dies.
                            if (it == npcs.last()) {
                                spawnNPC(it.type, it.spawnTime, 0, it.id, true)
                            } else {
                                spawnNPC(it.type, it.spawnTime, npcs.size, it.id)
                            }
                            // if !it.Spawntime >= timer -> safe to assume the rest aren't either.
                        }
                    }
                    timer += 1000
                }, 1000)
            } else {
                Log.d("SHANDLER", "spawn handler is paused")
            }
        }
    }

    private fun spawnNPC(type: NPCType, spawnTime: Long, remaining: Int, npcID: Int, isLast: Boolean = false) {
        Handler().postDelayed({
            if (isLast) {
                spawnCallback.notifyNPCSpawned(type, remaining, npcID,true)
            } else {
                spawnCallback.notifyNPCSpawned(type, remaining, npcID)
            }
        }, spawnTime)
    }
}