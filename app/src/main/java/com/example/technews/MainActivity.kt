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
    var text by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        val news = getAllNews()
        news.forEach { Log.d("NEWS", "${it.source}: ${it.title}") }
        text = if (news.isEmpty()) "No news. Check your internet." else news.joinToString("\n\n") { "${it.source}: ${it.title}" }
    }

    Text(text, modifier.verticalScroll(rememberScrollState()).padding(16.dp))
}