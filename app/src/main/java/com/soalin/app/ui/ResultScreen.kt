package com.soalin.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soalin.app.data.SessionQuestion

@Composable
fun ResultScreen(
    title: String,
    session: List<SessionQuestion>,
    answers: List<Int>,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    val correct = answers.indices.count { answers[it] == session[it].correctIndex }
    val total = session.size
    val percent = correct * 100 / total
    val wrong = session.indices.filter { answers[it] != session[it].correctIndex }
    val message = when {
        percent == 100 -> "Sempurna! Luar biasa!"
        percent >= 80 -> "Keren, hampir sempurna!"
        percent >= 60 -> "Bagus, terus latihan ya!"
        else -> "Ayo coba lagi, kamu pasti bisa!"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.Bg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(title, color = C.Dim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.size(170.dp),
                    strokeWidth = 12.dp,
                    color = if (percent >= 60) C.Good else C.Accent,
                    trackColor = C.SurfaceHi
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$percent", color = C.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    Text("dari 100", color = C.Dim, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(message, color = C.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Benar $correct  •  Salah ${total - correct}  •  Total $total", color = C.Dim)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = C.Primary, contentColor = C.Bg)
            ) { Text("Ulangi (diacak lagi)", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Kembali ke Beranda", color = C.Primary) }
            Spacer(Modifier.height(12.dp))
            Text(
                if (wrong.isEmpty()) "Semua jawabanmu benar!" else "Soal yang masih salah (${wrong.size})",
                color = C.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
        }

        itemsIndexed(wrong) { _, i ->
            val q = session[i]
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Surface).padding(16.dp)
            ) {
                Text("${i + 1}. ${q.text}", color = C.Text, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Jawabanmu: ${q.options[answers[i]]}", color = C.Bad, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("Jawaban benar: ${q.options[q.correctIndex]}", color = C.Good, fontSize = 14.sp)
                q.explanation?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Pembahasan: $it", color = C.Dim, fontSize = 13.sp)
                }
            }
        }
    }
}
