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

class NPCSpawnHandler(context: Context, level: Int, private val handler: Handler) : Runnable {

    private var spawnCallback: NPCSpawnCallback
    private lateinit var npcs: ArrayList<NPCSpawnData>
    private var timer: Long = 0
    private var paused = false
    private var scanning = true

    interface NPCSpawnCallback {
        fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int, isLast: Boolean = false)
        fun notifyAllNPCSpawned()
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
                if (scanning) {
                    scanning = false
                    handler.postDelayed({
                        Log.d("SHANDLER", "Timer value: $timer")
                        if (npcs.isNotEmpty()) {
                            spawnIfReady(npcs)
                        }
                        scanning = true
                        timer += 1000
                    }, 1000)
                }
            } else {
                Log.d("SHANDLER", "spawn handler is paused")
            }
        }
    }

    private fun spawnIfReady(array: ArrayList<NPCSpawnData>) {
        val first = array.first()
        var second: NPCSpawnData? = null
        if (array.size > 1) {
            second = array[1]
        }
        if (first.spawnTime <= timer) {
            if (second != null && second.spawnTime == array.first().spawnTime) {
                spawnNPC(first.type, first.spawnTime, array.size, first.id)
                array.removeAt(1)
                spawnIfReady(array)
            } else {
                spawnNPC(first.type, first.spawnTime, array.size, first.id)
                array.removeAt(1)
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