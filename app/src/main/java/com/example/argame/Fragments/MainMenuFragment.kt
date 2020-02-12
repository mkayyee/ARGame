package com.example.argame.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.R
import kotlinx.android.synthetic.main.menu_main.*

class MainMenuFragment : Fragment() {

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
        return layoutInflater.inflate(R.layout.menu_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        val buttonContainer = main_menu_button_container
        buttonContainer.children.forEach {
            if (it is Button) {
                it.setOnClickListener {
                    buttonCallbackListener!!.onButtonPressed(it as Button)
                }
            }
        }
    }
}