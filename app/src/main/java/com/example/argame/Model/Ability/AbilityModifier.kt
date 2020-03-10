package com.example.argame.Model.Ability

class AbilityModifier() {

    private var a = 1

    fun getModifier(ability: Ability) : Int {
        return when(ability){
            Ability.FBALL -> a
            Ability.BEAM -> a
            Ability.SHIELD -> a
            Ability.TELEPORT -> a
            Ability.DOT -> a
            Ability.ATK -> a
        }

    }



    fun setModifier(ability: Ability, modifier : String, value : Any) {
        when (ability) {
            Ability.FBALL -> {
                modifier // Look up ability to change
                a = value as Int // add value to ability
            }
            Ability.BEAM -> "moi"
            Ability.SHIELD -> "moi"
            Ability.TELEPORT -> "moi"
            Ability.DOT -> "moi"
            Ability.ATK -> "moi"
        }
    }

}