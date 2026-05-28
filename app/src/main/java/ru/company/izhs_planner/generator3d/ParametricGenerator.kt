package ru.company.izhs_planner.generator3d

import ru.company.izhs_planner.domain.model.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

class ParametricGenerator {
    fun generateFloorPlan(
        house: House,
        rooms: List<RoomType>,
        availableArea: Float
    ): List<Room> {
        val houseArea = house.width * house.length
        val roomsToPlace = allocateRooms(rooms, houseArea)
        
        return when {
            house.floors == 1 -> generateSingleFloor(house, roomsToPlace)
            house.floors == 2 -> generateTwoFloor(house, roomsToPlace)
            else -> generateMultiFloor(house, roomsToPlace, house.floors)
        }
    }

    private fun allocateRooms(rooms: List<RoomType>, totalArea: Float): List<Pair<RoomType, Float>> {
        val minAreas = rooms.sumOf { it.minArea.toDouble() }.toFloat()
        val scale = if (minAreas > totalArea) totalArea / minAreas else 1f
        
        return rooms.map { type -> 
            type to (type.minArea * scale).coerceAtLeast(type.minArea)
        }
    }

    private fun generateSingleFloor(house: House, rooms: List<Pair<RoomType, Float>>): List<Room> {
        val result = mutableListOf<Room>()
        var placedWidth = 0f
        var placedLength = 0f
        
        rooms.forEach { (type, targetArea) ->
            val (width, length) = calculateRoomDimensions(type, targetArea, house)
            
            val room = Room(
                type = type,
                width = width,
                length = length,
                x = placedWidth,
                y = 0f,
                floorNumber = 1
            )
            result.add(room)
            
            placedWidth += width + 0.2f
            
            if (placedWidth > house.width * 0.7f) {
                placedWidth = 0f
                placedLength += length + 0.3f
            }
        }
        
        return result
    }

    private fun generateTwoFloor(house: House, rooms: List<Pair<RoomType, Float>>): List<Room> {
        val firstFloorRooms = mutableListOf<Pair<RoomType, Float>>()
        val secondFloorRooms = mutableListOf<Pair<RoomType, Float>>()
        
        rooms.forEach { room ->
            when (room.first) {
                RoomType.KITCHEN, RoomType.LIVING_ROOM, RoomType.HALLWAY, 
                RoomType.BOILER_ROOM, RoomType.GARAGE -> firstFloorRooms.add(room)
                else -> secondFloorRooms.add(room)
            }
        }
        
        val firstFloorResult = generateFloorForLevel(house, firstFloorRooms, 1)
        val secondFloorResult = generateFloorForLevel(house, secondFloorRooms, 2)
        
        return firstFloorResult + secondFloorResult
    }

    private fun generateFloorForLevel(
        house: House,
        rooms: List<Pair<RoomType, Float>>,
        floorNumber: Int
    ): List<Room> {
        val result = mutableListOf<Room>()
        var currentX = 0f
        var currentY = 0f
        var maxYInRow = 0f
        
        rooms.forEach { (type, targetArea) ->
            val (width, length) = calculateRoomDimensions(type, targetArea, house)
            
            val room = Room(
                type = type,
                width = width,
                length = length,
                x = currentX,
                y = currentY,
                floorNumber = floorNumber
            )
            result.add(room)
            
            currentX += width + 0.15f
            maxYInRow = maxOf(maxYInRow, length)
            
            if (currentX > house.width * 0.8f) {
                currentX = 0f
                currentY += maxYInRow + 0.15f
                maxYInRow = 0f
            }
        }
        
        return result
    }

    private fun generateMultiFloor(
        house: House,
        rooms: List<Pair<RoomType, Float>>,
        floors: Int
    ): List<Room> {
        val result = mutableListOf<Room>()
        val roomsPerFloor = rooms.size / floors
        
        var startIndex = 0
        for (floorNum in 1..floors) {
            val endIndex = minOf(startIndex + roomsPerFloor, rooms.size)
            val floorRooms = rooms.subList(startIndex, endIndex)
            result.addAll(generateFloorForLevel(house, floorRooms, floorNum))
            startIndex = endIndex
        }
        
        return result
    }

    private fun calculateRoomDimensions(
        type: RoomType,
        targetArea: Float,
        house: House
    ): Pair<Float, Float> {
        val aspectRatio = when (type) {
            RoomType.LIVING_ROOM -> 1.5f
            RoomType.BEDROOM -> 1.2f
            RoomType.KITCHEN -> 1.0f
            else -> 1.0f
        }
        
        val width = (sqrt(targetArea * aspectRatio)).coerceIn(type.minWidth, house.width * 0.5f)
        val length = (targetArea / width).coerceIn(type.minLength, house.length * 0.6f)
        
        return width to length
    }

    fun calculateMaterials(house: House): Estimate {
        val materials = mutableListOf<MaterialCalculation>()
        
        val foundationVolume = calculateFoundationVolume(house)
        materials.add(MaterialCalculation(
            materialName = "Бетон М200 (фундамент)",
            volume = foundationVolume,
            area = 0f,
            count = 0,
            unitPrice = 4500,
            totalPrice = (foundationVolume * 4500).toInt()
        ))
        
        val wallVolume = calculateWallVolume(house)
        materials.add(MaterialCalculation(
            materialName = "Газобетон (стены)",
            volume = wallVolume,
            area = house.width * house.length * house.floors * 0.3f,
            count = 0,
            unitPrice = 3500,
            totalPrice = (wallVolume * 3500).toInt()
        ))
        
        val roofArea = calculateRoofArea(house)
        materials.add(MaterialCalculation(
            materialName = "Металлочерепица",
            volume = 0f,
            area = roofArea,
            count = 0,
            unitPrice = 550,
            totalPrice = (roofArea * 550).toInt()
        ))
        
        val floorArea = house.width * house.length * house.floors
        materials.add(MaterialCalculation(
            materialName = "Плиты перекрытия",
            volume = 0f,
            area = floorArea,
            count = (floorArea / 6).toInt() + 1,
            unitPrice = 2500,
            totalPrice = ((floorArea / 6).toInt() + 1) * 2500
        ))
        
        val totalCost = materials.sumOf { it.totalPrice }
        
        return Estimate(materials = materials, totalCost = totalCost)
    }

    private fun calculateFoundationVolume(house: House): Float {
        val perimeters = (house.width + house.length) * 2
        val depth = when (house.foundationType) {
            FoundationType.STRIP -> 1.5f
            FoundationType.TILE -> 0.3f
            FoundationType.COLUMN -> 2f
            FoundationType.SCREW_PILE -> 2.5f
        }
        val width = 0.5f
        return perimeters * width * depth
    }

    private fun calculateWallVolume(house: House): Float {
        val wallHeight = house.floorHeight
        val wallThickness = 0.3f
        val perimeter = (house.width + house.length) * 2
        return perimeter * wallHeight * wallThickness * house.floors
    }

    private fun calculateRoofArea(house: House): Float {
        return when (house.roofType) {
            RoofType.FLAT -> house.width * house.length
            RoofType.GABLED -> house.width * house.length * 1.4f
            RoofType.HIPPED -> house.width * house.length * 1.3f
            RoofType.MANSARD -> house.width * house.length * 1.5f
        }
    }
}