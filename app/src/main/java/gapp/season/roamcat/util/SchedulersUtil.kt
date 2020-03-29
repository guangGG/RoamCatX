package gapp.season.roamcat.util

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object SchedulersUtil {
    fun computation(): Scheduler {
        return Schedulers.computation()
    }

    fun single(): Scheduler {
        return Schedulers.single()
    }

    fun io(): Scheduler {
        return Schedulers.io()
    }

    fun ui(): Scheduler {
        return AndroidSchedulers.mainThread()
    }
}
