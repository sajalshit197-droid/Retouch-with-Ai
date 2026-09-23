package com.example.data.repository

import com.example.data.db.EditedProject
import com.example.data.db.EditedProjectDao
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val dao: EditedProjectDao) {
    val allProjects: Flow<List<EditedProject>> = dao.getAllProjects()

    suspend fun getProjectById(id: Long): EditedProject? = dao.getProjectById(id)

    suspend fun saveProject(project: EditedProject): Long = dao.insertProject(project)

    suspend fun deleteProject(project: EditedProject) = dao.deleteProject(project)

    suspend fun deleteProjectById(id: Long) = dao.deleteProjectById(id)
}
