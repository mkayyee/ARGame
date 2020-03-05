package com.example.argame.Fragments.Menu

import android.app.Activity
import android.content.Context
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
import com.example.argame.Activities.GameActivityPlayground
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.menu_main.*

/***
 *  The class responsible for drawing all the menus over any activity.
 *
 *  Handles the logic for callbacks from menus attached to it.
 */

class MenuFragmentController : Fragment(), FragmentCallbackListener {

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
        val intent = Intent(activity, GameActivityPlayground::class.java)  // HUOMIO !!!!
        startActivity(intent)
    }

    private fun resumeGame() {
        fragmentManager!!.beginTransaction().remove(this).commit()
        onResume()
    }


    override fun onButtonPressed(btn: Button) {
        when (btn.id) {
            //R.id.settings_button_back -> drawMainMenuFragment()
            //R.id.high_scores_button_back -> drawMainMenuFragment()
            //R.id.profile_button_back -> drawMainMenuFragment()
            R.id.button_profile -> drawFragment(profileFrag)
            R.id.button_high_scores -> drawFragment(highscoresFrag)
            R.id.button_game_settings -> drawFragment(settingsFrag)
            R.id.button_new_game -> {
                saver?.edit()?.putInt("levelNum", 1)?.apply()
                launchGameActivity()
            }
            R.id.button_resume_game -> launchGameActivity()
        }
    }
}