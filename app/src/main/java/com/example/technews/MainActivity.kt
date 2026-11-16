package com.example.technews

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.technews.getAllNews
import com.example.technews.ui.theme.TechnewsTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TechnewsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { pad ->
                    NewsText(Modifier.padding(pad))
                }
            }
        }
    }
}

@Composable
fun NewsText(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val dao = remember { Room.databaseBuilder(ctx, NewsDb::class.java, "news.db").build().dao() }
    val news by remember { dao.all() }.collectAsState(emptyList())
    var msg by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        val fresh = getAllNews()
        dao.save(fresh)
        dao.trim()
        if (fresh.isEmpty()) msg = "No news. Check your internet."
    }

    val text = if (news.isEmpty()) msg else "${news.size} saved\n\n" + news.joinToString("\n\n") { "${it.source}: ${it.title}\n${it.summary}" }
    Text(text, modifier.verticalScroll(rememberScrollState()).padding(16.dp))
}