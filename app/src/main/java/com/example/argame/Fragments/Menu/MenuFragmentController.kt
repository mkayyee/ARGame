package com.example.argame.Fragments.Menu

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.example.argame.Activities.GameActivity
import com.example.argame.Activities.TutorialActivityFirst
import com.example.argame.Fragments.Abilities.AbilityMenuFragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Model.Ability.Ability
import com.example.argame.Model.Ability.AbilityModifier
import com.example.argame.R
import org.jetbrains.anko.doAsync

/***
 *  The class responsible for drawing all the menus over any activity.
 *
 *  Handles the logic for callbacks from menus attached to it.
 */

class MenuFragmentController: Fragment(), FragmentCallbackListener {

    var saver : SharedPreferences? = null
    private val mainMenuFrag =
        MainMenuFragment()
    private val highscoresFrag =
        HighscoresFragment()
    private val profileFrag = ProfileFragment()
    private val settingsFrag =
        GameSettingsFragment()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        saver = PreferenceManager.getDefaultSharedPreferences(activity)
        return layoutInflater.inflate(R.layout.menu_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        drawMainMenuFragment()
    }

    private fun drawFragment(fragment: Fragment) {
        fragmentManager!!.beginTransaction()
            .replace(R.id.main_menu_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun drawMainMenuFragment() {
        // Add a check for a existing main menu in order to keep track of resume game
        fragmentManager!!.beginTransaction()
            .replace(R.id.main_menu_container, mainMenuFrag)
            .addToBackStack(null)
            .commit()
    }

    private fun launchGameActivity() {
        //val intent = Intent(activity, GameActivity::class.java)
        val intent = Intent(activity, GameActivity::class.java)  // HUOMIO !!!!
        startActivity(intent)
    }

    private fun resumeGame() {
        fragmentManager!!.beginTransaction().remove(this).commit()
        onResume()
    }


    override fun onButtonPressed(btn: Button) {
        when (btn.id) {
            R.id.button_profile -> drawFragment(profileFrag)
            R.id.button_high_scores -> drawFragment(highscoresFrag)
            R.id.button_tutorial -> {
                val intent = Intent(context, TutorialActivityFirst::class.java)
                startActivity(intent)
            }
            R.id.button_select_abilities_main -> drawFragment(AbilityMenuFragment(btn.context))
            R.id.button_new_game -> {
                val abilityList = listOf(
                    Ability.FBALL,
                    Ability.DOT,
                    Ability.SHIELD,
                    Ability.TELEPORT,
                    Ability.BEAM
                )
                saver?.edit()?.putInt("levelNum", 1)?.apply()
                saver?.edit()?.putInt("skillLevel", 0)?.apply()
                doAsync {abilityList.forEach {
                    saver?.edit()?.putFloat(
                        it.name + "pwr",
                        (AbilityModifier.getPwrModifier(it) - 1).toFloat()
                    )?.apply()
                    saver?.edit()
                        ?.putFloat(it.name + "cd", (AbilityModifier.getCdModifier(it) - 1).toFloat())
                        ?.apply()
                    Log.d("MODIFIER", "RESET " + it.name)
                }
                }

                launchGameActivity()
            }
            R.id.button_resume_game -> launchGameActivity()
        }
    }
}