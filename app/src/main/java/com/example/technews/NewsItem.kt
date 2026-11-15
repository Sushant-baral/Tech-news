package com.example.technews

data class NewsItem(
    val title: String,
    val source: String,
    val link: String,
    val time: Long   // milliseconds since 1970, easy to sort later
)