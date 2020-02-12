package com.example.argame.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.argame.Interfaces.FragmentCallbackHandler
import com.example.argame.R
import kotlinx.android.synthetic.main.high_scores.*

class HighscoresFragment : Fragment() {

    private var buttonCallback: FragmentCallbackHandler? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        buttonCallback = context as FragmentCallbackHandler
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.high_scores, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        high_scores_button_back.setOnClickListener {
            buttonCallback!!.onButtonPressed(it as Button)
        }
    }
}