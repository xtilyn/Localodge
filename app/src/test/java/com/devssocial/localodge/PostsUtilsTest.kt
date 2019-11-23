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
            likes = setOf("haha"),
            timestamp = Timestamp(Date(System.currentTimeMillis()))
        )
        val post2 = Post(
            rating = 1,
            likes = setOf("haha", "hoho"),
            timestamp = Timestamp(Date(System.currentTimeMillis()))
        )
        val post3 = Post(
            rating = 1,
            likes = setOf("haha"),
            timestamp = Timestamp(Date(System.currentTimeMillis()))
        )
        val post4 = Post(
            rating = 1,
            likes = setOf("haha"),
            timestamp = Timestamp(Date(System.currentTimeMillis() - 100))
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

    @Test
    fun shouldConstructPagesCorrectly() {
        val sampleData = arrayListOf(
            PostViewItem(objectID = "1"),
            PostViewItem(objectID = "2"),
            PostViewItem(objectID = "3"),
            PostViewItem(objectID = "4"),
            PostViewItem(objectID = "5")
        )

        val map = PostsUtil.constructMapBasedOnHitsPerPage(
            2,
            sampleData
        )

        println(map)
        assert(
            map[0]?.get(0)?.objectID  == "1" &&
            map[0]?.get(1)?.objectID  == "2" &&
            map[1]?.get(0)?.objectID  == "3" &&
            map[1]?.get(1)?.objectID  == "4" &&
            map[2]?.get(0)?.objectID  == "5"
        )
    }
}
