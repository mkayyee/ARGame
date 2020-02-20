package com.example.argame.Model

data class Highscore(val userID: Int,
                     val username: String,
                     val score: Int,
                     val avatar: String? = null)