package com.example.argame.Fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import kotlinx.android.synthetic.main.activity_level_intermission.*

class AbilityMenuFragment : Fragment() {

    private lateinit var fragmentCallbackListener: FragmentCallbackListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fragmentCallbackListener = context as FragmentCallbackListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = layoutInflater.inflate(R.layout.activity_level_intermission, container, false)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_close_ability_menu.setOnClickListener {
            fragmentCallbackListener.onButtonPressed(it as Button)
        }
    }
}