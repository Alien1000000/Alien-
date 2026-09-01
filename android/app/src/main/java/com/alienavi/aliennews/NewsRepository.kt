package com.alienavi.aliennews

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NewsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("alien_news", Context.MODE_PRIVATE)

    fun fetch(language: String): List<Article> {
        val connection = URL("https://aliennews.co.il/api/app-feed?lang=$language").openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "AlienNewsAndroid/1.0")
        return try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val articles = parse(body)
            prefs.edit().putString("feed_$language", body).apply()
            articles
        } finally {
            connection.disconnect()
        }
    }

    fun cached(language: String): List<Article> = prefs.getString("feed_$language", null)
        ?.let { runCatching { parse(it) }.getOrDefault(emptyList()) }
        .orEmpty()

    fun savedIds(): Set<String> = prefs.getStringSet("saved", emptySet()).orEmpty()

    fun toggleSaved(article: Article) {
        val key = "${article.language}:${article.id}"
        val updated = savedIds().toMutableSet().apply { if (!add(key)) remove(key) }
        prefs.edit().putStringSet("saved", updated).apply()
    }

    fun language(): String = prefs.getString("language", "he")
        ?.takeIf { it == "he" || it == "en" }
        ?: "he"

    fun setLanguage(language: String) {
        require(language == "he" || language == "en")
        prefs.edit().putString("language", language).apply()
    }

    fun notificationsEnabled(): Boolean = prefs.getBoolean("notifications", false)
    fun setNotificationsEnabled(value: Boolean) = prefs.edit().putBoolean("notifications", value).apply()

    fun lastArticleId(): String? = prefs.getString("last_article", null)
    fun setLastArticleId(id: String) = prefs.edit().putString("last_article", id).apply()

    private fun parse(body: String): List<Article> {
        val items = JSONObject(body).getJSONArray("items")
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                add(
                    Article(
                        id = item.getString("id"),
                        language = item.getString("language"),
                        title = item.getString("title"),
                        summary = item.getString("summary"),
                        category = item.getString("category"),
                        evidenceLevel = item.getString("evidenceLevel"),
                        publishedAt = item.getString("publishedAt"),
                        imageUrl = item.getString("imageUrl"),
                        imageAlt = item.getString("imageAlt"),
                        articleUrl = item.getString("articleUrl"),
                        sourceUrl = item.getString("sourceUrl"),
                        readingMinutes = item.getInt("readingMinutes"),
                    ),
                )
            }
        }
    }
}
