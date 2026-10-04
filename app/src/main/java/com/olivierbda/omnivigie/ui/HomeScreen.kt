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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation


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
import android.net.Uri
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.data.repository.AiCoreStatus
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
    val isProcessingOverlayVisible by viewModel.isProcessingOverlayVisible.collectAsState()
    val processingTitle by viewModel.processingTitle.collectAsState()
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Curation") },
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
                    0 -> DashboardScreen(viewModel = viewModel)
                    1 -> CurationTab(
                        articles = unsentArticles,
                        onCleanupClick = { viewModel.cleanupArticles() },
                        onThemeClick = { theme -> selectedThemeForDetail = theme }
                    )
                    2 -> SettingsTab(viewModel)
                }
            }
        }

        AnimatedVisibility(
            visible = isProcessingOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ProcessingVideoScreen(
                title = processingTitle,
                status = syncStatus,
                onDismiss = {
                    viewModel.hideProcessingOverlay()
                }
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
    var showCleanupConfirmDialog by remember { mutableStateOf(false) }

    val themes = remember(articles) {
        val themeMap = mutableMapOf<String, Int>()
        articles.forEach { article ->
            if (!article.isQualified || article.aiThemes.isEmpty()) {
                themeMap["Non classé"] = (themeMap["Non classé"] ?: 0) + 1
            } else if (article.aiThemes.contains("Exclus") || article.aiInterest == false) {
                themeMap["Exclus"] = (themeMap["Exclus"] ?: 0) + 1
            } else {
                article.aiThemes.forEach { theme ->
                    themeMap[theme] = (themeMap[theme] ?: 0) + 1
                }
            }
        }
        themeMap.toList().sortedWith(Comparator { a, b ->
            when {
                a.first == "Non classé" -> -1
                b.first == "Non classé" -> 1
                a.first == "Exclus" -> 1
                b.first == "Exclus" -> -1
                else -> b.second.compareTo(a.second)
            }
        })
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
                onClick = { showCleanupConfirmDialog = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CosmicSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoDelete,
                    contentDescription = "Cleanup Exclus",
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

    if (showCleanupConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirmDialog = false },
            containerColor = CosmicSurface,
            title = { Text("Purger les articles exclus ?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Voulez-vous supprimer définitivement tous les articles classés dans la catégorie 'Exclus' (sponsors, articles trop courts ou rejetés par le LLM) ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanupConfirmDialog = false
                        onCleanupClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemRed)
                ) {
                    Text("Purger les exclus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupConfirmDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
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
                    text = article.aiExplanation,
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
                        article.aiExplanation?.contains("trop ancien", ignoreCase = true) == true -> Icons.Default.Schedule to CosmicTertiary
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
                            imageVector = Icons.AutoMirrored.Filled.Launch,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(viewModel: HomeViewModel = viewModel()) {
    val gmailFilter by viewModel.gmailFilter.collectAsState()
    val gmailFilterDate by viewModel.gmailFilterDate.collectAsState()
    val qualificationCriteria by viewModel.qualificationCriteria.collectAsState()
    val qualificationThemes by viewModel.qualificationThemes.collectAsState()
    val minReadingTime by viewModel.minReadingTime.collectAsState()
    val articleRetentionDays by viewModel.articleRetentionDays.collectAsState()
    val classificationEngine by viewModel.classificationEngine.collectAsState()
    val aiCoreStatus by viewModel.aiCoreStatus.collectAsState()
    val isTestingAiCore by viewModel.isTestingAiCore.collectAsState()
    val aiCoreTestResult by viewModel.aiCoreTestResult.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val geminiModel by viewModel.geminiModel.collectAsState()
    val isTestingLlm by viewModel.isTestingLlm.collectAsState()
    val llmTestResult by viewModel.llmTestResult.collectAsState()

    var showCriteriaDialog by remember { mutableStateOf(false) }
    var showAddThemeDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showCustomRetentionDialog by remember { mutableStateOf(false) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }
    var newThemeInput by remember { mutableStateOf("") }
    var criteriaEditInput by remember { mutableStateOf("") }
    var customRetentionInput by remember(articleRetentionDays) { mutableStateOf(articleRetentionDays.toString()) }
    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var modelInput by remember(geminiModel) { mutableStateOf(geminiModel) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current



    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // 1. Paramètres d'Acquisition Gmail & Purge
        item {
            Text(
                text = "Paramètres de Veille & Filtre Gmail",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Date de début de récupération des emails (Gmail) :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedButton(
                        onClick = { showDatePickerDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary,
                            containerColor = CosmicSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, CosmicPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = CosmicTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Date : $gmailFilterDate",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "• from: dan@tldrnewsletter.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "• from: tldr@tldrnewsletter.com",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Rétention des Articles (Exclusion automatique)
        item {
            Text(
                text = "Rétention des Articles (Exclusion)",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Durée de rétention des articles :",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "$articleRetentionDays jours",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = CosmicPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(7, 14, 30, 60)
                        options.forEach { days ->
                            val isSelected = articleRetentionDays == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateArticleRetentionDays(days) },
                                label = {
                                    Text(
                                        text = "$days j",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CosmicPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = CosmicSurfaceVariant,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                customRetentionInput = articleRetentionDays.toString()
                                showCustomRetentionDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = CosmicTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Durée personnalisée...",
                                color = CosmicTertiary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Text(
                        text = "Les articles de plus de $articleRetentionDays jours seront automatiquement reclassés dans la catégorie « Exclus » lors du lancement de « Sync & Traiter la veille ».",
                        style = MaterialTheme.typography.labelSmall,
                        color = CosmicTertiary
                    )
                }
            }
        }

        // 3. Moteur de Classification (Local TPU vs Cloud Gemini API)
        item {
            Text(
                text = "Moteur de Classification des Articles",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sélectionnez le moteur d'IA pour qualifier vos articles de veille :",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = classificationEngine == "aicore",
                            onClick = { viewModel.updateClassificationEngine("aicore") },
                            label = {
                                Text(
                                    text = "⚡ TPU Pixel 10 (Local)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (classificationEngine == "aicore") FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CosmicPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = CosmicSurfaceVariant,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = classificationEngine == "gemini_cloud",
                            onClick = { viewModel.updateClassificationEngine("gemini_cloud") },
                            label = {
                                Text(
                                    text = "☁️ API Cloud (Distant)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (classificationEngine == "gemini_cloud") FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CosmicPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = CosmicSurfaceVariant,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Carte d'état Android AICore
                    Surface(
                        color = CosmicSurfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Android AICore (Google Tensor)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )

                                val (statusText, statusColor) = when (aiCoreStatus) {
                                    is AiCoreStatus.Available -> "Disponible" to SystemGreen
                                    is AiCoreStatus.Downloadable -> "À télécharger" to SystemOrange
                                    is AiCoreStatus.Downloading -> "Téléchargement" to SystemOrange
                                    is AiCoreStatus.Unavailable -> "Indisponible" to SystemRed
                                    is AiCoreStatus.Error -> "Erreur" to SystemRed
                                }

                                Surface(
                                    color = statusColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, statusColor)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Modèle : Gemini Nano 4 Fast (E2B / Faible latence)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )

                            // Bouton tester TPU
                            Button(
                                onClick = { viewModel.testAiCoreConnection() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CosmicBackground),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isTestingAiCore
                            ) {
                                if (isTestingAiCore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = CosmicTertiary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Test TPU en cours...", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = CosmicTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tester Gemini Nano (TPU)", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            aiCoreTestResult?.let { result ->
                                val isSuccess = result.startsWith("Succès")
                                Surface(
                                    color = if (isSuccess) SystemGreen.copy(alpha = 0.15f) else SystemRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, if (isSuccess) SystemGreen else SystemRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = result,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSuccess) SystemGreen else SystemRed,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { viewModel.clearAiCoreTestResult() },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Fermer",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "En mode local, si le TPU est indisponible, l'application bascule automatiquement sur l'API Gemini Cloud.",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmicTertiary
                            )
                        }
                    }
                }
            }
        }

        // 4. Configuration du Modèle LLM Cloud (Google Gemini)
        item {
            Text(
                text = "Configuration API Cloud (Gemini)",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Paramètres de connexion à l'API Gemini pour la suggestion de fiches NotebookLM et le fallback TPU :",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Nom du modèle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Modèle LLM :",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = modelInput,
                            onValueChange = { 
                                modelInput = it
                                viewModel.updateGeminiModel(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("ex: gemini-2.0-flash-lite") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://ai.google.dev/gemini-api/docs/models?hl=fr")
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Launch,
                                        contentDescription = "Voir la documentation des modèles Gemini",
                                        tint = CosmicTertiary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicPrimary,
                                unfocusedBorderColor = CosmicSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }


                    // Clé API Gemini
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Clé API Gemini :",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { 
                                apiKeyInput = it
                                viewModel.updateGeminiApiKey(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Clé API Google AI Studio") },
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Afficher/Masquer la clé",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CosmicPrimary,
                                unfocusedBorderColor = CosmicSurfaceVariant,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Bouton Tester
                    Button(
                        onClick = { viewModel.testLlmConnection(apiKeyInput, modelInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isTestingLlm
                    ) {
                        if (isTestingLlm) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = CosmicTertiary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test en cours...", color = TextPrimary)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CosmicTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tester la connexion LLM", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Résultat du test
                    llmTestResult?.let { result ->
                        val isSuccess = result.startsWith("Succès")
                        Surface(
                            color = if (isSuccess) SystemGreen.copy(alpha = 0.15f) else SystemRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSuccess) SystemGreen else SystemRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isSuccess) SystemGreen else SystemRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) SystemGreen else SystemRed,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearLlmTestResult() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Pré-filtrage par Temps de Lecture
        item {
            Text(
                text = "Pré-filtrage Articles",
                style = MaterialTheme.typography.titleMedium,
                color = TextAccent,
                fontWeight = FontWeight.Bold
            )
        }


        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Temps de lecture minimum requis pour l'analyse :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(0, 3, 5, 10)
                        options.forEach { minutes ->
                            val isSelected = minReadingTime == minutes
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.updateMinReadingTime(minutes) },
                                label = {
                                    Text(
                                        text = if (minutes == 0) "0 min (Tout)" else "$minutes min",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CosmicPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = CosmicSurfaceVariant,
                                    labelColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Critères de Veille (criteria.md)
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
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = qualificationCriteria,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            criteriaEditInput = qualificationCriteria
                            showCriteriaDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Criteria",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifier les critères", color = TextPrimary)
                    }
                }
            }
        }

        // 4. Thèmes & Catégories (themes.json)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thèmes & Catégories (${qualificationThemes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextAccent,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        newThemeInput = ""
                        showAddThemeDialog = true
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CosmicPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter un thème",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        qualificationThemes.forEach { theme ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(theme, color = TextPrimary, style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove theme",
                                        tint = SystemRed,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.removeQualificationTheme(theme) }
                                    )
                                },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = CosmicSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Zone de Danger (Purge BDD) tout en bas
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showClearDataConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SystemRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remise à zéro de la base de données", color = Color.White)
            }
        }
    }

    // Dialog 1: DatePicker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { millis ->
                            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = millis
                            }
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            sdf.timeZone = TimeZone.getTimeZone("UTC")
                            val formattedDate = sdf.format(calendar.time)
                            viewModel.updateGmailFilterDate(formattedDate)
                        }
                    }
                ) {
                    Text("OK", color = CosmicPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = CosmicSurface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CosmicSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = CosmicPrimary,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = CosmicPrimary,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = CosmicPrimary,
                    todayContentColor = CosmicPrimary,
                    todayDateBorderColor = CosmicPrimary
                )
            )
        }
    }

    // Dialog 2: Edit Criteria Dialog
    if (showCriteriaDialog) {
        AlertDialog(
            onDismissRequest = { showCriteriaDialog = false },
            containerColor = CosmicSurface,
            title = { Text("Édition des Critères de Veille", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Rédigez les consignes transmises à Gemini pour évaluer les articles :",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = criteriaEditInput,
                        onValueChange = { criteriaEditInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPrimary,
                            unfocusedBorderColor = CosmicSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateQualificationCriteria(criteriaEditInput)
                        showCriteriaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCriteriaDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }

    // Dialog 3: Add Theme Dialog
    if (showAddThemeDialog) {
        AlertDialog(
            onDismissRequest = { showAddThemeDialog = false },
            containerColor = CosmicSurface,
            title = { Text("Nouveau Thème / Catégorie", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newThemeInput,
                    onValueChange = { newThemeInput = it },
                    label = { Text("Nom du thème") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CosmicPrimary,
                        unfocusedBorderColor = CosmicSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newThemeInput.isNotBlank()) {
                            viewModel.addQualificationTheme(newThemeInput)
                            showAddThemeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddThemeDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }

    // Dialog 4: Clear Data Confirmation Dialog
    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            containerColor = CosmicSurface,
            title = { Text("Réinitialiser la base ?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Êtes-vous sûr de vouloir supprimer tous les articles et emails stockés en local ? Cette action est irréversible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDataConfirmDialog = false
                        viewModel.clearData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemRed)
                ) {
                    Text("Confirmer la suppression", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirmDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }

    // Dialog 5: Custom Retention Days Dialog
    if (showCustomRetentionDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRetentionDialog = false },
            containerColor = CosmicSurface,
            title = {
                Text(
                    text = "Durée de rétention des articles",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Indiquez le nombre de jours au-delà duquel un article est considéré trop ancien :",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = customRetentionInput,
                        onValueChange = { input ->
                            customRetentionInput = input.filter { it.isDigit() }
                        },
                        label = { Text("Nombre de jours") },
                        singleLine = true,
                        placeholder = { Text("30") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicPrimary,
                            unfocusedBorderColor = CosmicSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = customRetentionInput.toIntOrNull() ?: 30
                        if (days > 0) {
                            viewModel.updateArticleRetentionDays(days)
                        }
                        showCustomRetentionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRetentionDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    OmnivigieTheme {
        HomeScreen()
    }
}
