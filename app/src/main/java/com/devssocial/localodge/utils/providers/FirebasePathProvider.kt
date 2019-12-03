package com.devssocial.localodge.utils.providers

object FirebasePathProvider {

    fun getProfilePicPath(userId: String): String =
        "users/$userId/profilePictures/${userId}.png"

    fun getSecondBucketPath(): String = "gs://localodge"

    fun getPostsMediaPath(postId: String) = "posts/$postId"
}