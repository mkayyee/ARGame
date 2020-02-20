package com.example.argame.Model

data class User(
    val id: Int,
    val username: String,
    val highScore: Int? = null,
    var numberOfGames: Int = 0,
    val avatar: String? = null)


