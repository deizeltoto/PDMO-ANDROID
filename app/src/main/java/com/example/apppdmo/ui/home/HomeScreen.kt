package com.example.apppdmo.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apppdmo.data.local.entity.ContentEntity
import com.example.apppdmo.data.repository.CommunityRepository
import com.example.apppdmo.ui.components.ErrorView
import com.example.apppdmo.ui.components.LoadingView

@Composable
fun HomeScreen(
    repository: CommunityRepository,
    onNavigateToBible: () -> Unit,
    onNavigateToSongs: () -> Unit,
    onNavigateToContents: () -> Unit,
    onNavigateToContentDetail: (Long) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNotifyMessage: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                LoadingView()
            }
            is HomeUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.loadHomeData() }
                )
            }
            is HomeUiState.Success -> {
                HomeContent(
                    data = state.data,
                    onNavigateToBible = onNavigateToBible,
                    onNavigateToSongs = onNavigateToSongs,
                    onNavigateToContents = onNavigateToContents,
                    onNavigateToContentDetail = onNavigateToContentDetail,
                    onNavigateToAbout = onNavigateToAbout,
                    onShareMessage = { message, ref ->
                        shareDailyMessage(context, message, ref)
                    },
                    onNotifyMessage = onNotifyMessage
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    onNavigateToBible: () -> Unit,
    onNavigateToSongs: () -> Unit,
    onNavigateToContents: () -> Unit,
    onNavigateToContentDetail: (Long) -> Unit,
    onNavigateToAbout: () -> Unit,
    onShareMessage: (String, String) -> Unit,
    onNotifyMessage: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection(
                communityName = data.communityName,
                onNavigateToAbout = onNavigateToAbout
            )
        }

        item {
            data.dailyMessage?.let { daily ->
                DailyMessageSection(
                    message = daily.message,
                    reference = daily.bibleReference,
                    onShare = { onShareMessage(daily.message, daily.bibleReference) },
                    onNotify = { onNotifyMessage(daily.message, daily.bibleReference) }
                )
            }
        }

        item {
            QuickAccessSection(
                onNavigateToBible = onNavigateToBible,
                onNavigateToSongs = onNavigateToSongs,
                onNavigateToContents = onNavigateToContents
            )
        }

        item {
            RecentContentsHeader(onSeeAllClick = onNavigateToContents)
        }

        items(data.recentContents) { content ->
            ContentItemCard(
                content = content,
                onClick = { onNavigateToContentDetail(content.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderSection(
    communityName: String,
    onNavigateToAbout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = communityName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Paz seja convosco! Seja bem-vindo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNavigateToAbout) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Sobre a Comunidade",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun DailyMessageSection(
    message: String,
    reference: String,
    onShare: () -> Unit,
    onNotify: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Mensagem do dia",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row {
                    IconButton(onClick = onNotify) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificar Mensagem do Dia",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Partilhar mensagem",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"$message\"",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reference,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun QuickAccessSection(
    onNavigateToBible: () -> Unit,
    onNavigateToSongs: () -> Unit,
    onNavigateToContents: () -> Unit
) {
    Column {
        Text(
            text = "Acessos Rápidos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessCard(
                title = "Bíblia",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = onNavigateToBible,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                title = "Cânticos",
                icon = Icons.Default.MusicNote,
                onClick = onNavigateToSongs,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAccessCard(
                title = "Estudos",
                icon = Icons.Default.Book,
                onClick = onNavigateToContents,
                modifier = Modifier.weight(1f)
            )
            QuickAccessCard(
                title = "Pregações",
                icon = Icons.Default.Mic,
                onClick = onNavigateToContents,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentContentsHeader(onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Conteúdos recentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onSeeAllClick) {
            Text(text = "Ver todos")
        }
    }
}

@Composable
private fun ContentItemCard(
    content: ContentEntity,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (content.type.uppercase()) {
                        "STUDY", "ESTUDO" -> Icons.Default.Book
                        "SERMON", "PREGAÇÃO" -> Icons.Default.Mic
                        else -> Icons.AutoMirrored.Filled.Article
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = content.type,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    val label = when (content.type.uppercase()) {
                        "STUDY", "ESTUDO" -> "Estudo"
                        "SERMON", "PREGAÇÃO" -> "Pregação"
                        else -> "Artigo"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = content.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Por ${content.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun shareDailyMessage(context: Context, message: String, reference: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "\"$message\"\n- $reference")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Partilhar Mensagem do Dia")
    context.startActivity(shareIntent)
}
