package com.example.argame.Model.CombatControllable

data class CombatControllableStatus(
    val isAlive: Boolean,
    val currentHealth: Double,
    val attackPower: Double,
    val name: String,
    val maxHealth: Double,
    val level: Int,
    val experience: Double,
    val xpRequired: Double)