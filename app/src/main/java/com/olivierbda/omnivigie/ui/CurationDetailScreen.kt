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
import com.olivierbda.omnivigie.ui.theme.*
import com.olivierbda.omnivigie.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurationDetailScreen(
    theme: String,
    articles: List<ArticleEntity>,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val selectedIds by viewModel.selectedArticles.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    val currentThemeArticles = remember(articles, theme) {
        if (theme == "Non classé") {
            articles.filter { it.aiThemes.isEmpty() }
        } else {
            articles.filter { it.aiThemes.contains(theme) }
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPrimary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedIds.intersect(currentThemeArticles.map { it.id }.toSet()).isNotEmpty()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Création du Notebook", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        viewModel.createNotebook(theme)
                        onBack()
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = CosmicPrimary)
            )
            
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
                        text = article.aiExplanation ?: "",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(article.source, style = MaterialTheme.typography.labelSmall, color = TextAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(article.readingTime, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SystemRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}
