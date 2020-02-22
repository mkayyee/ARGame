package com.example.argame.Model

object NPCDataForLevels {
    object LevelOne {
        val npc1 = NPCSpawnData(NPCType.MELEE, 0)
        val npc2 = NPCSpawnData(NPCType.SUPPORT, 10000)
        val npc3 = NPCSpawnData(NPCType.MELEE, 18000)
        val npc4 = NPCSpawnData(NPCType.RANGED, 250000)
        val npc5 = NPCSpawnData(NPCType.RANGED, 30000)
        val npcs = arrayOf(
            npc1, npc2, npc3, npc4, npc5)
    }
}