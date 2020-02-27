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

class NextLevelFragment : Fragment(), View.OnClickListener {

    private var buttonCallbackListener: FragmentCallbackListener? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = layoutInflater.inflate(R.layout.menu_level_completed, container, false)
        val newGameBtn = v.findViewById<Button>(R.id.button_next_level)
        val exitBtn =  v.findViewById<Button>(R.id.button_exit)
        newGameBtn.setOnClickListener(this)
        exitBtn.setOnClickListener(this)
        return v
    }

    override fun onClick (v: View) {
        when (v.id) {
            R.id.button_exit -> {
                val intent = Intent(activity, MainActivity::class.java)  // HUOMIO !!!!
                startActivity(intent)
            }
            R.id.button_next_level -> {
                val intent = Intent(activity, GameActivityPlayground::class.java)  // HUOMIO !!!!
                startActivity(intent)
            }
        }
    }
}