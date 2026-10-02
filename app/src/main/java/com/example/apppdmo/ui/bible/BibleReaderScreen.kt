package com.example.apppdmo.ui.bible

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apppdmo.data.local.entity.BibleBookEntity
import com.example.apppdmo.data.local.entity.BibleVerseEntity
import com.example.apppdmo.data.repository.BibleRepository
import com.example.apppdmo.ui.components.ErrorView
import com.example.apppdmo.ui.components.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleReaderScreen(
    bibleRepository: BibleRepository,
    bookId: Int,
    chapter: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BibleReaderViewModel = viewModel(
        factory = BibleReaderViewModel.Factory(bibleRepository, bookId, chapter)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedVerseForActions by remember { mutableStateOf<BibleVerseEntity?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                    val title = when (val state = uiState) {
                        is BibleReaderUiState.Success -> "${state.book.name} ${state.chapter}"
                        else -> "Leitura"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is BibleReaderUiState.Loading -> LoadingView()
                is BibleReaderUiState.Error -> ErrorView(
                    message = state.message,
                    onRetry = { /* auto-reloaded by ViewModel */ }
                )
                is BibleReaderUiState.Success -> {
                    ReaderContent(
                        book = state.book,
                        chapter = state.chapter,
                        verses = state.verses,
                        favoriteVerseIds = state.favoriteVerseIds,
                        onPreviousChapter = { viewModel.goToPreviousChapter() },
                        onNextChapter = { viewModel.goToNextChapter(state.book.chapterCount) },
                        onVerseClick = { verse -> selectedVerseForActions = verse },
                        onToggleFavorite = { verseId -> viewModel.toggleFavorite(verseId) }
                    )
                }
            }
        }

        selectedVerseForActions?.let { verse ->
            val isFav = (uiState as? BibleReaderUiState.Success)?.favoriteVerseIds?.contains(verse.id) == true
            val bookName = (uiState as? BibleReaderUiState.Success)?.book?.name ?: ""
            val referenceText = "$bookName ${verse.chapter}:${verse.verse}"

            ModalBottomSheet(
                onDismissRequest = { selectedVerseForActions = null },
                sheetState = sheetState
            ) {
                VerseActionBottomSheetContent(
                    verseText = verse.text,
                    reference = referenceText,
                    isFavorite = isFav,
                    onToggleFavorite = {
                        viewModel.toggleFavorite(verse.id)
                        selectedVerseForActions = null
                    },
                    onCopy = {
                        copyVerseToClipboard(context, verse.text, referenceText)
                        selectedVerseForActions = null
                    },
                    onShare = {
                        shareVerse(context, verse.text, referenceText)
                        selectedVerseForActions = null
                    }
                )
            }
        }
    }
}

@Composable
private fun ReaderContent(
    book: BibleBookEntity,
    chapter: Int,
    verses: List<BibleVerseEntity>,
    favoriteVerseIds: Set<Int>,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onVerseClick: (BibleVerseEntity) -> Unit,
    onToggleFavorite: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPreviousChapter,
                    enabled = chapter > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Capítulo anterior")
                }

                TextButton(
                    onClick = onNextChapter,
                    enabled = chapter < book.chapterCount
                ) {
                    Text("Próximo capítulo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        if (verses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Nenhum versículo cadastrado para este capítulo ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(verses) { verse ->
            val isFavorite = favoriteVerseIds.contains(verse.id)
            VerseItemRow(
                verse = verse,
                isFavorite = isFavorite,
                onClick = { onVerseClick(verse) },
                onToggleFavorite = { onToggleFavorite(verse.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VerseItemRow(
    verse: BibleVerseEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isFavorite) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "${verse.verse}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = verse.text,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favoritar",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VerseActionBottomSheetContent(
    verseText: String,
    reference: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = reference,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"$verseText\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            OutlinedButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFavorite) "Remover" else "Favoritar")
            }

            OutlinedButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copiar")
            }

            OutlinedButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partilhar")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun copyVerseToClipboard(context: Context, text: String, reference: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Versículo da Bíblia", "\"$text\"\n$reference")
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Versículo copiado!", Toast.LENGTH_SHORT).show()
}

private fun shareVerse(context: Context, text: String, reference: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "\"$text\"\n$reference")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Partilhar Versículo")
    context.startActivity(shareIntent)
}
