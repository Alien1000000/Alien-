package com.alienavi.aliennews

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private var notificationPermissionResult: ((Boolean) -> Unit)? = null
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionResult?.invoke(granted)
        notificationPermissionResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent.dataString?.takeIf { it.startsWith("https://aliennews.co.il/") }?.let { articleUrl ->
            startActivity(Intent(this, ArticleReaderActivity::class.java).apply {
                putExtra("url", articleUrl)
                putExtra("title", "Alien News")
            })
        }
        setContent {
            AlienNewsTheme {
                AlienNewsAppScreen(
                    onExit = ::finish,
                    requestNotificationPermission = { onResult ->
                        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            onResult(true)
                        } else {
                            notificationPermissionResult = onResult
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AlienNewsAppScreen(
    viewModel: NewsViewModel = viewModel(),
    onExit: () -> Unit,
    requestNotificationPermission: ((Boolean) -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val direction = if (state.language == "he") LayoutDirection.Rtl else LayoutDirection.Ltr
    val context = LocalContext.current
    var showExitConfirmation by remember { mutableStateOf(false) }

    BackHandler {
        if (state.tab != AppTab.HOME) viewModel.setTab(AppTab.HOME)
        else showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(if (state.language == "he") "לצאת מהאפליקציה?" else "Exit the app?") },
            text = { Text(if (state.language == "he") "לחיצה על יציאה תסגור את Alien News." else "Exit will close Alien News.") },
            confirmButton = {
                TextButton(onClick = onExit) { Text(if (state.language == "he") "יציאה" else "Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text(if (state.language == "he") "ביטול" else "Cancel") }
            },
        )
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides direction) {
        Scaffold(
            containerColor = Cream,
            topBar = { BrandHeader(state.offline, state.loading, state.language, viewModel::refresh) },
            bottomBar = { BottomNavigation(state.tab, state.language, viewModel::setTab) },
        ) { padding ->
            AnimatedContent(targetState = state.tab, label = "tab") { tab ->
                when (tab) {
                    AppTab.HOME -> HomeScreen(state, padding, viewModel, onOpen = { openArticle(context, it) })
                    AppTab.SEARCH -> SearchScreen(state, padding, viewModel, onOpen = { openArticle(context, it) })
                    AppTab.SAVED -> SavedScreen(state, padding, viewModel, onOpen = { openArticle(context, it) })
                    AppTab.SETTINGS -> SettingsScreen(state, padding, viewModel, requestNotificationPermission)
                }
            }
        }
    }
}

private fun openArticle(context: android.content.Context, article: Article) {
    context.startActivity(Intent(context, ArticleReaderActivity::class.java).apply {
        putExtra("url", article.articleUrl)
        putExtra("title", article.title)
        putExtra("language", article.language)
    })
}

@Composable
private fun BrandHeader(offline: Boolean, loading: Boolean, language: String, refresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Navy).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(42.dp).clip(CircleShape))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text("ALIEN NEWS", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (offline) {
                        if (language == "he") "מצב לא מקוון · מהמטמון" else "OFFLINE · CACHED"
                    } else "BY ALIEN AVI",
                    color = if (offline) Sage else Gold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            IconButton(onClick = refresh) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Gold, strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Refresh, "רענון", tint = Color.White)
            }
        }
    }
}

@Composable
private fun BottomNavigation(tab: AppTab, language: String, onTab: (AppTab) -> Unit) {
    val labels = if (language == "he") listOf("ראשי", "חיפוש", "שמורים", "הגדרות") else listOf("Home", "Search", "Saved", "Settings")
    val tabs = listOf(AppTab.HOME to Icons.Outlined.Home, AppTab.SEARCH to Icons.Outlined.Search, AppTab.SAVED to Icons.Outlined.Bookmark, AppTab.SETTINGS to Icons.Outlined.Settings)
    NavigationBar(containerColor = Paper, modifier = Modifier.navigationBarsPadding()) {
        tabs.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = tab == item.first,
                onClick = { onTab(item.first) },
                icon = { Icon(item.second, labels[index]) },
                label = { Text(labels[index]) },
            )
        }
    }
}

