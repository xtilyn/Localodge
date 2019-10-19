package com.devssocial.localodge

import com.devssocial.localodge.models.Post
import com.devssocial.localodge.ui.dashboard.utils.PostsUtil
import com.google.firebase.Timestamp
import org.junit.Test
import java.util.*


class PostsUtilsTest {
    /**
     * Should order specified posts based on priorities:
     * rating (descending) > likesCount (descending) > timestamp (descending)
     */
    @Test
    fun ordersPostsCorrectly() {
        val post1 = Post(
            rating = 5,
            likes = hashMapOf("haha" to true),
            createdDate = Timestamp(Date(System.currentTimeMillis()))
        )
        val post2 = Post(
            rating = 1,
            likes = hashMapOf("haha" to true, "hoho" to true),
            createdDate = Timestamp(Date(System.currentTimeMillis()))
        )
        val post3 = Post(
            rating = 1,
            likes = hashMapOf("haha" to true),
            createdDate = Timestamp(Date(System.currentTimeMillis()))
        )
        val post4 = Post(
            rating = 1,
            likes = hashMapOf("haha" to true),
            createdDate = Timestamp(Date(System.currentTimeMillis() - 100))
        )
        val unorderedPosts = arrayListOf(
            post4,
            post2,
            post3,
            post1
        )
        val orderedPosts = PostsUtil.orderPosts(unorderedPosts)
        assert(orderedPosts[0] == post1 &&
                orderedPosts[1] == post2 &&
                orderedPosts[2] == post3 &&
                orderedPosts[3] == post4
        )
    }
}
