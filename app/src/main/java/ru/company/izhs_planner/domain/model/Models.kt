package ru.company.izhs_planner.domain.model

import java.util.UUID

data class Plot(
    val id: String = UUID.randomUUID().toString(),
    val width: Float = 20f,
    val length: Float = 30f,
    val slope: Float = 0f,
    val area: Float = 6f
)

data class House(
    val id: String = UUID.randomUUID().toString(),
    val plotId: String = "",
    val width: Float = 10f,
    val length: Float = 12f,
    val floors: Int = 2,
    val floorHeight: Float = 3f,
    val roofType: RoofType = RoofType.GABLED,
    val foundationType: FoundationType = FoundationType.STRIP,
    val wallMaterial: WallMaterial = WallMaterial.GAS_BLOCK,
    val roofMaterial: RoofMaterial = RoofMaterial.METAL_TILE
)

enum class RoofType {
    FLAT,
    GABLED,
    HIPPED,
    MANSARD
}

enum class FoundationType {
    STRIP,
    TILE,
    COLUMN,
    SCREW_PILE
}

enum class WallMaterial(val displayName: String, val thermalConductivity: Float) {
    BRICK("Кирпич", 0.7f),
    BLOCK("Пеноблок", 0.18f),
    GAS_BLOCK("Газобетон", 0.12f),
    WOOD("Дерево", 0.15f),
    FRAME("Каркасный", 0.1f)
}

enum class RoofMaterial(val displayName: String, val weight: Float) {
    METAL_TILE("Металлочерепица", 5f),
    SOFT_ROOF("Мягкая кровля", 8f),
    ONDULINE("Ондулин", 3f),
    SLATE("Шифер", 10f),
    FLAT_ROOF("Плоская (наплавляемая)", 6f)
}

data class Floor(
    val number: Int,
    val rooms: List<Room> = emptyList()
)

data class Room(
    val id: String = UUID.randomUUID().toString(),
    val type: RoomType,
    val width: Float,
    val length: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val floorNumber: Int = 1
) {
    val area: Float get() = width * length
}

enum class RoomType(val displayName: String, val minArea: Float, val minWidth: Float, val minLength: Float) {
    KITCHEN("Кухня", 5f, 2f, 2f),
    LIVING_ROOM("Гостиная", 15f, 4f, 3.5f),
    BEDROOM("Спальня", 10f, 3f, 3f),
    BATHROOM("Ванная", 3f, 1.5f, 1.5f),
    TOILET("Туалет", 1.5f, 1f, 1f),
    HALLWAY("Прихожая", 3f, 1.5f, 1.5f),
    CORRIDOR("Коридор", 2f, 1f, 1f),
    CLOSET("Кладовая", 2f, 1m, 1m),
    GARAGE("Гараж", 15f, 3f, 4f),
    BOILER_ROOM("Котельная", 6f, 2f, 2f),
    UTILITY("Хозкомната", 4f, 2f, 2f)
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val plot: Plot = Plot(),
    val house: House = House(),
    val floors: List<Floor> = listOf(Floor(1), Floor(2)),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalArea: Float get() = house.width * house.length * house.floors
    val livingArea: Float get() = floors.sumOf { floor -> floor.rooms.filter { it.type != RoomType.CLOSET && it.type != RoomType.BOILER_ROOM && it.type != RoomType.UTILITY }.sumOf { it.area } }
}

data class MaterialCalculation(
    val materialName: String,
    val volume: Float,
    val area: Float,
    val count: Int,
    val unitPrice: Int,
    val totalPrice: Int
) {
    val weight: Float get() = volume * if (materialName.contains("бетон")) 2400f else 0f
}

data class Estimate(
    val materials: List<MaterialCalculation>,
    val totalCost: Int
)

data class CodeCheckResult(
    val checkName: String,
    val status: CheckStatus,
    val message: String
)

enum class CheckStatus {
    PASS,
    WARNING,
    ERROR
}