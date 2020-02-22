package com.example.argame.Model

import android.content.Context
import android.os.Handler

/***
 *   An instance of NPCSpawnHandler will be created
 *   at the start of every level.
 *
 *   This class handles notifying the context it was
 *   created in, providing the type of NPC that should be
 *   spawned, after a delay defined in the level's data array.
 */

class NPCSpawnHandler(context: Context) {

    private var spawnCallback: NPCSpawnCallback

    interface NPCSpawnCallback {
        fun notifyNPCSpawned(type: NPCType, remaining: Int, npcID: Int, isLast: Boolean = false)
    }

    init {
        spawnCallback = context as NPCSpawnCallback
    }

    fun beginSpawning(array: Array<NPCSpawnData>) {
        var npcsLeft = array.size
        var npcID = 0
        array.forEach {
            npcsLeft --
            // notify that the last NPC was spawned, so GameActivity
            // can tell that the level is over when the last NPC dies.
            if (it == array.last()) {
                spawnNPC(it.type, it.spawnTime, npcsLeft, npcID,true)
            } else {
                spawnNPC(it.type, it.spawnTime, npcsLeft, npcID)
            }
            npcID ++
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