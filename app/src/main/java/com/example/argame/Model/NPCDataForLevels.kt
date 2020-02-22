package com.example.argame.Model

object NPCDataForLevels {
    object LevelOne {
        val npc1 = NPCSpawnData(NPCType.MELEE, 0)
        val npc2 = NPCSpawnData(NPCType.MELEE, 2000)
        val npc3 = NPCSpawnData(NPCType.SUPPORT, 4000)
        val npc4 = NPCSpawnData(NPCType.MELEE, 6000)
        val npc5 = NPCSpawnData(NPCType.SUPPORT, 6000)
        val npc6 = NPCSpawnData(NPCType.MELEE, 8000)
        val npc7 = NPCSpawnData(NPCType.RANGED, 10000)
        val npc8 = NPCSpawnData(NPCType.RANGED, 15000)
        val npcs = arrayOf(
            npc1, npc2, npc3, npc4, npc5, npc6, npc7, npc8)
    }
}