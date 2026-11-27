package com.soalin.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soalin.app.R
import com.soalin.app.data.Downloader
import com.soalin.app.data.QuizParseException
import com.soalin.app.data.QuizParser
import com.soalin.app.data.QuizSet
import com.soalin.app.data.Repository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    repo: Repository,
    sets: List<QuizSet>,
    onChanged: () -> Unit,
    onStart: (QuizSet) -> Unit
) {
    var showLoad by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<QuizSet?>(null) }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = "Logo Soalin",
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Soalin", color = C.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("Belajar offline, soal diacak tiap sesi", color = C.Dim, fontSize = 13.sp)
                }
            }

            if (sets.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Belum ada soal", color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tekan tombol \"Muat Soal\" lalu tempel link file soal\n(Google Drive, GitHub, dll) atau pilih file dari HP.",
                        color = C.Dim,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sets, key = { it.id }) { set ->
                        SetCard(
                            set = set,
                            best = repo.bestScore(set.id),
                            onClick = { onStart(set) },
                            onDelete = { toDelete = set }
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showLoad = true },
            containerColor = C.Primary,
            contentColor = C.Bg,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Muat Soal", fontWeight = FontWeight.SemiBold) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        )
    }

    if (showLoad) {
        LoadDialog(
            repo = repo,
            onDismiss = { showLoad = false },
            onLoaded = {
                showLoad = false
                onChanged()
            }
        )
    }

    toDelete?.let { set ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = C.Surface,
            title = { Text("Hapus paket soal?", color = C.Text) },
            text = { Text("\"${set.title}\" akan dihapus dari HP ini.", color = C.Dim) },
            confirmButton = {
                TextButton(onClick = {
                    repo.delete(set.id)
                    toDelete = null
                    onChanged()
                }) { Text("Hapus", color = C.Bad) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Batal", color = C.Dim) } }
        )
    }
}

@Composable
private fun SetCard(set: QuizSet, best: Int, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.Surface)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(set.title, color = C.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (set.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(set.description, color = C.Dim, fontSize = 13.sp, maxLines = 2)
            }
            Spacer(Modifier.height(8.dp))
            val info = buildString {
                append("${set.questions.size} soal")
                if (best >= 0) append("  •  Skor terbaik $best%")
            }
            Text(info, color = C.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = C.Dim)
        }
    }
}

@Composable
private fun LoadDialog(repo: Repository, onDismiss: () -> Unit, onLoaded: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun finish(set: QuizSet) {
        repo.save(set)
        Toast.makeText(ctx, "Berhasil: ${set.title} (${set.questions.size} soal)", Toast.LENGTH_LONG).show()
        onLoaded()
    }

    fun loadFromLink() {
        if (url.isBlank()) {
            error = "Tempel link-nya dulu."
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val raw = Downloader.fetch(url)
                finish(QuizParser.parse(raw, url.trim()))
            } catch (e: QuizParseException) {
                error = e.message
            } catch (e: Exception) {
                error = e.message ?: "Gagal memuat. Cek koneksi internet dan link-nya."
            } finally {
                loading = false
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            loading = true
            error = null
            scope.launch {
                try {
                    val raw = ctx.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw QuizParseException("File tidak bisa dibaca.")
                    finish(QuizParser.parse(raw, null))
                } catch (e: QuizParseException) {
                    error = e.message
                } catch (e: Exception) {
                    error = e.message ?: "File tidak bisa dibaca."
                } finally {
                    loading = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = C.Surface,
        title = { Text("Muat Soal", color = C.Text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Tempel link file soal (.json) dari Google Drive, GitHub, Dropbox, atau link langsung.",
                    color = C.Dim,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Link soal") },
                    singleLine = false,
                    maxLines = 3,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = C.Bad, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Atau pilih file dari HP", color = C.Primary) }
                if (loading) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = C.Accent)
                        Spacer(Modifier.width(10.dp))
                        Text("Memuat soal...", color = C.Dim, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { loadFromLink() }, enabled = !loading) {
                Text("Muat", color = if (loading) C.Dim else C.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Batal", color = C.Dim) }
        }
    )
}
