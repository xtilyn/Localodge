package com.devssocial.localodge

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.junit.Test

class Playground {

    @Test
    fun sample() {
        println(10 / 3)
    }

    @Test
    @Throws(InterruptedException::class)
    fun single_merge_inOrder() {
        val temp = Single.merge(
            listOf(
                Single.create<String> { e ->
                    e.onSuccess("hi im single 1")
                },
                Single.create<String> { e ->
                    Thread.sleep(1000)
                    e.onSuccess("hi im single 2")
                },
                Single.create<String> { e ->
                    e.onSuccess("hi im single 3")
                }
            )
        )

        temp.subscribeOn(Schedulers.trampoline()).observeOn(Schedulers.trampoline())
            .subscribeBy(
                onError = {
                    println(it)
                },
                onNext = {
                    println(it)
                },
                onComplete = {
                    println("hi im done.")
                }
            )
    }

    @Test
    fun singleOnErrorResumeNext() {
        val disposable = Single.just("this should not print")
            .flatMap {
                Single.error<String>(Throwable("No value"))
            }
            .onErrorResumeNext {
                if (it.message == "No value") return@onErrorResumeNext Single.just("this should print")
                else return@onErrorResumeNext Single.error(it)
            }
        disposable.subscribeOn(Schedulers.trampoline()).observeOn(Schedulers.trampoline())
            .subscribeBy(
                onError = {
                    println("subscribe onError: ${it.message}")
                },
                onSuccess = {
                    println("subscribe onSuccess: $it")
                }
            )
    }

    @Test
    fun complexCompletablesToSingle() {
        val disposable = Completable.create {
            Thread.sleep(1000)
            println("im first")
            it.onComplete()
        }.andThen(SingleSource<String> {
            it.onSuccess("uwu")
        }).flatMapCompletable {
            println(it)
            Completable.complete()
        }

        disposable.subscribeOn(Schedulers.trampoline()).observeOn(Schedulers.trampoline())
            .subscribeBy(
                onError = {
                    println("subscribe onError: ${it.message}")
                },
                onComplete = {
                }
            )
    }

    @Test
    fun clientSideJoins() {
        val combinedSingles = arrayListOf(
            Single.zip(
                Single.create { e ->
                    Thread.sleep(1000)
                    e.onSuccess(arrayListOf("A. comment 1", "A. comment 2"))
                },
                Single.just(1),
                BiFunction<ArrayList<String>, Int, String> { comments, user ->
                    return@BiFunction "comments: $comments, user: $user"
                }
            ),
            Single.zip(
                Single.create { e ->
                    e.onSuccess(arrayListOf("B. comment 1", "B. comment 2"))
                },
                Single.just(2),
                BiFunction<ArrayList<String>, Int, String> { comments, user ->
                    return@BiFunction "comments: $comments, user: $user"
                }
            )
        )

        val disposable = Single.merge(combinedSingles)
            .subscribeOn(Schedulers.trampoline())
            .observeOn(Schedulers.trampoline())

        disposable.subscribeBy {
            println(it)
        }
    }
}