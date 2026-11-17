package com.example.technews

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.room.Room

val bg = ColorProvider(day = Color.White, night = Color(0xFF202124))
val fg = ColorProvider(day = Color.Black, night = Color.White)
val key = intPreferencesKey("i")

class NewsWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = Room.databaseBuilder(context, NewsDb::class.java, "news.db").build().dao()
        val list = dao.list()
        provideContent {
            val i = currentState<Preferences>()[key] ?: 0
            val n = if (list.isEmpty()) null else list[i % list.size]
            Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(16.dp).padding(12.dp).clickable(actionRunCallback<NextAction>())) {
                if (n == null) {
                    Text("Open the app to load news", style = TextStyle(color = fg, fontSize = 14.sp))
                } else {
                    Text(n.title, maxLines = 3, style = TextStyle(color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Text(n.summary, maxLines = 3, style = TextStyle(color = fg, fontSize = 13.sp))
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${n.source} · ${timeAgo(n.time)}", style = TextStyle(color = fg, fontSize = 11.sp))
                        Spacer(GlanceModifier.defaultWeight())
                        Button("Read", actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.link))))
                    }
                }
            }
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { it[key] = (it[key] ?: 0) + 1 }
        NewsWidget().update(context, glanceId)
    }
}

class NewsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NewsWidget()
}