package com.devssocial.localodge

import com.devssocial.localodge.models.Post
import com.devssocial.localodge.ui.dashboard.utils.PostsUtil
import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UtilsTest {
    @Test
    fun ordersPostsByDescendingRating() {
        val unorderedPosts = arrayListOf(
            Post(rating = 1),
            Post(rating = 0),
            Post(rating = 5)
        )
        val orderedPosts = PostsUtil.orderPosts(unorderedPosts)
        assert(
            orderedPosts[0].rating == 5 &&
            orderedPosts[1].rating == 1 &&
            orderedPosts[2].rating == 0
        )
    }
}
