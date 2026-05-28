package ru.company.izhs_planner.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.company.izhs_planner.data.local.IzhsDatabase
import ru.company.izhs_planner.data.local.entity.ChatSessionEntity
import ru.company.izhs_planner.data.local.entity.ProjectEntity
import ru.company.izhs_planner.domain.model.*
import ru.company.izhs_planner.domain.model.chat.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class ProjectRepository(private val database: IzhsDatabase) {
    private val projectDao = database.projectDao()
    private val gson = Gson()

    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProjectById(id: String): Project? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    suspend fun saveProject(project: Project) {
        projectDao.insertProject(project.toEntity())
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun getProjectCount(): Int {
        return projectDao.getProjectCount()
    }

    private fun ProjectEntity.toDomain(): Project {
        val floorListType = object : TypeToken<List<Floor>>() {}.type
        val floors: List<Floor> = try {
            gson.fromJson(floorsJson, floorListType)
        } catch (e: Exception) {
            listOf(Floor(1, emptyList()), Floor(2, emptyList()))
        }
        return Project(
            id = id,
            name = name,
            description = description,
            plot = Plot(
                width = plotWidth,
                length = plotLength,
                slope = plotSlope,
                area = plotArea
            ),
            house = House(
                id = id,
                width = houseWidth,
                length = houseLength,
                floors = houseFloors,
                floorHeight = houseFloorHeight,
                roofType = RoofType.valueOf(houseRoofType),
                foundationType = FoundationType.valueOf(houseFoundationType),
                wallMaterial = WallMaterial.valueOf(houseWallMaterial),
                roofMaterial = RoofMaterial.valueOf(houseRoofMaterial)
            ),
            floors = floors,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Project.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            description = description,
            plotWidth = plot.width,
            plotLength = plot.length,
            plotSlope = plot.slope,
            plotArea = plot.area,
            houseWidth = house.width,
            houseLength = house.length,
            houseFloors = house.floors,
            houseFloorHeight = house.floorHeight,
            houseRoofType = house.roofType.name,
            houseFoundationType = house.foundationType.name,
            houseWallMaterial = house.wallMaterial.name,
            houseRoofMaterial = house.roofMaterial.name,
            floorsJson = gson.toJson(floors),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

class ChatRepository(private val database: IzhsDatabase) {
    private val chatDao = database.chatDao()
    private val gson = Gson()

    fun getAllSessions(): Flow<List<ChatSession>> {
        return chatDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getSessionsByType(agentType: AgentType): Flow<List<ChatSession>> {
        return chatDao.getSessionsByType(agentType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getSessionById(id: String): ChatSession? {
        return chatDao.getSessionById(id)?.toDomain()
    }

    suspend fun saveSession(session: ChatSession) {
        chatDao.insertSession(session.toEntity())
    }

    suspend fun deleteSession(id: String) {
        chatDao.deleteSessionById(id)
    }

    suspend fun clearAllSessions() {
        chatDao.clearAllSessions()
    }

    private fun ChatSessionEntity.toDomain(): ChatSession {
        val messageListType = object : TypeToken<List<ChatMessage>>() {}.type
        val messages: List<ChatMessage> = try {
            gson.fromJson(messagesJson, messageListType)
        } catch (e: Exception) {
            emptyList()
        }
        return ChatSession(
            id = id,
            agentType = AgentType.valueOf(agentType),
            messages = messages,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ChatSession.toEntity(): ChatSessionEntity {
        return ChatSessionEntity(
            id = id,
            agentType = agentType.name,
            messagesJson = gson.toJson(messages),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}