package com.example.argame.Networking

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

object NetworkAPI {

    object HighScoreModel {
        data class HighScore(val username: String, val score: Int, val avatar: String? = null)
        data class PostHSBody(val id: Int, val score: Int)
    }

    object UserModel {
        data class NewUserBody(val username: String)
        data class NewUsernameBody(val username: String, val uid: Int)
        data class DeleteUserBody(val uid: Int)
        data class ParseUserId(val uid: Int)
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
    fun postNewUsername(@Body body: NetworkAPI.UserModel.NewUsernameBody)

    // TODO: change to query
    @POST("deleteuser")
    fun postDeleteUser(@Body body: NetworkAPI.UserModel.DeleteUserBody)
}

interface HighscoreService {
    @GET("highscores")
    fun getAllHighScores() : Call<List<NetworkAPI.HighScoreModel.HighScore>>

    @POST("newscore")
    fun newHighScore(@Body body: NetworkAPI.HighScoreModel.PostHSBody)
}