package pl.rysiek.roadtune.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseConverters {
    @TypeConverter
    fun stateToString(state: DownloadState): String = state.name

    @TypeConverter
    fun stringToState(value: String): DownloadState = DownloadState.valueOf(value)
}

@Database(entities = [DownloadEntity::class], version = 2, exportSchema = false)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloads(): DownloadDao

    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "roadtune.db"
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE downloads ADD COLUMN playlistId TEXT")
                database.execSQL("ALTER TABLE downloads ADD COLUMN playlistTitle TEXT")
                database.execSQL("ALTER TABLE downloads ADD COLUMN playlistPosition INTEGER")
                database.execSQL("ALTER TABLE downloads ADD COLUMN playlistTotal INTEGER")
            }
        }
    }
}
