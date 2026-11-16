package com.example.technews

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class NewsItem(
    val title: String,
    val source: String,
    @PrimaryKey val link: String,
    val time: Long,
    val summary: String = ""
)