package com.example.argame.Model.Ability

import android.util.Log

object AbilityModifier {

    private var fBallPwr = 1.0
    private var fBallCd = 1.0
    private var beamPwr = 1.0
    private var beamCd = 1.0
    private var shieldPwr = 1.0
    private var shieldCd = 1.0
    private var teleportPwr = 1.0
    private var teleportCd = 1.0
    private var dotPwr = 1.0
    private var dotCd = 1.0
    private var atkPwr = 1.0
    private var atkCd = 1.0

    fun getPwrModifier(ability: Ability) : Double {
        return when(ability){
            Ability.FBALL -> fBallPwr
            Ability.BEAM -> beamPwr
            Ability.SHIELD -> shieldPwr
            Ability.TELEPORT -> teleportPwr
            Ability.DOT -> dotPwr
            Ability.ATK -> atkPwr
        }
    }

    fun getCdModifier(ability: Ability) : Double {
        return when(ability){
            Ability.FBALL -> fBallCd
            Ability.BEAM -> beamCd
            Ability.SHIELD -> shieldCd
            Ability.TELEPORT -> teleportCd
            Ability.DOT -> dotCd
            Ability.ATK -> atkCd
        }
    }

    fun setModifier(ability: Ability, pwrBoost : Boolean, value: Double = 0.0) {
        var pwrIncrease = value
        if (pwrIncrease > 0) {
            pwrIncrease -= 0.05
        }
        when (ability) {
            Ability.FBALL -> {
                if (pwrBoost) {
                    fBallPwr += 0.05 + pwrIncrease
                    Log.d("Fireball PWR: ", fBallPwr.toString())
                }
                else {
                    fBallCd -= 0.05 + pwrIncrease
                }
            }
            Ability.BEAM -> {
                if (pwrBoost) {
                    beamPwr += 0.05 + pwrIncrease
                }
                else {
                    beamCd -= 0.05 + pwrIncrease
                }
            }
            Ability.SHIELD -> {
                if (pwrBoost) {
                    shieldPwr += 0.05 + pwrIncrease
                }
                else {
                    shieldCd -= 0.05 + pwrIncrease
                }
            }
            Ability.TELEPORT -> {
                if (pwrBoost) {
                    teleportPwr += 0.05 + pwrIncrease
                }
                else {
                    teleportCd -= 0.05 + pwrIncrease
                }
            }
            Ability.DOT -> {
                if (pwrBoost) {
                    dotPwr += 0.05 + pwrIncrease
                }
                else {
                    dotCd -= 0.05 + pwrIncrease
                }
            }
            Ability.ATK -> {
                if (pwrBoost) {
                    atkPwr += 0.05 + pwrIncrease
                }
                else {
                    atkCd -= 0.05 + pwrIncrease
                }
            }
        }
    }
}