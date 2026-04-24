package ru.company.izhs_planner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val plotWidth: Float,
    val plotLength: Float,
    val plotSlope: Float,
    val plotArea: Float,
    val houseWidth: Float,
    val houseLength: Float,
    val houseFloors: Int,
    val houseFloorHeight: Float,
    val houseRoofType: String,
    val houseFoundationType: String,
    val houseWallMaterial: String,
    val houseRoofMaterial: String,
    val floorsJson: String,
    val createdAt: Long,
    val updatedAt: Long
)