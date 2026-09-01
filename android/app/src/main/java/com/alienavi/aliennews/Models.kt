package com.alienavi.aliennews

data class Article(
    val id: String,
    val language: String,
    val title: String,
    val summary: String,
    val category: String,
    val evidenceLevel: String,
    val publishedAt: String,
    val imageUrl: String,
    val imageAlt: String,
    val articleUrl: String,
    val sourceUrl: String,
    val readingMinutes: Int,
)

enum class AppTab { HOME, SEARCH, SAVED, SETTINGS }
