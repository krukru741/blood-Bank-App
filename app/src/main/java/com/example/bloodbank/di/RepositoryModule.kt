package com.example.bloodbank.di

import com.example.bloodbank.data.repository.AuthRepositoryImpl
import com.example.bloodbank.data.repository.BloodRequestRepositoryImpl
import com.example.bloodbank.data.repository.ChatRepositoryImpl
import com.example.bloodbank.data.repository.DonorRepositoryImpl
import com.example.bloodbank.data.repository.UserRepositoryImpl
import com.example.bloodbank.domain.repository.AuthRepository
import com.example.bloodbank.domain.repository.BloodRequestRepository
import com.example.bloodbank.domain.repository.ChatRepository
import com.example.bloodbank.domain.repository.DonorRepository
import com.example.bloodbank.domain.repository.UserRepository
import com.example.bloodbank.data.repository.HospitalRepositoryImpl
import com.example.bloodbank.domain.repository.HospitalRepository
import com.example.bloodbank.data.repository.PsgcRepositoryImpl
import com.example.bloodbank.domain.repository.PsgcRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule
 *
 * Binds each domain Repository *interface* to its concrete *implementation*
 * using @Binds — more efficient than @Provides because it avoids generating
 * an extra class; Hilt just casts the implementation to the interface type.
 *
 * Pattern:
 *   Domain Layer  →  AuthRepository          (interface)
 *   Data Layer    →  AuthRepositoryImpl      (implementation)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // ── Authentication ────────────────────────────────────────────────────────
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    // ── User Profile ──────────────────────────────────────────────────────────
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindHospitalRepository(
        impl: HospitalRepositoryImpl
    ): HospitalRepository

    // ── Donor ─────────────────────────────────────────────────────────────────
    @Binds
    @Singleton
    abstract fun bindDonorRepository(
        impl: DonorRepositoryImpl
    ): DonorRepository

    // ── Blood Requests ────────────────────────────────────────────────────────
    @Binds
    @Singleton
    abstract fun bindBloodRequestRepository(
        impl: BloodRequestRepositoryImpl
    ): BloodRequestRepository

    // ── Chat ──────────────────────────────────────────────────────────────────
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    // ── PSGC Locations ────────────────────────────────────────────────────────
    @Binds
    @Singleton
    abstract fun bindPsgcRepository(
        impl: PsgcRepositoryImpl
    ): PsgcRepository

}
