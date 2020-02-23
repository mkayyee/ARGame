package com.example.argame.Model

object NPCDataForLevels {
    object LevelOne {
        val npc1 = NPCSpawnData(NPCType.MELEE, 0, 0)
        val npc2 = NPCSpawnData(NPCType.SUPPORT, 10000, 1)
        val npc3 = NPCSpawnData(NPCType.MELEE, 18000, 2)
        val npc4 = NPCSpawnData(NPCType.RANGED, 250000, 3)
        val npc5 = NPCSpawnData(NPCType.RANGED, 30000, 4)
        val npcs = arrayListOf(
            npc1, npc2, npc3, npc4, npc5)
    }

    object LevelTwo {
        val npc1 = NPCSpawnData(NPCType.MELEE, 0,0)
        val npc2 = NPCSpawnData(NPCType.SUPPORT, 10000, 1)
        val npc3 = NPCSpawnData(NPCType.RANGED, 18000, 2)
        val npc4 = NPCSpawnData(NPCType.RANGED, 250000, 3)
        val npc5 = NPCSpawnData(NPCType.RANGED, 30000, 4)
        val npc6 = NPCSpawnData(NPCType.MELEE, 0, 5)
        val npc7 = NPCSpawnData(NPCType.MELEE, 0, 6)
        val npc8 = NPCSpawnData(NPCType.SUPPORT, 250000, 7)

        val npcs = arrayListOf(
            npc1, npc2, npc3, npc4, npc5, npc6, npc7, npc8)
    }

    object LevelTen {
        val npc1 = NPCSpawnData(NPCType.BOSSLVL10, 3000, 100)

        val npcs = arrayListOf(
            npc1)
    }
}