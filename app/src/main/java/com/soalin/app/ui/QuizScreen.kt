package com.soalin.app.ui

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.soalin.app.BellPlayer
import com.soalin.app.Guard
import com.soalin.app.GuardService
import com.soalin.app.Sfx
import com.soalin.app.data.SessionQuestion
import com.soalin.app.findActivity
import kotlinx.coroutines.delay

private enum class OptState { Idle, Selected, Correct, Wrong, Dim }

@Composable
fun QuizScreen(
    title: String,
    session: List<SessionQuestion>,
    onBegin: () -> Unit,
    onProgress: (correct: Int, wrong: Int) -> Unit,
    onFinish: (List<Int>) -> Unit,
    onExit: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableIntStateOf(-1) }
    var locked by remember { mutableStateOf(false) }
    val answers = remember { mutableStateListOf<Int>() }
    var confirmExit by remember { mutableStateOf(false) }

    BackHandler { confirmExit = true }

    val ctx = LocalContext.current
    val activity = remember { ctx.findActivity() }

    // Selesai / keluar lewat tombol aplikasi: lepas penyematan lalu bel 5 detik
    fun leave(next: () -> Unit) {
        Guard.quizActive = false
        GuardService.stop(ctx)
        activity?.let { Guard.unpin(it) }
        BellPlayer.ring(ctx)
        next()
    }

    DisposableEffect(Unit) {
        Guard.quizActive = true
        Guard.everPinned = false
        onDispose { Guard.quizActive = false }
    }

    // Mulai service penjaga dan minta penyematan layar.
    // Kalau penyematan ditolak, sesi dihentikan dan aplikasi ditutup otomatis.
    LaunchedEffect(Unit) {
        Sfx.load(ctx)
        GuardService.start(ctx)
        val act = activity
        if (act != null && !Guard.isPinned(act)) {
            Guard.pin(act)
            if (!Guard.awaitPinned(act)) {
                Guard.quizActive = false
                Guard.everPinned = false
                GuardService.stop(ctx)
                Toast.makeText(
                    ctx.applicationContext,
                    "Sesi dihentikan: penyematan layar harus diizinkan (Pengaturan > Keamanan > Penyematan aplikasi).",
                    Toast.LENGTH_LONG
                ).show()
                act.finishAndRemoveTask()
                return@LaunchedEffect
            }
        }
        onBegin() // catat waktu dibuka (log di tombol soal)
    }

    val q = session[index]
    val answered = locked
    val total = session.size
    val progress = (index + (if (answered) 1 else 0)) / total.toFloat()

    Column(Modifier.fillMaxSize().background(C.Bg).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) {
                Icon(Icons.Default.Close, contentDescription = "Keluar", tint = C.Dim)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = C.Dim, fontSize = 12.sp, maxLines = 1)
                Text("Soal ${index + 1} dari $total", color = C.Text, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = C.Accent,
            trackColor = C.SurfaceHi
        )
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(C.Surface).padding(20.dp)
            ) {
                Text(q.text, color = C.Text, fontSize = 19.sp, fontWeight = FontWeight.Medium, lineHeight = 27.sp)
            }
            Spacer(Modifier.height(16.dp))

            q.options.forEachIndexed { i, text ->
                val st = when {
                    !answered -> if (i == chosen) OptState.Selected else OptState.Idle
                    i == q.correctIndex -> OptState.Correct
                    i == chosen -> OptState.Wrong
                    else -> OptState.Dim
                }
                OptionItem(label = 'A' + i, text = text, state = st, enabled = !answered) { chosen = i }
                Spacer(Modifier.height(10.dp))
            }

            if (answered) {
                val ok = chosen == q.correctIndex
                Spacer(Modifier.height(4.dp))
                Text(
                    if (ok) "Benar!" else "Kurang tepat. Jawaban benar: ${'A' + q.correctIndex}. ${q.options[q.correctIndex]}",
                    color = if (ok) C.Good else C.Bad,
                    fontWeight = FontWeight.SemiBold
                )
                q.explanation?.let {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.SurfaceHi).padding(14.dp)
                    ) { Text("Pembahasan: $it", color = C.Dim, fontSize = 14.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (!locked) {
                    locked = true // kunci jawaban setelah tombol "Jawab" ditekan
                    val ok = chosen == q.correctIndex
                    Sfx.play(ok) // suara benar / salah
                    // Simpan hasil sampai soal ini; tetap ada walau sesi ditinggalkan
                    val correctNow = answers.indices.count { answers[it] == session[it].correctIndex } + if (ok) 1 else 0
                    onProgress(correctNow, answers.size + 1 - correctNow)
                } else {
                    answers.add(chosen)
                    if (index == total - 1) {
                        leave { onFinish(answers.toList()) }
                    } else {
                        index++
                        chosen = -1
                        locked = false
                    }
                }
            },
            enabled = chosen >= 0,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = C.Primary,
                contentColor = C.Bg,
                disabledContainerColor = C.SurfaceHi,
                disabledContentColor = C.Dim
            )
        ) {
            Text(
                when {
                    !locked -> "Jawab"
                    index == total - 1 -> "Selesai"
                    else -> "Lanjut"
                },
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            containerColor = C.Surface,
            title = { Text("Keluar dari sesi?", color = C.Text) },
            text = { Text("Jawaban yang sudah kamu kerjakan tetap tersimpan di riwayat soal.", color = C.Dim) },
            confirmButton = { TextButton(onClick = { leave(onExit) }) { Text("Keluar", color = C.Bad) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Lanjut belajar", color = C.Primary) } }
        )
    }
}

@Composable
private fun OptionItem(label: Char, text: String, state: OptState, enabled: Boolean, onClick: () -> Unit) {
    val (bg, borderColor, badge) = when (state) {
        OptState.Idle -> Triple(C.Surface, C.SurfaceHi, C.Primary)
        OptState.Selected -> Triple(C.Primary.copy(alpha = 0.15f), C.Primary, C.Primary)
        OptState.Correct -> Triple(C.Good.copy(alpha = 0.15f), C.Good, C.Good)
        OptState.Wrong -> Triple(C.Bad.copy(alpha = 0.15f), C.Bad, C.Bad)
        OptState.Dim -> Triple(C.Surface, C.SurfaceHi, C.Dim)
    }
    val alpha = if (state == OptState.Dim) 0.55f else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(badge.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text(label.toString(), color = badge, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Text(text, color = C.Text.copy(alpha = alpha), fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}
