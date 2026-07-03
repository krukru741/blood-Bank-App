package com.example.bloodbank.data.error

import com.example.bloodbank.domain.error.AppError
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException

object FirebaseErrorMapper {

    fun mapToAppError(throwable: Throwable?): AppError {
        return when (throwable) {
            is FirebaseAuthInvalidCredentialsException -> AppError.InvalidCredentials()
            is FirebaseAuthInvalidUserException -> AppError.UserNotFound()
            is FirebaseAuthUserCollisionException -> AppError.EmailAlreadyInUse()
            is FirebaseAuthWeakPasswordException -> AppError.WeakPassword()
            is FirebaseAuthException -> AppError.Unknown(throwable.message ?: "Authentication failed")
            
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.PermissionDenied()
                    FirebaseFirestoreException.Code.NOT_FOUND -> AppError.NotFound()
                    else -> AppError.Unknown(throwable.message ?: "Database error occurred")
                }
            }
            
            is FirebaseNetworkException -> AppError.Network()
            is IOException -> AppError.Network()
            is SocketTimeoutException -> AppError.Timeout()
            
            is FirebaseException -> AppError.Unknown(throwable.message ?: "Firebase error occurred")
            
            is IllegalStateException -> AppError.Unknown(throwable.message ?: "Internal error occurred")
            is IllegalArgumentException -> AppError.Unknown(throwable.message ?: "Invalid argument")
            
            else -> AppError.Unknown(throwable?.message ?: "An unexpected error occurred")
        }
    }
}
