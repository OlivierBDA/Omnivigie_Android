package com.olivierbda.omnivigie.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivierbda.omnivigie.ui.auth.NotebookAuthActivity
import com.olivierbda.omnivigie.ui.theme.*
import com.olivierbda.omnivigie.ui.viewmodel.HomeViewModel
import com.olivierbda.omnivigie.ui.viewmodel.NotebookSummary

@Composable
fun DashboardScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val unqualifiedCount by viewModel.unqualifiedCount.collectAsState()
    val pendingQualifiedCount by viewModel.pendingQualifiedCount.collectAsState()
    val notebooksCount by viewModel.notebooksCount.collectAsState()
    val recentNotebooks by viewModel.recentNotebooks.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val notebookStatus by viewModel.notebookStatus.collectAsState()
    val gcpStatus by viewModel.gcpStatus.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // 1. Header / Greeting card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(CosmicSurfaceVariant, Color(0xFF3B2D68))),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Bonjour, Dhi'San",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dernière synchro Gmail : $lastSyncTimestamp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. System Status Pills Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = "Gmail API",
                    status = "Connecté",
                    color = SystemGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.syncGmail(context as Activity) }
                )
                
                val gcpColor = if (gcpStatus.contains("OK") || gcpStatus == "Prêt") SystemGreen else SystemOrange
                StatusPill(
                    label = "Backend GCP",
                    status = gcpStatus,
                    color = gcpColor,
                    modifier = Modifier.weight(1.1f),
                    onClick = { viewModel.reauthGcp(context as Activity) }
                )
                
                val notebookColor = when {
                    notebookStatus.contains("Connecté") -> SystemGreen
                    notebookStatus.contains("renouveler") -> SystemOrange
                    else -> SystemRed
                }
                StatusPill(
                    label = "NotebookLM",
                    status = notebookStatus,
                    color = notebookColor,
                    modifier = Modifier.weight(1.1f),
                    onClick = { context.startActivity(Intent(context, NotebookAuthActivity::class.java)) }
                )
            }
        }

        // 3. Primary Action Button
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.syncAndProcessVeille(context as Activity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(GradientPurpleToIndigo),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sync & Traiter la veille",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                syncStatus?.let { status ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmicTertiary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 4. Statistics & KPIs Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    value = unqualifiedCount.toString(),
                    label = "Non qualifiés\n(LLM)",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    value = pendingQualifiedCount.toString(),
                    label = "Qualifiés prêts\n(Carnet)",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    value = notebooksCount.toString(),
                    label = "Carnets LM",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Recent Notebooks Section Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "DERNIERS NOTEBOOKS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextSecondary
                )
            )
        }

        // 6. Recent Notebooks Items
        if (recentNotebooks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2C224E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = TextAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aucun carnet créé pour l'instant",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Lancez la veille pour générer vos synthèses",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(recentNotebooks) { notebook ->
                NotebookItemRow(
                    notebook = notebook,
                    onClick = {
                        val intent = Intent(context, NotebookAuthActivity::class.java).apply {
                            notebook.notebookId?.let { putExtra("NOTEBOOK_ID", it) }
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "($status)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = color
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KpiCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NotebookItemRow(
    notebook: NotebookSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2C224E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = TextAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notebook.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val themesText = if (notebook.themes.isNotEmpty()) {
                    " • ${notebook.themes.joinToString(" & ")}"
                } else ""
                Text(
                    text = "${notebook.articleCount} articles$themesText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ouvrir",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
