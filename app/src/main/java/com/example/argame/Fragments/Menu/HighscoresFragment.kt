package com.example.argame.Fragments.Menu

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.argame.Interfaces.FragmentCallbackListener
import com.example.argame.Networking.HighscoreService
import com.example.argame.Networking.NetworkAPI
import com.example.argame.Networking.RetrofitClientInstance
import com.example.argame.R
import kotlinx.android.synthetic.main.high_scores.*
import kotlinx.android.synthetic.main.item_highscore.view.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import kotlin.math.log

/**
 *  Fragment for the list of high scores.
 *
 *  Instantiated from MenuFragmentController
 */

class HighscoresFragment : Fragment() {

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
        return layoutInflater.inflate(R.layout.high_scores, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtonListeners()
         NetworkAPI.executeIfConnected(context!!, true) {
             getHighScores()
         }
    }

    private fun setupButtonListeners() {
        high_scores_button_back.setOnClickListener {
            //buttonCallbackListener!!.onButtonPressed(it as Button)
            fragmentManager!!.popBackStack()
        }
    }

    private fun getHighScores() {
        val service = RetrofitClientInstance.retrofitInstance?.create(HighscoreService::class.java)
        val call = service?.getAllHighScores()

        call?.enqueue(object: Callback<List<NetworkAPI.HighScoreModel.HighScore>> {
            override fun onFailure(
                call: Call<List<NetworkAPI.HighScoreModel.HighScore>>,
                t: Throwable) {
                Log.d("RETROFIT", "Failed to fetch high scores")
                Log.d("RETROFIT", t.localizedMessage ?: "no message but error")
            }
            override fun onResponse(
                call: Call<List<NetworkAPI.HighScoreModel.HighScore>>,
                response: Response<List<NetworkAPI.HighScoreModel.HighScore>>
            ) {
                val body = response.body()
                val scores = body
                scores?.forEach {
                    Log.d("RETROFIT", "Username: ${it.username} Score: ${it.score}")
                }
                if (scores != null) {
                    val lm = LinearLayoutManager(context)
                    recycler_highScores.adapter = HighScoreAdapter(scores.sorted())
                    recycler_highScores.layoutManager = lm
                }
            }
        })
    }
}

class HighScoreAdapter(private val scores: List<NetworkAPI.HighScoreModel.HighScore>) : RecyclerView.Adapter<HighScoreViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighScoreViewHolder {
        val item = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_highscore, parent, false) as View
        return HighScoreViewHolder(item)
    }

    override fun getItemCount(): Int {
        return scores.size
    }

    override fun onBindViewHolder(holder: HighScoreViewHolder, position: Int) {
        val uName = scores[position].username
        val score = "Score: ${scores[position].score}"
        val order = "#${position+1}"
        holder.itemView.textView_highScore_order.text = order
        holder.itemView.textView_score_username.text = uName
        holder.itemView.textView_score.text = score
    }

}

class HighScoreViewHolder(v: View) : RecyclerView.ViewHolder(v)