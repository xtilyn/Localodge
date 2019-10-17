package com.devssocial.localodge.ui.dashboard.utils

import com.devssocial.localodge.models.Post

object PostsUtil {

    /**
     * Orders specified posts collection by promoted rating in
     * descending order.
     * @param posts to order
     */
    fun orderPosts(posts: List<Post>): List<Post> {
        val copy = arrayListOf<Post>()
        copy.addAll(posts)

        val ratingComparator = Comparator<Post> { post1, post2 ->
            post2.rating.compareTo(post1.rating)
        }

        return copy.sortedWith(ratingComparator)
    }

}