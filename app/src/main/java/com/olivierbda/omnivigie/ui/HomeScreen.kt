package com.olivierbda.omnivigie.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.ui.theme.*
import com.olivierbda.omnivigie.ui.viewmodel.HomeViewModel
import com.olivierbda.omnivigie.ui.auth.NotebookAuthActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    var activeTab by remember { mutableStateOf(0) }
    var selectedThemeForDetail by remember { mutableStateOf<String?>(null) }
    
    val articles by viewModel.articles.collectAsState()
    val unsentArticles by viewModel.unsentArticles.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val notebookStatus by viewModel.notebookStatus.collectAsState()
    val recentNotebooks by viewModel.recentNotebooks.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh notebook status when returning to app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotebookStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (selectedThemeForDetail != null) {
        CurationDetailScreen(
            theme = selectedThemeForDetail!!,
            articles = unsentArticles,
            viewModel = viewModel,
            onBack = { selectedThemeForDetail = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(CosmicPrimary, CosmicSecondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "OMNIVIGIE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                color = TextPrimary
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CosmicBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CosmicSurface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicPrimary,
                        selectedTextColor = CosmicPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = CosmicSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Curation") },
                    label = { Text("Curation") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicPrimary,
                        selectedTextColor = CosmicPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = CosmicSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Paramètres") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CosmicPrimary,
                        selectedTextColor = CosmicPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = CosmicSurfaceVariant
                    )
                )
            }
        },
        containerColor = CosmicBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CosmicBackground,
                            Color(0xFF07040E)
                        )
                    )
                )
        ) {
            when (activeTab) {
                0 -> DashboardTab(
                    articles = articles,
                    syncStatus = syncStatus,
                    notebookStatus = notebookStatus,
                    recentNotebooks = recentNotebooks,
                    onSyncClick = { viewModel.syncGmail(context as Activity) },
                    onQualifyClick = { viewModel.qualifyArticles() },
                    onNotebookClick = {
                        context.startActivity(Intent(context, NotebookAuthActivity::class.java))
                    }
                )
                1 -> CurationTab(
                    articles = unsentArticles,
                    onCleanupClick = { viewModel.cleanupArticles() },
                    onThemeClick = { theme -> selectedThemeForDetail = theme }
                )
                2 -> SettingsTab(viewModel)
            }
        }
    }
}

