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

    object LevelTwo {
        val npc1 = NPCSpawnData(NPCType.MELEE, 0)
        val npc2 = NPCSpawnData(NPCType.SUPPORT, 10000)
        val npc3 = NPCSpawnData(NPCType.RANGED, 18000)
        val npc4 = NPCSpawnData(NPCType.RANGED, 250000)
        val npc5 = NPCSpawnData(NPCType.RANGED, 30000)
        val npc6 = NPCSpawnData(NPCType.MELEE, 0)
        val npc7 = NPCSpawnData(NPCType.MELEE, 0)
        val npc8 = NPCSpawnData(NPCType.SUPPORT, 250000)

        val npcs = arrayOf(
            npc1, npc2, npc3, npc4, npc5, npc6, npc7, npc8)
    }

    object LevelTen {
        val npc1 = NPCSpawnData(NPCType.BOSSLVL10, 3000)

        val npcs = arrayOf(
            npc1)
    }
}