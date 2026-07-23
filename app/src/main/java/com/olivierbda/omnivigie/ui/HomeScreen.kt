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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(viewModel: HomeViewModel = viewModel()) {
    val gmailFilter by viewModel.gmailFilter.collectAsState()
    val gmailFilterDate by viewModel.gmailFilterDate.collectAsState()
    val qualificationCriteria by viewModel.qualificationCriteria.collectAsState()
    val qualificationThemes by viewModel.qualificationThemes.collectAsState()
    val minReadingTime by viewModel.minReadingTime.collectAsState()

    var showCriteriaDialog by remember { mutableStateOf(false) }
    var showAddThemeDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }
    var newThemeInput by remember { mutableStateOf("") }
    var criteriaEditInput by remember { mutableStateOf("") }

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

        // 2. Pré-filtrage par Temps de Lecture
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
}

@Preview
@Composable
fun HomeScreenPreview() {
    OmnivigieTheme {
        HomeScreen()
    }
}
