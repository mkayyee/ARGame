package com.example.argame.Model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sign

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
    private var stopped = false
    private var scanning = true
    private var nextSpawn: Long? = null
    private lateinit var first: NPCSpawnData

    interface NPCSpawnCallback {
        fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int)
        fun notifyAllNPCSpawned()
    }

    init {
        spawnCallback = context as NPCSpawnCallback
        when (level) {
            1 -> {
                npcs = NPCDataForLevels.LevelOne.npcs
                first = npcs.first()
            }
            2 -> {
                npcs = NPCDataForLevels.LevelTwo.npcs
                first = npcs.first()
            }
            10 -> {
                npcs = NPCDataForLevels.LevelTen.npcs
                first = npcs.first()
            }
        }
    }

    fun pause() { paused = true }
    fun resume() { paused = false }
    fun stop() { stopped = true }

    override fun run() {
        nextSpawn = first.spawnTime
        Looper.prepare()
        while (!stopped) {
            if (!paused) {
                try {
                    if (nextSpawn != null) {
                        if (nextSpawn!! <= timer) {
                            spawnNPC(first.type, first.spawnTime, npcs.size - 1, first.id)
                        }
                        if (scanning) {
                            scanning = false
                            handler.postDelayed({
                                Log.d("SHANDLER", "Timer value: $timer")
                                scanning = true
                                timer += 1000
                            }, 1000)
                        }
                    }
                } catch (error: Exception) {
                    Log.d("SHANDLER", "$error")
                }
            } else {
                Log.d("SHANDLER", "spawn handler is paused")
            }
        }
    }


    private fun spawnNPC(type: NPCType, spawnTime: Long, remaining: Int, npcID: Int) {
        Log.d("SHANDLER", "NPC spawned. npcID: $npcID")
        if (npcs.isNotEmpty()) {
            if (npcs.size > 1) {
                first = npcs[1]
                nextSpawn = npcs[1].spawnTime
            } else {
                nextSpawn = null
            }
            npcs.removeAt(0)
        }
        var time = spawnTime - timer
        if (time.sign == -1) {
            time = 0
        }
        handler.postDelayed({
            spawnCallback.notifyNPCSpawned(type, remaining, npcID)
            if (remaining == 0) {
                spawnCallback.notifyAllNPCSpawned()
                nextSpawn = null
            }
        }, time)
    }
}