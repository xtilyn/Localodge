package com.devssocial.localodge.utils.providers

object FirebasePathProvider {

    fun getProfilePicPath(userId: String): String =
        "users/$userId/profilePictures/${userId}.png"

    fun getSecondBucketPath(): String = "gs://localodge"
    fun getPostsMediaPath(postId: String) = "posts/$postId"
    fun getCommentsMediaPath(postId: String, commentId: String) = "comments/$postId/$commentId"
    fun getPostStatisticsPath(postId: String) = "posts/$postId/"
}