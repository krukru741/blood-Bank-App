package com.example.bloodbank.domain.model

import com.example.bloodbank.domain.error.AppError

/**
 * Sealed class representing the result of any async operation.
 * Used across all layers to avoid throwing exceptions across boundaries.
 */
sealed class Resource<out T> {

    /** Indicates an operation is in progress. */
    data object Loading : Resource<Nothing>()

    /** Indicates a successful operation with a [data] payload. */
    data class Success<T>(val data: T) : Resource<T>()

    /** Indicates a failure with a mapped [AppError]. */
    data class Error(val error: AppError) : Resource<Nothing>()
}
