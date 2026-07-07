package com.example.bloodbank.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton
import androidx.room.Room
import com.example.bloodbank.data.local.AppDatabase
import com.example.bloodbank.data.local.dao.LocationDao

// ── DataStore extension property ──────────────────────────────────────────────
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bloodbank_prefs"
)

// ── Dispatcher Qualifiers ─────────────────────────────────────────────────────
/** Marks the IO [CoroutineDispatcher] — use for network/disk operations. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Marks the Default [CoroutineDispatcher] — use for CPU-intensive work. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Marks the Main [CoroutineDispatcher] — use for UI updates. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * AppModule
 *
 * Provides application-scoped dependencies that are NOT Firebase-specific.
 * Installed in [SingletonComponent] → lives for the entire app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Coroutine Dispatchers ─────────────────────────────────────────────────

    @IoDispatcher
    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @DefaultDispatcher
    @Provides
    @Singleton
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @MainDispatcher
    @Provides
    @Singleton
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    // ── DataStore ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore

    // ── Room Database ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bloodbank_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(
        appDatabase: AppDatabase
    ): LocationDao {
        return appDatabase.locationDao
    }
}
