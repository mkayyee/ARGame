package com.example.argame.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.argame.Interfaces.FragmentCallbackHandler
import com.example.argame.Model.Menu
import com.example.argame.R

/***
 *  The class responsible for drawing all the menus over any activity.
 *
 *  Handles the logic for callbacks from menus attached to it.
 */

class MenuFragmentController : Fragment(), FragmentCallbackHandler {

    private val mainMenuFrag = MainMenuFragment()
    private val highscoresFrag = HighscoresFragment()
    private val profileFrag = ProfileFragment()
    private val settingsFrag = GameSettingsFragment()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.menu_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMainMenu()
    }

    private fun initFragment(fragment: Fragment) {
        fragmentManager!!.beginTransaction()
            .replace(R.id.main_menu_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun initMainMenu() {
        fragmentManager!!.beginTransaction()
            .replace(R.id.main_menu_container, mainMenuFrag)
            .addToBackStack(null)
            .commit()
    }

    override fun onButtonPressed(btn: Button) {
        when (btn.id) {
            R.id.settings_button_back -> initMainMenu()
            R.id.high_scores_button_back -> initMainMenu()
            R.id.profile_button_back -> initMainMenu()
            R.id.button_profile -> initFragment(profileFrag)
            R.id.button_high_scores -> initFragment(highscoresFrag)
            R.id.button_game_settings -> initFragment(settingsFrag)
        }
    }
}