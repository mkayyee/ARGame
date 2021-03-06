package com.example.argame.Model.NPC

object NPCDataForLevels {

    fun getNPCForLevelCount(level: Int) : Int {
        return when (level) {
            1 -> LevelOne.npcs.size
            2 -> LevelTwo.npcs.size
            3 -> LevelThree.npcs.size
            4 -> LevelFour.npcs.size
            5 -> LevelFive.npcs.size
            6 -> LevelSix.npcs.size
            7 -> LevelSeven.npcs.size
            8 -> LevelEight.npcs.size
            9 -> LevelNine.npcs.size
            10 -> LevelTen.npcs.size
            else -> 0
        }
    }

    object LevelOne {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.SUPPORT,
            5000,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.MELEE,
            10000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.RANGED,
            15000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.RANGED,
            20000,
            4
        )
        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5
        )
    }

    object LevelTwo {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.SUPPORT,
            10000,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.RANGED,
            18000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.RANGED,
            25000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.RANGED,
            30000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.MELEE,
            0,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.MELEE,
            0,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.SUPPORT,
            40000,
            7
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8
        )
    }

    object LevelThree {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.SUPPORT,
            1000,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.RANGED,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.RANGED,
            2000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.MELEE,
            10000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.MELEE,
            10000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.SUPPORT,
            20000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.SUPPORT,
            20000,
            8
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9
        )
    }

    object LevelFour {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.SUPPORT,
            1000,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.RANGED,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.RANGED,
            2000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.MELEE,
            10000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.MELEE,
            10000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.SUPPORT,
            18000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.SUPPORT,
            20000,
            8
        )
        val npc10 = NPCSpawnData(
            NPCType.MELEE,
            20000,
            9
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9,
            npc10
        )
    }
    object LevelFive {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.MELEE,
            1000,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.MELEE,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.MELEE,
            3000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.MELEE,
            4000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.SUPPORT,
            10000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            8
        )
        val npc10 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            9
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9,
            npc10
        )
    }
    object LevelSix {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.MELEE,
            0,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.RANGED,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.RANGED,
            2000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.SUPPORT,
            2000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            8
        )
        val npc10 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            9
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9,
            npc10
        )
    }

    object LevelSeven {
        val npc1 = NPCSpawnData(
            NPCType.MELEE,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.SUPPORT,
            1000,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.MELEE,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.SUPPORT,
            3000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.MELEE,
            4000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.SUPPORT,
            10000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.MELEE,
            10000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.MELEE,
            15000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.MELEE,
            15000,
            8
        )
        val npc10 = NPCSpawnData(
            NPCType.MELEE,
            15000,
            9
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9,
            npc10
        )
    }

    object LevelEight {
        val npc1 = NPCSpawnData(
            NPCType.RANGED,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.RANGED,
            0,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.MELEE,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.MELEE,
            3000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.SUPPORT,
            4000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.RANGED,
            10000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.SUPPORT,
            10000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            8
        )
        val npc10 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            9
        )
        val npc11 = NPCSpawnData(
            NPCType.SUPPORT,
            15000,
            10
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9,
            npc10,
            npc11
        )
    }

    object LevelNine {
        val npc1 = NPCSpawnData(
            NPCType.RANGED,
            0,
            0
        )
        val npc2 = NPCSpawnData(
            NPCType.RANGED,
            0,
            1
        )
        val npc3 = NPCSpawnData(
            NPCType.MELEE,
            2000,
            2
        )
        val npc4 = NPCSpawnData(
            NPCType.MELEE,
            2000,
            3
        )
        val npc5 = NPCSpawnData(
            NPCType.SUPPORT,
            4000,
            4
        )
        val npc6 = NPCSpawnData(
            NPCType.SUPPORT,
            4000,
            5
        )
        val npc7 = NPCSpawnData(
            NPCType.RANGED,
            12000,
            6
        )
        val npc8 = NPCSpawnData(
            NPCType.RANGED,
            12000,
            7
        )
        val npc9 = NPCSpawnData(
            NPCType.MELEE,
            12000,
            8
        )
        val npc10 = NPCSpawnData(
            NPCType.MELEE,
            12000,
            9
        )
        val npc11 = NPCSpawnData(
            NPCType.SUPPORT,
            12000,
            10
        )

        val npcs = arrayOf(
            npc1,
            npc2,
            npc3,
            npc4,
            npc5,
            npc6,
            npc7,
            npc8,
            npc9,
            npc10,
            npc11
        )
    }

    object LevelTen {
        val npc1 = NPCSpawnData(
            NPCType.BOSSLVL10,
            3000,
            100
        )

        val npcs = arrayOf(
            npc1
        )
    }
}