@Composable
fun DashboardTab(
    articles: List<ArticleEntity>,
    syncStatus: String?,
    notebookStatus: String,
    recentNotebooks: List<Pair<String, String>>,
    onSyncClick: () -> Unit,
    onQualifyClick: () -> Unit,
    onNotebookClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcoming card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(GradientPurpleToIndigo),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Bonjour, Olivier",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Votre assistant de veille est prêt. ${articles.size} nouveaux articles attendent votre qualification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    
                    syncStatus?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmicTertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (recentNotebooks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Derniers Carnets :",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextAccent
                        )
                        recentNotebooks.forEach { notebook ->
                            Text(
                                text = "• ${notebook.second}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Diagnostic indicators
        item {
            Text(
                text = "Diagnostics Système",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DiagnosticRow(
                    title = "Compte Gmail API",
                    status = "Connecté (olivier@bda.com)",
                    statusColor = SystemGreen,
                    icon = Icons.Default.Email
                )
                DiagnosticRow(
                    title = "Google Gemini SDK",
                    status = "Prêt (Clé configurée)",
                    statusColor = SystemGreen,
                    icon = Icons.Default.Psychology
                )
                DiagnosticRow(
                    title = "Google NotebookLM",
                    status = notebookStatus,
                    statusColor = if (notebookStatus == "Connecté") SystemGreen else SystemRed,
                    icon = Icons.Default.CloudQueue,
                    onClick = onNotebookClick
                )
            }
        }

        // Core Actions Card
        item {
            Text(
                text = "Actions Rapides",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(
                    title = "Sync Gmail",
                    icon = Icons.Default.Sync,
                    onClick = onSyncClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Qualifier (LLM)",
                    icon = Icons.Default.CheckCircle,
                    onClick = onQualifyClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent stats section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(value = "${articles.size}", label = "Articles Lus", modifier = Modifier.weight(1f))
                StatCard(value = "${articles.count { it.aiInterest == true }}", label = "Pertinents", modifier = Modifier.weight(1f))
                StatCard(value = "0", label = "Carnets LM", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DiagnosticRow(
    title: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CosmicSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CosmicPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CosmicTertiary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                ),
                color = CosmicPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun CurationTab(
    articles: List<ArticleEntity>,
    onCleanupClick: () -> Unit,
    onThemeClick: (String) -> Unit
) {
    val themes = remember(articles) {
        val themeMap = mutableMapOf<String, Int>()
        articles.forEach { article ->
            if (article.aiThemes.isEmpty()) {
                themeMap["Non classé"] = (themeMap["Non classé"] ?: 0) + 1
            } else {
                article.aiThemes.forEach { theme ->
                    themeMap[theme] = (themeMap[theme] ?: 0) + 1
                }
            }
        }
        themeMap.toList().sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Curation par Thème",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = onCleanupClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CosmicSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoDelete,
                    contentDescription = "Cleanup",
                    tint = SystemRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (themes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun article à traiter", color = TextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(themes) { (theme, count) ->
                    ThemeTile(theme, count, onClick = { onThemeClick(theme) })
                }
            }
        }
    }
}

@Composable
fun ThemeTile(theme: String, count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$count article(s) en attente",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CosmicPrimary
            )
        }
    }
}

@Composable
fun ArticleCard(article: ArticleEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Theme tag
                val themeText = article.aiThemes.firstOrNull() ?: "Non classé"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CosmicSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = themeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextAccent
                    )
                }
                
                Text(
                    text = article.readingTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (article.aiInterest == true) CosmicTertiary else TextPrimary
            )
            
            if (article.aiExplanation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.aiExplanation ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = CosmicPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = article.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Icon
                    val (statusIcon, statusColor) = when {
                        article.isSentToNotebook -> Icons.Default.CheckCircle to SystemGreen
                        !article.isQualified -> Icons.Default.QuestionMark to TextSecondary
                        article.aiExplanation?.contains("Publicité", ignoreCase = true) == true -> Icons.Default.Block to SystemRed
                        article.aiExplanation?.contains("trop court", ignoreCase = true) == true -> Icons.Default.HourglassEmpty to CosmicTertiary
                        article.aiInterest == true -> Icons.Default.PriorityHigh to CosmicTertiary
                        else -> Icons.Default.Close to SystemRed
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CosmicSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "Status",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CosmicSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Open URL",
                            tint = CosmicTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CosmicSurfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Add to Notebook",
                            tint = CosmicPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(viewModel: HomeViewModel = viewModel()) {
    val gmailFilter by viewModel.gmailFilter.collectAsState()
    var geminiKey by remember { mutableStateOf("••••••••••••••••••••••••") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Paramètres de Veille",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Clé d'API Gemini") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPrimary,
                            unfocusedBorderColor = CosmicSurfaceVariant,
                            focusedLabelColor = CosmicPrimary,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                    )

                    OutlinedTextField(
                        value = gmailFilter,
                        onValueChange = { viewModel.updateGmailFilter(it) },
                        label = { Text("Filtre de recherche Gmail") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPrimary,
                            unfocusedBorderColor = CosmicSurfaceVariant,
                            focusedLabelColor = CosmicPrimary,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Button(
                        onClick = { viewModel.clearData() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remise à zéro de la base", color = Color.White)
                    }
                }
            }
        }

        item {
            Text(
                text = "Critères de Veille (criteria.md)",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "* Architecture Data, Data Engineering, Pipelines de données.\n* Intelligence Artificielle générative, Modèles de langage (LLM), Agents autonomes.\n* Nouveaux outils pour les développeurs, frameworks modernes.\n* RAG, Agent, Agentic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifier", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    OmnivigieTheme {
        HomeScreen()
    }
}
