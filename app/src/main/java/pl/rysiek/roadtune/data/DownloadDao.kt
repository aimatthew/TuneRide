package pl.rysiek.roadtune.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DownloadEntity>)

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun get(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE playlistId = :playlistId ORDER BY playlistPosition ASC")
    suspend fun getPlaylistItems(playlistId: String): List<DownloadEntity>

    @Query("UPDATE downloads SET title = :title, uploader = :uploader, thumbnailUrl = :thumbnail WHERE id = :id")
    suspend fun updateMetadata(id: String, title: String, uploader: String?, thumbnail: String?)

    @Query("UPDATE downloads SET state = :state, progress = :progress, errorMessage = NULL WHERE id = :id")
    suspend fun updateProgress(id: String, state: DownloadState, progress: Int)

    @Query("UPDATE downloads SET state = 'COMPLETED', progress = 100, outputFileName = :fileName, outputUri = :uri, completedAt = :completedAt, errorMessage = NULL WHERE id = :id")
    suspend fun markCompleted(id: String, fileName: String, uri: String, completedAt: Long)

    @Query("UPDATE downloads SET state = 'FAILED', errorMessage = :message WHERE id = :id")
    suspend fun markFailed(id: String, message: String)

    @Query("UPDATE downloads SET state = 'FAILED', errorMessage = :message WHERE id = :id AND state IN ('QUEUED', 'PREPARING', 'DOWNLOADING', 'COPYING')")
    suspend fun cancelActiveItem(id: String, message: String)

    @Query("UPDATE downloads SET state = 'FAILED', errorMessage = :message WHERE playlistId = :playlistId AND state IN ('QUEUED', 'PREPARING', 'DOWNLOADING', 'COPYING')")
    suspend fun cancelActivePlaylist(playlistId: String, message: String)

    @Query("UPDATE downloads SET state = 'QUEUED', progress = 0, errorMessage = NULL, outputFileName = NULL, outputUri = NULL, completedAt = NULL, createdAt = :createdAt WHERE id = :id AND state = 'FAILED'")
    suspend fun prepareRetry(id: String, createdAt: Long)

    @Delete
    suspend fun delete(item: DownloadEntity)

    @Query("DELETE FROM downloads")
    suspend fun clear()
}
