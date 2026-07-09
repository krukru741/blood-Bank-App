package com.example.bloodbank.data.repository

import com.example.bloodbank.domain.model.Resource
import com.example.bloodbank.domain.model.HospitalMarker
import com.example.bloodbank.domain.model.MockHospitalData
import com.example.bloodbank.domain.repository.HospitalRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HospitalRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : HospitalRepository {

    private val collection = firestore.collection("hospitals")

    override fun getHospitals(): Flow<Resource<List<HospitalMarker>>> = callbackFlow {
        trySend(Resource.Loading)
        
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(com.example.bloodbank.domain.error.AppError.Unknown(error.message ?: "Firestore error")))
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    // Seed the database if it's empty
                    CoroutineScope(Dispatchers.IO).launch {
                        seedHospitals(MockHospitalData.hospitals)
                    }
                } else {
                    val hospitals = snapshot.documents.mapNotNull { it.toObject(HospitalMarker::class.java) }
                    trySend(Resource.Success(hospitals))
                }
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun seedHospitals(hospitals: List<HospitalMarker>): Resource<Unit> {
        return try {
            val batch = firestore.batch()
            for (hospital in hospitals) {
                val docRef = if (hospital.id.isNotEmpty()) {
                    collection.document(hospital.id)
                } else {
                    collection.document()
                }
                // Ensure the id matches the document id
                batch.set(docRef, hospital.copy(id = docRef.id))
            }
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(com.example.bloodbank.domain.error.AppError.Unknown(e.message ?: "Seeding failed"))
        }
    }

    override suspend fun addHospital(hospital: HospitalMarker): Resource<Unit> {
        return try {
            val docRef = if (hospital.id.isNotEmpty()) {
                collection.document(hospital.id)
            } else {
                collection.document()
            }
            val newHospital = hospital.copy(id = docRef.id)
            docRef.set(newHospital).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(com.example.bloodbank.domain.error.AppError.Unknown(e.message ?: "Failed to add hospital"))
        }
    }
}
