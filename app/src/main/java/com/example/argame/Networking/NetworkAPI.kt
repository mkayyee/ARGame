package com.example.argame.Networking

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import com.example.argame.Activities.MainActivity
import com.example.argame.Model.Persistence.User
import okhttp3.ResponseBody
import org.jetbrains.anko.networkStatsManager
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

object NetworkAPI {

    // Checks if the device has a network connection.
    // Run the provided block if connected -- if not -- notify the user
    // (if called from a context where they should be notified), and execute nothing.
    fun executeIfConnected(context: Context, shouldNotify: Boolean = false, runBlock: () -> Unit) {
        val service = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (service.isDefaultNetworkActive) {
            Log.d("NETWORK", "isDefaultNetworkActive: true")
            runBlock()
        } else {
            Log.d("NETWORK", "isDefaultNetworkActive: false -- called from context: $context")
            // Only make a toast in activities that absolutely require a network connection
            if (shouldNotify) {
                val t= Toast.makeText(context, "Check your network connection", Toast.LENGTH_SHORT)
                t.setGravity(Gravity.CENTER, 0, 0)
                t.show()
            }
        }
    }

    object HighScoreModel {
        data class HighScore(val username: String, val score: Int, val avatar: String? = null) : Comparable<HighScore> {
            override fun compareTo(other: HighScore): Int {
                return Integer.compare(other.score, this.score)
            }
        }
        data class PostHSBody(val uid: Int, val score: Int)
    }

    object UserModel {
        data class NewUserBody(val username: String)
        data class NewUsernameBody(val username: String, val uid: Int)
        data class GetUserResponse(val id: Int, val username: String, val avatar: String?, val score: Int?)
        data class DeleteUserBody(val uid: Int)
        data class ParseUserId(val userid: Int)
    }
}

object RetrofitClientInstance {
    private var retrofit: Retrofit? = null
    private const val BASE_URL = "http://185.87.111.206/survivar/"
    val retrofitInstance: Retrofit?
        get() {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit
        }
}

interface UserService {
    @POST("newuser")  // returns auto-generated user id from the server
    fun postNewUser(@Body body: NetworkAPI.UserModel.NewUserBody) : Call<NetworkAPI.UserModel.ParseUserId>

    @POST("newusername")
    fun postNewUsername(@Body body: NetworkAPI.UserModel.NewUsernameBody) : Call<ResponseBody>

    // TODO: change to query
    @POST("deleteuser")
    fun postDeleteUser(@Body body: NetworkAPI.UserModel.DeleteUserBody)

    @GET("user")
    fun getUser(@Query("id") id: Int) : Call<NetworkAPI.UserModel.GetUserResponse>
}

interface HighscoreService {
    @GET("highscores")
    fun getAllHighScores() : Call<List<NetworkAPI.HighScoreModel.HighScore>>

    @POST("newscore")
    fun newHighScore(@Body body: NetworkAPI.HighScoreModel.PostHSBody) : Call<ResponseBody>
}