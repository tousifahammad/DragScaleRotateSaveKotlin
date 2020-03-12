package net.app.mvvmsampleapp.app

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration
import net.app.mvvmsampleapp.floor.design.DesignViewModelFactory
import net.app.mvvmsampleapp.floor.design.TableRepository
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

class MyApplication : Application(), KodeinAware {

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)

        //==================== REALM Database Configuration =============//
        RealmConfiguration.Builder()
            .name("FloorPlanerDB.realm")
            .schemaVersion(1)
            .build()
    }

    override fun onTerminate() {
        super.onTerminate()
        Realm.getDefaultInstance().close()
    }

    override val kodein = Kodein.lazy {
        //==================== MVVM Dependency Injection =============//
        import(androidXModule(this@MyApplication))
        bind() from singleton { TableRepository() }
        bind() from provider { DesignViewModelFactory(instance(), instance()) }

    }

}