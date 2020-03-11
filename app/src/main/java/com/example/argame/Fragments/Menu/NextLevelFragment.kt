package com.example.argame.Fragments.Menu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.argame.Activities.GameActivity
import com.example.argame.Activities.MainActivity
import com.example.argame.Fragments.Abilities.AbilityMenuFragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R

/***
 *  Fragment for the User profile.
 *
 *  Instantiated from MenuFragmentController
 */

class NextLevelFragment(val fragManager: FragmentManager) : Fragment(), View.OnClickListener {

    private var buttonCallbackListener: FragmentCallbackListener? = null



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = layoutInflater.inflate(R.layout.menu_level_completed, container, false)
        val newGameBtn = v.findViewById<Button>(R.id.button_next_level)
        val exitBtn =  v.findViewById<Button>(R.id.button_exit)
        val selectAbilityBtn = v.findViewById<Button>(R.id.button_select_abilities)
        newGameBtn.setOnClickListener(this)
        exitBtn.setOnClickListener(this)
        selectAbilityBtn.setOnClickListener(this)
        return v
    }

    override fun onClick (v: View) {
        when (v.id) {
            R.id.button_exit -> {
                activity?.finish()
                val intent = Intent(activity, MainActivity::class.java)  // HUOMIO !!!!
                startActivity(intent)
            }
            R.id.button_next_level -> {
                activity?.finish()
                val intent = Intent(activity, GameActivity::class.java)  // HUOMIO !!!!
                startActivity(intent)
            }
            R.id.button_select_abilities -> {
                val fragment =
                    AbilityMenuFragment(v.context)
                fragManager.beginTransaction()
                    .replace(R.id.playground_main_menu_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}