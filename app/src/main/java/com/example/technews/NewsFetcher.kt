package com.example.technews

import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.text.Html
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters


fun text(e: Element, tag: String): String =
    e.getElementsByTagName(tag).item(0)?.textContent ?: ""

fun safe(name: String, load: () -> List<NewsItem>): List<NewsItem> =
    try { load() } catch (e: Exception) { Log.e("NEWS", "$name failed: ${e.message}"); emptyList() }

fun clean(s: String) = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()
    .replace("\uFFFC", "").replace(Regex("\\s+"), " ").trim().take(200)

fun toMillis(date: String): Long {
    val formats = listOf("EEE, dd MMM yyyy HH:mm:ss Z", "yyyy-MM-dd'T'HH:mm:ssXXX")
    for (f in formats) {
        try {
            return SimpleDateFormat(f, Locale.US).parse(date.trim())!!.time
        } catch (e: Exception) { }
    }
    return System.currentTimeMillis() // couldn't read the date, use now
}

fun getRss(name: String, url: String): List<NewsItem> {
    val xml = URL(url).readText()
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(InputSource(StringReader(xml)))
    // RSS feeds use <item>, Atom feeds (like The Verge) use <entry>
    var nodes = doc.getElementsByTagName("item")
    if (nodes.length == 0) nodes = doc.getElementsByTagName("entry")

    val list = mutableListOf<NewsItem>()
    for (i in 0 until minOf(nodes.length, 15)) {
        val e = nodes.item(i) as Element
        val linkTag = e.getElementsByTagName("link").item(0) as? Element
        val href = linkTag?.getAttribute("href") ?: ""   // Atom puts the link here
        val link = if (href.isNotEmpty()) href else linkTag?.textContent ?: ""
        val date = text(e, "pubDate").ifEmpty { text(e, "published") }
            .ifEmpty { text(e, "updated") }
        val sum = clean(text(e, "description").ifEmpty { text(e, "summary") }.ifEmpty { text(e, "content") })
        list.add(NewsItem(text(e, "title").trim(), name, link.trim(), toMillis(date), sum))
    }
    return list
}

fun getHackerNews(): List<NewsItem> {
    val ids = JSONArray(URL("https://hacker-news.firebaseio.com/v0/topstories.json").readText())
    val list = mutableListOf<NewsItem>()
    for (i in 0 until 15) {
        val id = ids.getInt(i)
        val o = JSONObject(URL("https://hacker-news.firebaseio.com/v0/item/$id.json").readText())
        // some posts (like "Ask HN") have no outside link, so use the HN page
        val link = o.optString("url", "https://news.ycombinator.com/item?id=$id")
        list.add(NewsItem(o.optString("title"), "Hacker News", link, o.getLong("time") * 1000, URL(link).host.removePrefix("www.")))
    }
    return list
}

suspend fun getAllNews(): List<NewsItem> = withContext(Dispatchers.IO) {
    val all = mutableListOf<NewsItem>()
    all += safe("The Verge") { getRss("The Verge", "https://www.theverge.com/rss/index.xml") }
    all += safe("TechCrunch") { getRss("TechCrunch", "https://techcrunch.com/feed/") }
    all += safe("Ars Technica") { getRss("Ars Technica", "https://feeds.arstechnica.com/arstechnica/index") }
    all += safe("Wired") { getRss("Wired", "https://www.wired.com/feed/rss") }
    all += safe("Hacker News") { getHackerNews() }
    all.sortedByDescending { it.time }
}
class RefreshWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val news = getAllNews()
        Log.d("REFRESH", "downloaded ${news.size}")
        if (news.isNotEmpty()) {
            val dao = Room.databaseBuilder(applicationContext, NewsDb::class.java, "news.db").build().dao()
            dao.save(news)
            dao.trim()
            NewsWidget().updateAll(applicationContext)
            Log.d("REFRESH", "saved and widget updated")
        }
        return Result.success()
    }
}