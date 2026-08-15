package com.olivierbda.omnivigie.ui

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

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivierbda.omnivigie.data.local.entities.ArticleEntity
import com.olivierbda.omnivigie.domain.usecase.RecommendationResult
import com.olivierbda.omnivigie.ui.theme.*

import com.olivierbda.omnivigie.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurationDetailScreen(
    theme: String,
    articles: List<ArticleEntity>,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val selectedIds by viewModel.selectedArticles.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val recommendationState by viewModel.recommendationState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val currentThemeArticles = remember(articles, theme) {
        when (theme) {
            "Non classé" -> articles.filter { !it.isQualified || it.aiThemes.isEmpty() }
            "Exclus" -> articles.filter { it.aiThemes.contains("Exclus") || (it.isQualified && it.aiInterest == false) }
            else -> articles.filter { it.aiThemes.contains(theme) }
        }
    }

    // Auto-select all when entering if none selected
    LaunchedEffect(currentThemeArticles) {
        if (selectedIds.isEmpty()) {
            viewModel.selectAll(currentThemeArticles.map { it.id })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(theme, color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },

                actions = {
                    Text(
                        text = "${selectedIds.intersect(currentThemeArticles.map { it.id }.toSet()).size}/${currentThemeArticles.size}",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextAccent
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicBackground)
            )
        },
        bottomBar = {
            if (currentThemeArticles.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CosmicSurface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Button 1: Suggestion d'articles (Gemini Stars icon)
                        OutlinedButton(
                            onClick = { viewModel.recommendArticlesForTheme(theme, currentThemeArticles) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = CosmicPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CosmicPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CosmicPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Suggestion\nd'articles",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            )
                        }

                        // Button 2: Création du Notebook (NotebookLM / MenuBook icon)
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedIds.intersect(currentThemeArticles.map { it.id }.toSet()).isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Création du\nNotebook",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = CosmicBackground
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentThemeArticles) { article ->
                    SelectableArticleCard(
                        article = article,
                        isSelected = selectedIds.contains(article.id),
                        onToggle = { viewModel.toggleArticleSelection(article.id) },
                        onDelete = { viewModel.deleteArticle(article) }
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val notebookName = "[AI] $date TLDR-$theme"
        val count = selectedIds.intersect(currentThemeArticles.map { it.id }.toSet()).size

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = CosmicSurface,
            title = { Text("Confirmer la création", color = TextPrimary) },
            text = {
                Column {
                    Text("Nom du Notebook :", style = MaterialTheme.typography.labelMedium, color = TextAccent)
                    Text(notebookName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nombre d'articles : $count", color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showConfirmDialog = false
                        isProcessing = true
                        val articlesToCreate = selectedIds.intersect(currentThemeArticles.map { it.id }.toSet()).toList()
                        viewModel.createNotebook(context as android.app.Activity, theme, articlesToCreate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }

    if (isProcessing) {
        val isDone = syncStatus?.startsWith("Terminé", ignoreCase = true) == true
        ProcessingVideoScreen(
            title = "Création Carnet Gemini Notebook",
            status = syncStatus,
            onDismiss = {
                isProcessing = false
                viewModel.hideProcessingOverlay()
                if (isDone) onBack()
            }
        )
    }


    when (val state = recommendationState) {
        is RecommendationResult.Loading -> {
            AlertDialog(
                onDismissRequest = { },
                containerColor = CosmicSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CosmicPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Suggestion par IA", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CosmicPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyse de ${currentThemeArticles.size} articles par Gemini...",
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = { }
            )
        }
        is RecommendationResult.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetRecommendationState() },
                containerColor = CosmicSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CosmicPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Suggestion d'articles par l'IA", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Surface(
                            color = CosmicBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "${state.recommendation.recommendedArticleIds.size} articles sélectionnés sur ${currentThemeArticles.size}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = TextAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Pourquoi ces articles :",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.recommendation.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.applyRecommendation(state.recommendation.recommendedArticleIds, currentThemeArticles)
                            viewModel.resetRecommendationState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                    ) {
                        Text("Accepter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.resetRecommendationState() }) {
                        Text("Annuler", color = TextSecondary)
                    }
                }
            )
        }
        is RecommendationResult.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetRecommendationState() },
                containerColor = CosmicSurface,
                title = { Text("Erreur de recommandation", color = SystemRed) },
                text = { Text(state.message, color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetRecommendationState() },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary)
                    ) {
                        Text("Fermer")
                    }
                }
            )
        }
        else -> {}
    }
}

@Composable
fun SelectableArticleCard(
    article: ArticleEntity,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .border(
                1.dp,
                if (isSelected) CosmicPrimary else Color.Transparent,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = CosmicPrimary)
                )
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = SystemRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                if (article.aiExplanation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.aiExplanation,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = CosmicTertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = article.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextAccent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = article.readingTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
