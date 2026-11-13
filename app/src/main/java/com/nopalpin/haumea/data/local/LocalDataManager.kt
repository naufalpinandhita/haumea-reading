package com.nopalpin.haumea.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class BookmarkItem(
    val slug: String,
    val title: String,
    val thumbnail: String,
    val type: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class HistoryItem(
    val slug: String,
    val title: String,
    val thumbnail: String,
    val type: String,
    val lastChapterId: String = "",
    val lastChapterTitle: String = "",
    val lastPage: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

class LocalDataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("haumea_local_db", Context.MODE_PRIVATE)
    private val gson = Gson()

    // --- BOOKMARKS ---
    fun getBookmarks(): List<BookmarkItem> {
        val json = prefs.getString("bookmarks", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<BookmarkItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun isBookmarked(slug: String): Boolean {
        return getBookmarks().any { it.slug == slug }
    }

    fun toggleBookmark(item: BookmarkItem): Boolean {
        val list = getBookmarks().toMutableList()
        val existingIdx = list.indexOfFirst { it.slug == item.slug }
        val isAdded: Boolean
        if (existingIdx != -1) {
            list.removeAt(existingIdx)
            isAdded = false
        } else {
            list.add(0, item)
            isAdded = true
        }
        prefs.edit().putString("bookmarks", gson.toJson(list)).apply()
        return isAdded
    }

    fun removeBookmark(slug: String) {
        val list = getBookmarks().filterNot { it.slug == slug }
        prefs.edit().putString("bookmarks", gson.toJson(list)).apply()
    }

    // --- HISTORY ---
    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("history", "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<HistoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addOrUpdateHistory(item: HistoryItem) {
        val list = getHistory().filterNot { it.slug == item.slug }.toMutableList()
        list.add(0, item)
        val trimmed = if (list.size > 100) list.take(100) else list
        prefs.edit().putString("history", gson.toJson(trimmed)).apply()
    }

    fun removeHistory(slug: String) {
        val list = getHistory().filterNot { it.slug == slug }
        prefs.edit().putString("history", gson.toJson(list)).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }
}
