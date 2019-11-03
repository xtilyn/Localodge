package com.devssocial.localodge.utils

object FirebasePathProvider {

    fun getProfilePicPath(userId: String): String =
        "users/$userId/profilePictures/${userId}.png"

}