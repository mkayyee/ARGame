package com.example.argame.Model

data class CombatControllableStatus(
    val isAlive: Boolean,
    val currentHealth: Int,
    val currentAP: Int,
    val name: String)