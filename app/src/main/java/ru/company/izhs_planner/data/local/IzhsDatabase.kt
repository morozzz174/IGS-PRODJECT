package ru.company.izhs_planner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.company.izhs_planner.data.local.dao.ChatDao
import ru.company.izhs_planner.data.local.dao.ProjectDao
import ru.company.izhs_planner.data.local.entity.ChatSessionEntity
import ru.company.izhs_planner.data.local.entity.ProjectEntity

@Database(
    entities = [ProjectEntity::class, ChatSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IzhsDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chatDao(): ChatDao

    companion object {
        private var INSTANCE: IzhsDatabase? = null

        fun getDatabase(context: Context): IzhsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IzhsDatabase::class.java,
                    "izhs_planner_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}