@Composable
private fun HomeScreen(state: NewsUiState, padding: PaddingValues, viewModel: NewsViewModel, onOpen: (Article) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.loadFailed) {
            item {
                LoadErrorState(
                    message = if (state.language == "he") "לא הצלחנו לטעון את החדשות. בדוק את החיבור ונסה שוב." else "We could not load the news. Check your connection and try again.",
                    retryLabel = if (state.language == "he") "נסה שוב" else "Try again",
                    retry = viewModel::refresh,
                )
            }
        } else item { DailyBrief(state) }
        val visible = state.articles
        visible.firstOrNull()?.let { featured -> item { FeaturedArticle(featured, state, viewModel, onOpen) } }
        items(visible.drop(1), key = { "${it.language}:${it.id}" }) { article -> ArticleCard(article, state, viewModel, onOpen) }
        if (!state.loading && visible.isEmpty()) item { EmptyState(if (state.language == "he") "אין כתבות להצגה כרגע" else "No stories available") }
    }
}

@Composable
private fun DailyBrief(state: NewsUiState) {
    val newest = state.articles.firstOrNull()
    Box(
        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Navy, DeepBlue))).padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Column {
            Text(if (state.language == "he") "התדריך היומי" else "DAILY BRIEF", color = Gold, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                newest?.title ?: if (state.language == "he") "החדשות מתעדכנות" else "Updating the latest reports",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                BriefStat(state.articles.size.toString(), if (state.language == "he") "כתבות" else "stories")
                BriefStat(state.savedIds.size.toString(), if (state.language == "he") "שמורים" else "saved")
            }
        }
    }
}

@Composable private fun BriefStat(value: String, label: String) = Column { Text(value, color = Gold, style = MaterialTheme.typography.titleLarge); Text(label, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodyMedium) }

@Composable
private fun FeaturedArticle(article: Article, state: NewsUiState, viewModel: NewsViewModel, onOpen: (Article) -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth().clickable { onOpen(article) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column {
            AsyncImage(article.imageUrl, article.imageAlt, Modifier.fillMaxWidth().height(222.dp).background(Mist), contentScale = ContentScale.Crop)
            Column(Modifier.padding(18.dp)) {
                EvidenceRow(article)
                Spacer(Modifier.height(10.dp))
                Text(article.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(9.dp))
                Text(article.summary, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis, color = Ink.copy(alpha = .78f))
                ArticleActions(article, state, viewModel, onOpen)
            }
        }
    }
}

@Composable
private fun ArticleCard(article: Article, state: NewsUiState, viewModel: NewsViewModel, onOpen: (Article) -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 18.dp).fillMaxWidth().clickable { onOpen(article) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Paper),
    ) {
        Column {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                AsyncImage(article.imageUrl, article.imageAlt, Modifier.size(104.dp).clip(RoundedCornerShape(14.dp)).background(Mist), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(article.category, color = Gold, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(5.dp))
                    Text(article.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text("${article.readingMinutes} ${if (state.language == "he") "דקות קריאה" else "min read"}", color = Ink.copy(alpha = .55f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color = Mist)
            ArticleActions(article, state, viewModel, onOpen)
        }
    }
}

@Composable
private fun EvidenceRow(article: Article) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(Sage, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(article.evidenceLevel, color = DeepBlue, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.weight(1f))
        Text(formatPublishedDate(article.publishedAt), color = Ink.copy(alpha = .5f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ArticleActions(article: Article, state: NewsUiState, viewModel: NewsViewModel, onOpen: (Article) -> Unit) {
    val context = LocalContext.current
    val saved = state.savedIds.contains("${article.language}:${article.id}")
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { viewModel.toggleSaved(article) }) { Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (saved) "הסר משמורים" else "שמור כתבה", tint = if (saved) Gold else Ink) }
        IconButton(onClick = {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, article.title); putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.articleUrl}") }, null))
        }) { Icon(Icons.Outlined.Share, "שיתוף") }
        Spacer(Modifier.weight(1f))
        Row(Modifier.clickable { onOpen(article) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.language == "he") "לכתבה" else "Read", color = Navy, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(4.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, "לכתבה", Modifier.size(18.dp), tint = Gold)
        }
    }
}

