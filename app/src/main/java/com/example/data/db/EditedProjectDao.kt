package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EditedProjectDao {
    @Query("SELECT * FROM edited_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<EditedProject>>

    @Query("SELECT * FROM edited_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): EditedProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: EditedProject): Long

    @Delete
    suspend fun deleteProject(project: EditedProject)

    @Query("DELETE FROM edited_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}
