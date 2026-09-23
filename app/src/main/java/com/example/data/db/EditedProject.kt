package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edited_projects")
data class EditedProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val imagePath: String,
    val thumbnailPath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val filterName: String = "Original",
    val filterIntensity: Float = 1.0f,
    val bgMode: String = "ORIGINAL", // ORIGINAL, REMOVED, SOLID, BLUR, STUDIO
    val bgParam: String = "",
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val exposure: Float = 0f,
    val warmth: Float = 0f,
    val saturation: Float = 0f,
    val skinSmooth: Float = 0f,
    val faceGlow: Float = 0f,
    val vignette: Float = 0f,
    val aiSummary: String = ""
)
