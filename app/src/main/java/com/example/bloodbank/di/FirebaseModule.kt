package com.example.bloodbank.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FirebaseModule
 *
 * Provides Firebase SDK singletons to the Hilt DI graph.
 * All instances are app-scoped (@Singleton) because Firebase SDKs
 * are thread-safe and expensive to initialise.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // ── Firebase Auth ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()

    // ── Cloud Firestore ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val settings = FirebaseFirestoreSettings.Builder()
            // Enable offline persistence (cached data while device is offline)
            .setPersistenceEnabled(true)
            // Cache size: 100 MB — increase for heavy data apps
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = settings
        }
    }

    // ── Realtime Database ─────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance().apply {
            // Keep syncing this database instance even when there are no active listeners
            setPersistenceEnabled(true)
        }

    // ── Firebase Storage ──────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage =
        FirebaseStorage.getInstance()
}
