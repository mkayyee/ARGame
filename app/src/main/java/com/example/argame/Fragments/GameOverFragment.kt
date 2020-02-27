package com.example.argame.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import kotlinx.android.synthetic.main.profile.*

/***
 *  Fragment for the User profile.
 *
 *  Instantiated from MenuFragmentController
 */

class GameOverFragment : Fragment() {

    private var buttonCallbackListener: FragmentCallbackListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        buttonCallbackListener = context as FragmentCallbackListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.menu_game_over, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        //profile_button_back.setOnClickListener {
           // buttonCallbackListener!!.onButtonPressed(it as Button) TODO
       // }
    }
}