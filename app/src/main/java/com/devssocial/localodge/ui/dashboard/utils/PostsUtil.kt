package com.devssocial.localodge.ui.dashboard.utils

import com.devssocial.localodge.models.Post
import com.devssocial.localodge.models.PostViewItem
import kotlin.math.ceil

object PostsUtil {

    /**
     * Should order specified posts based on priorities:
     * rating (descending) > likesCount (descending) > timestamp (descending)
     * @param posts to order
     */
    fun orderPosts(posts: List<Post>): List<Post> {
        val copy = arrayListOf<Post>()
        copy.addAll(posts)

        val ratingComparator = Comparator<Post> { post1, post2 ->
            post2.rating.compareTo(post1.rating)
        }
        val likesCountComparator = Comparator<Post> { post1, post2 ->
            post2.likes.size.compareTo(post1.likes.size)
        }
        val timestampComparator = Comparator<Post> { post1, post2 ->
            post2.timestamp?.compareTo(post1.timestamp ?: return@Comparator 0) ?: 0
        }

        return copy.sortedWith(ComplexComparator(
            ratingComparator,
            likesCountComparator,
            timestampComparator
        ))
    }

    /**
     * Constructs HashMap<page: Int, hits: List> for pagination
     *  where key = current page and value = data associated with current page.
     *  @param hitsPerPage number of items to display in a single page
     *  @param data data to paginate
     */
    fun constructMapBasedOnHitsPerPage(
        hitsPerPage: Int,
        data: List<PostViewItem>
    ): HashMap<Int, ArrayList<PostViewItem>> {
        val n = data.size
        val result = hashMapOf<Int, ArrayList<PostViewItem>>()
        var currentPage = 0
        val pagesTotal: Int = ceil(n / hitsPerPage.toDouble()).toInt()
        var currStartInclusive = 0
        var currEndExclusive = hitsPerPage
        repeat(pagesTotal) {
            result[currentPage] = ArrayList(if (currEndExclusive >= n) {
                data.subList(currStartInclusive, n)
            } else {
                data.subList(currStartInclusive, currEndExclusive)
            })

            currentPage++
            currStartInclusive = currEndExclusive
            currEndExclusive += hitsPerPage
        }
        return result
    }

}