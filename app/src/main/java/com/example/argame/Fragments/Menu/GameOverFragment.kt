package com.example.argame.Fragments.Menu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.argame.Activities.GameActivity
import com.example.argame.Activities.MainActivity
import com.example.argame.Fragments.Abilities.AbilityMenuFragment
import com.example.argame.R
import kotlinx.android.synthetic.main.menu_game_over.*
import kotlinx.android.synthetic.main.menu_main.*
import kotlinx.android.synthetic.main.menu_main.button_new_game

/***
 *  Fragment for the User profile.
 *
 *  Instantiated from MenuFragmentController
 */

class GameOverFragment(private val isGameMenu: Boolean = false) : Fragment(), View.OnClickListener {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = layoutInflater.inflate(R.layout.menu_game_over, container, false)
        val newGameBtn = v.findViewById<Button>(R.id.button_new_game)
        val exitBtn =  v.findViewById<Button>(R.id.button_exit)
        val selectAbilityBtn = v.findViewById<Button>(R.id.button_select_abilities)
        newGameBtn.setOnClickListener(this)
        exitBtn.setOnClickListener(this)
        selectAbilityBtn.setOnClickListener(this)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isGameMenu) {
            button_new_game.visibility = View.GONE
            button_select_abilities.visibility = View.GONE
            tv_game_over.text = getString(R.string.survivar)
        }
    }

    override fun onClick (v: View) {
        when (v.id) {
            R.id.button_exit -> {
                activity?.finish()
                val intent = Intent(activity, MainActivity::class.java)  // HUOMIO !!!!
                startActivity(intent)
            }
            R.id.button_new_game -> {
                activity?.finish()
                val intent = Intent(activity, GameActivity::class.java)  // HUOMIO !!!!
                startActivity(intent)
             }
            R.id.button_select_abilities -> {
                val fragment =
                    AbilityMenuFragment(v.context)
                fragmentManager!!.beginTransaction()
                    .replace(R.id.playground_main_menu_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }



}