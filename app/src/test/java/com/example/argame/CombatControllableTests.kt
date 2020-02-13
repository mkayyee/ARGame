package com.example.argame

import com.example.argame.Model.NPC
import com.example.argame.Model.Player
import com.example.argame.Model.minimumAP
import com.example.argame.Model.minimumMaxHealth
import org.junit.Test
import org.junit.Assert.*

const val positiveAP = 5.0
const val negativeAP = positiveAP * -1.0
const val positiveHP = 200.0
const val negativeHP = positiveHP * -1.0
const val npcName = "NPC 1"
const val playerName = "Player 1"

class CombatControllableTests {

    @Test
    fun testDealDamage() {
        val player = Player(positiveAP, playerName, positiveHP)
        val npc = NPC(positiveAP, npcName, positiveHP)
        // dealDamage() should deal damage equal to the input --
        // multiplied by the damage dealer's attackPower
        val npcStartHealth = npc.getStatus().currentHealth
        val playerAP = player.getStatus().attackPower
        val damage = 2.0
        val expectedNPCHealth = npcStartHealth - playerAP * damage
        player.dealDamage(damage, npc)
        // Verifying that a correct amount of health was removed from the target
        assertEquals(expectedNPCHealth, npc.getStatus().currentHealth, 0.0)

        // A dead CombatControllable should not be able to do damage
        // kill player
        npc.dealDamage(player.getStatus().maxHealth, player)
        // verify that the player is dead
        assertEquals(false, player.getStatus().isAlive)
        // get npc health before damage attempt
        val npcHealthBefore = npc.getStatus().currentHealth
        // try damaging
        player.dealDamage(5.0, npc)
        // confirm that it failed
        assertEquals(npcHealthBefore, npc.getStatus().currentHealth, 0.0)
    }

    @Test
    fun testNegativeConstructorValues() {
        // Calling both constructors with negative values
        val player = Player(negativeAP, playerName, negativeHP)
        val npc = NPC(negativeAP, npcName, negativeHP)
        // Verifying that they are converted into positive values
        assertEquals(positiveHP, npc.getStatus().currentHealth, 0.0)
        assertEquals(positiveHP, player.getStatus().currentHealth, 0.0)
        assertEquals(positiveAP, player.getStatus().attackPower, 0.0)
        assertEquals(positiveAP, npc.getStatus().attackPower, 0.0)
    }

    @Test
    fun testInvalidConstructorValues() {
        val lessThanOne = 0.5
        val zero = 0.0
        // Calling both constructors with smaller than allowed values
        val player = Player(lessThanOne, playerName, lessThanOne)
        val npc = NPC(zero, npcName, zero)
        // confirming that the values are set correctly (according to minimum values)
        assertNotEquals(npc.getStatus().currentHealth, zero, 0.0)
        assertNotEquals(npc.getStatus().attackPower, zero, 0.0)
        assertNotEquals(player.getStatus().currentHealth, lessThanOne, 0.0)
        assertNotEquals(player.getStatus().attackPower, lessThanOne, 0.0)
        // minimumAP is const val = 1.0 defined in CombatControllable.kt
        assertEquals(npc.getStatus().attackPower, minimumAP, 0.0)
        assertEquals(player.getStatus().attackPower, minimumAP, 0.0)
        // just like minimumMaxHealth
        assertEquals(npc.getStatus().maxHealth, minimumMaxHealth, 0.0)
        assertEquals(player.getStatus().maxHealth, minimumMaxHealth, 0.0)

    }

    @Test
    fun testGetStatus() {
        val player = Player(positiveAP, playerName, positiveHP)
        val npc = NPC(negativeAP, npcName, negativeHP)
        assertEquals(positiveHP, player.getStatus().currentHealth, 0.0)
        assertEquals(positiveHP, npc.getStatus().currentHealth, 0.0)
        assertEquals(positiveAP, player.getStatus().attackPower, 0.0)
        assertEquals(positiveAP, npc.getStatus().attackPower, 0.0)
        assertEquals(true, player.getStatus().isAlive)
        assertEquals(true, npc.getStatus().isAlive)
        assertEquals(playerName, player.getStatus().name)
        assertEquals(npcName, npc.getStatus().name)
    }

    @Test
    fun testKillCombatControllable() {
        val player = Player(positiveAP, playerName, positiveHP)
        val npc = NPC(negativeAP, npcName, negativeHP)
        // overkill (this will get multiplied by ap as well)
        val damage = npc.getStatus().maxHealth
        player.dealDamage(damage, npc)
        // confirm that NPC health is at 0
        assertEquals(0.0, npc.getStatus().currentHealth, 0.0)
        // confirm that NPC is dead
        assertEquals(false, npc.getStatus().isAlive)

        // The npc shouldn't be able to deal damage when dead
        npc.dealDamage(damage, player)
        // player was initialized with positiveHP, so currentHealth should equal that
        assertEquals(positiveHP, player.getStatus().currentHealth, 0.0)
        assertEquals(true, player.getStatus().isAlive)
    }

