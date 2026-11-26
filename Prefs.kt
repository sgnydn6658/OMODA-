package com.aracekran.video

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val name: String, val url: String)

/** Kayıtlı siteler ve ayarlar (telefonda saklanır). */
object Prefs {
    private const val FILE = "arac_ekran_video"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_TEXT_ZOOM = "text_zoom"
    private const val KEY_DESKTOP = "desktop_mode"

    val DEFAULT_BOOKMARKS = listOf(
        Bookmark("YouTube", "https://m.youtube.com/"),
        Bookmark("Google", "https://www.google.com/")
    )

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun bookmarks(ctx: Context): MutableList<Bookmark> {
        val raw = sp(ctx).getString(KEY_BOOKMARKS, null) ?: return DEFAULT_BOOKMARKS.toMutableList()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Bookmark(o.getString("name"), o.getString("url"))
            }
        } catch (e: Exception) {
            DEFAULT_BOOKMARKS.toMutableList()
        }
    }

    fun saveBookmarks(ctx: Context, list: List<Bookmark>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
        sp(ctx).edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun homeUrl(ctx: Context): String = bookmarks(ctx).firstOrNull()?.url ?: DEFAULT_BOOKMARKS[0].url

    /** Yazı/sayfa büyüklüğü yüzdesi (100 = normal). */
    fun textZoom(ctx: Context): Int = sp(ctx).getInt(KEY_TEXT_ZOOM, 125)
    fun setTextZoom(ctx: Context, value: Int) = sp(ctx).edit().putInt(KEY_TEXT_ZOOM, value).apply()

    /** Masaüstü sitesi modu (bazı siteler geniş ekranda daha iyi görünür). */
    fun desktopMode(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_DESKTOP, false)
    fun setDesktopMode(ctx: Context, value: Boolean) = sp(ctx).edit().putBoolean(KEY_DESKTOP, value).apply()

    /**
     * Yazılan metni adrese çevirir: "youtube.com" -> https://youtube.com,
     * adres değilse Google'da arar.
     */
    fun normalizeUrl(input: String): String {
        val t = input.trim()
        if (t.isEmpty()) return t
        if (t.startsWith("http://", true) || t.startsWith("https://", true)) return t
        if (!t.contains(' ') && t.contains('.')) return "https://$t"
        return "https://www.google.com/search?q=" + Uri.encode(t)
    }
}