@Composable
private fun SearchScreen(state: NewsUiState, padding: PaddingValues, viewModel: NewsViewModel, onOpen: (Article) -> Unit) {
    val filtered = state.articles.filter { article -> state.query.isBlank() || article.title.contains(state.query, true) || article.summary.contains(state.query, true) || article.category.contains(state.query, true) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text(if (state.language == "he") "חיפוש חכם" else "Smart search", style = MaterialTheme.typography.displaySmall) }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text(if (state.language == "he") "נושא, מקור או מילה מתוך כתבה" else "Topic, source or keyword") },
                shape = RoundedCornerShape(18.dp),
            )
        }
        if (state.query.isNotBlank()) item { Text("${filtered.size} ${if (state.language == "he") "תוצאות" else "results"}", color = Gold, style = MaterialTheme.typography.labelLarge) }
        items(filtered, key = { "search:${it.language}:${it.id}" }) { ArticleCard(it, state, viewModel, onOpen) }
        if (filtered.isEmpty()) item { EmptyState(if (state.language == "he") "לא מצאתי כתבה מתאימה" else "No matching stories") }
    }
}

@Composable
private fun SavedScreen(state: NewsUiState, padding: PaddingValues, viewModel: NewsViewModel, onOpen: (Article) -> Unit) {
    val saved = state.articles.filter { state.savedIds.contains("${it.language}:${it.id}") }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item { Text(if (state.language == "he") "הספרייה שלי" else "My library", style = MaterialTheme.typography.displaySmall) }
        item { Text(if (state.language == "he") "כתבות שסימנת נשמרות גם לאחר סגירת האפליקציה." else "Bookmarked stories stay saved on this device.", color = Ink.copy(alpha = .65f)) }
        items(saved, key = { "saved:${it.language}:${it.id}" }) { ArticleCard(it, state, viewModel, onOpen) }
        if (saved.isEmpty()) item { EmptyState(if (state.language == "he") "עדיין לא שמרת כתבות" else "No saved stories yet") }
    }
}

@Composable
private fun SettingsScreen(state: NewsUiState, padding: PaddingValues, viewModel: NewsViewModel, requestNotificationPermission: ((Boolean) -> Unit) -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(if (state.language == "he") "הגדרות" else "Settings", style = MaterialTheme.typography.displaySmall) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Paper)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Language, null, tint = Gold)
                        Spacer(Modifier.width(14.dp))
                        Text(if (state.language == "he") "שפת האפליקציה" else "App language", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { viewModel.setLanguage("he") }, enabled = state.language != "he", modifier = Modifier.weight(1f)) { Text("עברית") }
                        Button(onClick = { viewModel.setLanguage("en") }, enabled = state.language != "en", modifier = Modifier.weight(1f)) { Text("English") }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Paper)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Notifications, null, tint = Gold); Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(if (state.language == "he") "התראות חדשות" else "News alerts", style = MaterialTheme.typography.titleMedium); Text(if (state.language == "he") "רק כאשר מתפרסמת כתבה חדשה" else "Only when a new story is published", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = .6f)) }
                    Switch(
                        state.notifications,
                        onCheckedChange = { enabled ->
                            if (enabled) requestNotificationPermission { granted -> viewModel.setNotifications(granted) }
                            else viewModel.setNotifications(false)
                        },
                    )
                }
            }
        }
        item { SettingCard(Icons.Outlined.Share, if (state.language == "he") "שתף את Alien News" else "Share Alien News", "aliennews.co.il") { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://aliennews.co.il") }, null)) } }
        item { SettingCard(Icons.Outlined.Settings, if (state.language == "he") "פרטיות ותנאים" else "Privacy and terms", if (state.language == "he") "ללא הרשמה וללא מעקב אישי" else "No account and no personal tracking") { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://aliennews.co.il/privacy"))) } }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(78.dp).clip(CircleShape))
                Spacer(Modifier.height(10.dp)); Text("Alien News · ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium); Text("אבי מואס · Alien Avi", color = Gold)
            }
        }
    }
}

@Composable
private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = action), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Paper)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = .6f)) }; Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = Gold)
        }
    }
}

private fun formatPublishedDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}.getOrDefault(value)

@Composable
private fun EmptyState(message: String) {
    Column(Modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Search, null, Modifier.size(44.dp), tint = Gold); Spacer(Modifier.height(12.dp)); Text(message, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LoadErrorState(message: String, retryLabel: String, retry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Refresh, null, Modifier.size(44.dp), tint = Gold)
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(18.dp))
        Button(onClick = retry) { Text(retryLabel) }
    }
}
