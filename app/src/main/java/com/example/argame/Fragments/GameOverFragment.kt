package com.example.argame.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.argame.Activities.GameActivityPlayground
import com.example.argame.Activities.MainActivity
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import kotlinx.android.synthetic.main.profile.*

/***
 *  Fragment for the User profile.
 *
 *  Instantiated from MenuFragmentController
 */

class GameOverFragment : Fragment(), View.OnClickListener {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = layoutInflater.inflate(R.layout.menu_game_over, container, false)
        val newGameBtn = v.findViewById<Button>(R.id.button_new_game)
        val exitBtn =  v.findViewById<Button>(R.id.button_exit)
        newGameBtn.setOnClickListener(this)
        exitBtn.setOnClickListener(this)
        return v
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
                val intent = Intent(activity, GameActivityPlayground::class.java)  // HUOMIO !!!!
                startActivity(intent)
             }
        }
    }



}