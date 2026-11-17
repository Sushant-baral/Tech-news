package com.example.technews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.technews.ui.theme.TechnewsTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val req = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("refresh", ExistingPeriodicWorkPolicy.KEEP, req)
        setContent {
            TechnewsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { pad ->
                    NewsList(Modifier.padding(pad))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val dao = remember { Room.databaseBuilder(ctx, NewsDb::class.java, "news.db").build().dao() }
    val news by remember { dao.all() }.collectAsState(emptyList())
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    suspend fun refresh() {
        loading = true
        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val fresh = getAllNews().filter { it.time > since }
        dao.save(fresh)
        dao.deleteOld(since)
        dao.trim()
        failed = fresh.isEmpty()
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    PullToRefreshBox(isRefreshing = loading, onRefresh = { scope.launch { refresh() } }, modifier = modifier) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (news.isEmpty() && failed) {
                item { Text("No news. Check your internet, then pull down to refresh.", Modifier.padding(16.dp)) }
            }
            items(news, key = { it.link }) { n ->
                Card(Modifier.fillMaxWidth().clickable { uri.openUri(n.link) }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleMedium)
                        if (n.summary.isNotEmpty()) {
                            Text(n.summary, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("${n.source} · ${timeAgo(n.time)}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

fun timeAgo(time: Long): String {
    val m = (System.currentTimeMillis() - time) / 60000
    return when {
        m < 1 -> "just now"
        m < 60 -> "${m}m ago"
        m < 1440 -> "${m / 60}h ago"
        else -> "${m / 1440}d ago"
    }
}