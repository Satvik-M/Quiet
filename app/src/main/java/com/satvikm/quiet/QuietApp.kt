package com.satvikm.quiet

import android.app.Application
import com.satvikm.quiet.data.apps.AppRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuietApp : Application() {

    @Inject lateinit var appRepository: AppRepository

    override fun onCreate() {
        super.onCreate()
        appRepository.start()
    }
}