    @Test
    fun testRestoreFullHealth() {
        val player = Player(positiveAP, playerName, positiveHP)
        val npc = NPC(negativeAP, npcName, negativeHP)
        // deal damage to player
        npc.dealDamage(1.0, player)
        // confirmation that the player did take damage
        assertEquals(true, player.getStatus().currentHealth < positiveHP)
        // restore full health
        player.restoreFullHealth()
        // confirm that player's health == player's max health
        assertEquals(true,
            player.getStatus().currentHealth == player.getStatus().maxHealth)
    }

    @Test
    fun testRestoreHealth() {
        val player = Player(positiveAP, playerName, positiveHP)
        val npc = NPC(negativeAP, npcName, negativeHP)
        npc.dealDamage(2.0, player)
        val damageDone = 2.0 * positiveAP
        // do damage and confirm the right amount of damage taken
        val tempHealth = player.getStatus().currentHealth
        assertEquals(tempHealth, positiveHP - damageDone, 0.0)

        // restore half of the damage done
        player.restoreHealth(positiveAP)
        // check that the right amount of health was restored
        assertEquals(player.getStatus().currentHealth, tempHealth + positiveAP, 0.0)
        // the player should not be full health at this point
        assertNotEquals(player.getStatus().currentHealth, player.getStatus().maxHealth, 0.0)

        // restore health over player's max health
        player.restoreHealth(player.getStatus().maxHealth * 10)
        // check that the player health is restored at max health
        assertEquals(player.getStatus().currentHealth, player.getStatus().maxHealth, 0.0)

        // test for negative health restore (shouldn't do anything)
        npc.dealDamage(5.0, player)
        // get player's hp before
        val hpBefore = player.getStatus().currentHealth
        player.restoreHealth(-500.0)
        // confirm that it didn't change
        assertEquals(hpBefore, player.getStatus().currentHealth, 0.0)

        // kill the player, and test restoring full health -> Should fail
        npc.dealDamage(player.getStatus().maxHealth * 5000, player)
        // confirm that the player is dead
        assertEquals(false, player.getStatus().isAlive)
        // try restoring to full heath - player should still be dead after
        player.restoreFullHealth()
        assertEquals(false, player.getStatus().isAlive)
        // try restoring some heath - player should still be dead after
        player.restoreHealth(50.0)
        assertEquals(false, player.getStatus().isAlive)
    }

    @Test
    fun testIncreaseMaxHealth() {
        val player = Player(positiveAP, playerName, positiveHP)
        // increase max health by 200%
        player.increaseMaxHealth(2.0)
        // confirm
        assertEquals(player.getStatus().maxHealth, positiveHP * 2.0, 0.0)

        // try to multiply by value lower than 1 -- should fail
        var tempHealth = player.getStatus().maxHealth
        player.increaseMaxHealth(0.5)
        assertEquals(tempHealth, player.getStatus().maxHealth, 0.0)

        // try to multiply by negative value
        tempHealth = player.getStatus().maxHealth
        player.increaseMaxHealth(-1.0)
        // confirm that it didn't change
        assertEquals(tempHealth, player.getStatus().maxHealth, 0.0)

        // try to multiple by 0
        tempHealth = player.getStatus().maxHealth
        player.increaseMaxHealth(0.0)
        // confirm that it didn't change
        assertEquals(tempHealth, player.getStatus().maxHealth, 0.0)
    }

    @Test
    fun testIncreaseMaxAP() {
        val player = Player(positiveAP, playerName, positiveHP)
        // increase max AP by 200%
        player.increaseAP(2.0)
        // confirm
        assertEquals(player.getStatus().attackPower, positiveAP * 2.0, 0.0)

        // try to multiply by value lower than 1 -- should fail
        var tempAP = player.getStatus().attackPower
        player.increaseAP(0.5)
        assertEquals(tempAP, player.getStatus().attackPower, 0.0)

        // try to multiply by negative value
        tempAP = player.getStatus().attackPower
        player.increaseAP(-1.0)
        // confirm that it didn't change
        assertEquals(tempAP, player.getStatus().attackPower, 0.0)

        // try to multiple by 0
        tempAP = player.getStatus().attackPower
        player.increaseAP(0.0)
        // confirm that it didn't change
        assertEquals(tempAP, player.getStatus().attackPower, 0.0)
    }
}