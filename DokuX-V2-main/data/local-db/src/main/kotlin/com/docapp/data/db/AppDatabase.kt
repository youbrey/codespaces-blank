package com.docapp.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val sizeBytes: Long,
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "revisions",
    foreignKeys = [ForeignKey(
        entity = DocumentEntity::class,
        parentColumns = ["id"], childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("documentId")]
)
data class RevisionEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val timestamp: Long,
    val snapshotPath: String,
    val label: String? = null
)

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY modifiedAt DESC")
    suspend fun getAllOnce(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE title LIKE '%' || :query || '%' ORDER BY modifiedAt DESC")
    suspend fun search(query: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): DocumentEntity?

    @Query("UPDATE documents SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: String, isFav: Boolean)

    @Upsert
    suspend fun upsert(doc: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM revisions WHERE documentId = :documentId ORDER BY timestamp DESC")
    suspend fun listFor(documentId: String): List<RevisionEntity>

    @Insert
    suspend fun insert(revision: RevisionEntity)

    /** Kebijakan retensi: simpan maksimal N revisi terakhir per dokumen. */
    @Query("""
        DELETE FROM revisions WHERE id IN (
            SELECT id FROM revisions WHERE documentId = :documentId
            ORDER BY timestamp DESC LIMIT -1 OFFSET :keepCount
        )
    """)
    suspend fun pruneOld(documentId: String, keepCount: Int = 10)
}

@Database(entities = [DocumentEntity::class, RevisionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun revisionDao(): RevisionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /** Singleton manual — dipakai konteks tanpa Hilt graph (mis. WorkManager CoroutineWorker). */
        fun instance(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "doceditor.db")
                    .build().also { INSTANCE = it }
            }
    }
}
