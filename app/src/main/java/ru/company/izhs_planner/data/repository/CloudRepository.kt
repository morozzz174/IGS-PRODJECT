package ru.company.izhs_planner.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ru.company.izhs_planner.domain.model.Project

interface CloudRepository {
    val isSyncing: StateFlow<Boolean>
    val lastSyncTime: StateFlow<Long>
    val syncError: StateFlow<String?>
    
    suspend fun syncProject(project: Project): Boolean
    suspend fun deleteProject(projectId: String): Boolean
    suspend fun fetchProjects(userId: String): List<Project>
    suspend fun syncAllProjects(userId: String): Boolean
    fun setUserId(userId: String)
    fun clear()
}

class CloudRepositoryImpl : CloudRepository {
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: StateFlow<Boolean> = _isSyncing
    
    private val _lastSyncTime = MutableStateFlow(0L)
    override val lastSyncTime: StateFlow<Long> = _lastSyncTime
    
    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError
    
    private var userId: String = ""
    
    private val firestore = com.google.firebase.firestore.Firebase.firestore
    
    override fun setUserId(userId: String) {
        this.userId = userId
    }
    
    override suspend fun syncProject(project: Project): Boolean {
        if (userId.isEmpty()) return false
        _isSyncing.value = true
        _syncError.value = null
        
        return try {
            val projectData = mapOf(
                "id" to project.id,
                "name" to project.name,
                "description" to project.description,
                "plotWidth" to project.plot.width,
                "plotLength" to project.plot.length,
                "plotSlope" to project.plot.slope,
                "plotArea" to project.plot.area,
                "houseWidth" to project.house.width,
                "houseLength" to project.house.length,
                "houseFloors" to project.house.floors,
                "houseFloorHeight" to project.house.floorHeight,
                "houseRoofType" to project.house.roofType.name,
                "houseFoundationType" to project.house.foundationType.name,
                "houseWallMaterial" to project.house.wallMaterial.name,
                "houseRoofMaterial" to project.house.roofMaterial.name,
                "floorsJson" to com.google.gson.Gson().toJson(project.floors),
                "createdAt" to project.createdAt,
                "updatedAt" to project.updatedAt,
                "syncedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("projects")
                .document(project.id)
                .set(projectData)
                .await()
            
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false
            true
        } catch (e: Exception) {
            _syncError.value = e.message
            _isSyncing.value = false
            false
        }
    }
    
    override suspend fun deleteProject(projectId: String): Boolean {
        if (userId.isEmpty()) return false
        _isSyncing.value = true
        
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("projects")
                .document(projectId)
                .delete()
                .await()
            
            _isSyncing.value = false
            true
        } catch (e: Exception) {
            _syncError.value = e.message
            _isSyncing.value = false
            false
        }
    }
    
    override suspend fun fetchProjects(userId: String): List<Project> {
        if (userId.isEmpty()) return emptyList()
        _isSyncing.value = true
        
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("projects")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.DESCENDING)
                .get()
                .await()
            
            val projects = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<CloudProject>()?.toProject()
                } catch (e: Exception) {
                    null
                }
            }
            
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false
            projects
        } catch (e: Exception) {
            _syncError.value = e.message
            _isSyncing.value = false
            emptyList()
        }
    }
    
    override suspend fun syncAllProjects(userId: String): Boolean {
        this.userId = userId
        if (userId.isEmpty()) return false
        _isSyncing.value = true
        _syncError.value = null
        
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("projects")
                .get()
                .await()
            
            for (doc in snapshot.documents) {
                try {
                    val cloudProject = doc.toObject<CloudProject>()
                    // Handle sync if needed
                } catch (e: Exception) {
                    // Continue
                }
            }
            
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false
            true
        } catch (e: Exception) {
            _syncError.value = e.message
            _isSyncing.value = false
            false
        }
    }
    
    override fun clear() {
        userId = ""
        _lastSyncTime.value = 0L
        _syncError.value = null
    }
    
    @Keep
    data class CloudProject(
        val id: String = "",
        val name: String = "",
        val description: String = "",
        val plotWidth: Float = 20f,
        val plotLength: Float = 30f,
        val plotSlope: Float = 0f,
        val plotArea: Float = 6f,
        val houseWidth: Float = 10f,
        val houseLength: Float = 12f,
        val houseFloors: Int = 2,
        val houseFloorHeight: Float = 3f,
        val houseRoofType: String = "GABLED",
        val houseFoundationType: String = "STRIP",
        val houseWallMaterial: String = "GAS_BLOCK",
        val houseRoofMaterial: String = "METAL_TILE",
        val floorsJson: String = "[]",
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val syncedAt: Long = 0L
    ) {
        fun toProject(): Project {
            val floorList = try {
                com.google.gson.Gson().fromJson(floorsJson, 
                    Array<ru.company.izhs_planner.domain.model.Floor>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
            
            return Project(
                id = id,
                name = name,
                description = description,
                plot = ru.company.izhs_planner.domain.model.Plot(
                    width = plotWidth,
                    length = plotLength,
                    slope = plotSlope,
                    area = plotArea
                ),
                house = ru.company.izhs_planner.domain.model.House(
                    id = id,
                    width = houseWidth,
                    length = houseLength,
                    floors = houseFloors,
                    floorHeight = houseFloorHeight,
                    roofType = try { 
                        ru.company.izhs_planner.domain.model.RoofType.valueOf(houseRoofType) 
                    } catch (e: Exception) { 
                        ru.company.izhs_planner.domain.model.RoofType.GABLED 
                    },
                    foundationType = try { 
                        ru.company.izhs_planner.domain.model.FoundationType.valueOf(houseFoundationType) 
                    } catch (e: Exception) { 
                        ru.company.izhs_planner.domain.model.FoundationType.STRIP 
                    },
                    wallMaterial = try { 
                        ru.company.izhs_planner.domain.model.WallMaterial.valueOf(houseWallMaterial) 
                    } catch (e: Exception) { 
                        ru.company.izhs_planner.domain.model.WallMaterial.GAS_BLOCK 
                    },
                    roofMaterial = try { 
                        ru.company.izhs_planner.domain.model.RoofMaterial.valueOf(houseRoofMaterial) 
                    } catch (e: Exception) { 
                        ru.company.izhs_planner.domain.model.RoofMaterial.METAL_TILE 
                    }
                ),
                floors = floorList,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }
}