package com.example.argame.Model

data class CombatControllableStatus(
    val isAlive: Boolean,
    val currentHealth: Double,
    val attackPower: Double,
    val name: String,
    val maxHealth: Double)