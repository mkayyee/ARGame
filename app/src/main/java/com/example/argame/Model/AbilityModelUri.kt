package com.example.argame.Model

import android.net.Uri

// An enum of all the abilities and their corresponding Uri for their projectile 3D models
enum class AbilityModelUri {
    TEST;
    fun uri() : Uri {
        when (this) {
            TEST-> return Uri.parse("untitledv4.sfb")
        }
    }
}