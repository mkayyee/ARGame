package com.example.argame.Model

object NPCDataForLevels {
    object LevelOne {
        val npc1 = NPCSpawnData(NPCType.MELEE, 0, 0)
        val npc2 = NPCSpawnData(NPCType.SUPPORT, 5000, 1)
        val npc3 = NPCSpawnData(NPCType.MELEE, 10000, 2)
        val npc4 = NPCSpawnData(NPCType.RANGED, 15000, 3)
        val npc5 = NPCSpawnData(NPCType.RANGED, 20000, 4)
        val npcs = arrayListOf(
            npc1, npc2, npc3, npc4, npc5)
    }
}