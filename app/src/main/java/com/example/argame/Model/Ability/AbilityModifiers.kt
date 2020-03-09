package com.example.argame.Model.Ability

class AbilityModifiers() {



    fun mainModifier(ability: Ability, modifier : String, value : Any) {
        when (ability) {
            Ability.TEST -> {
                modifier // Look up ability to change
                value // add value to ability
            }
            Ability.BEAM -> "moi"
            Ability.SHIELD -> "moi"
            Ability.TELEPORT -> "moi"
        }
    }





}