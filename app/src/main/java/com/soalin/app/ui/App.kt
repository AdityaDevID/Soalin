package com.soalin.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.soalin.app.data.QuizSet
import com.soalin.app.data.Repository
import com.soalin.app.data.SessionQuestion
import com.soalin.app.data.newSession

sealed interface Screen {
    data object Home : Screen
    data class Quiz(
        val set: QuizSet,
        val session: List<SessionQuestion>,
        val startedAt: Long = System.currentTimeMillis()
    ) : Screen
    data class Result(
        val set: QuizSet,
        val session: List<SessionQuestion>,
        val answers: List<Int>
    ) : Screen
}

@Composable
fun SoalinApp() {
    val ctx = LocalContext.current
    val repo = remember { Repository(ctx) }
    var sets by remember { mutableStateOf(repo.loadAll()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BackHandler(enabled = screen is Screen.Result) { screen = Screen.Home }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            repo = repo,
            sets = sets,
            onChanged = { sets = repo.loadAll() },
            onStart = { set ->
                if (set.questions.isEmpty()) {
                    Toast.makeText(ctx, "Paket soal kosong", Toast.LENGTH_SHORT).show()
                } else {
                    screen = Screen.Quiz(set, set.newSession()) // acak baru setiap sesi
                }
            }
        )

        is Screen.Quiz -> QuizScreen(
            title = s.set.title,
            session = s.session,
            onBegin = { repo.saveLog(s.set.id, s.startedAt, 0, 0) },
            onProgress = { correct, wrong -> repo.saveLog(s.set.id, s.startedAt, correct, wrong) },
            onFinish = { answers ->
                val correct = answers.indices.count { answers[it] == s.session[it].correctIndex }
                repo.saveBestScore(s.set.id, correct * 100 / s.session.size)
                sets = repo.loadAll()
                screen = Screen.Result(s.set, s.session, answers)
            },
            onExit = { screen = Screen.Home }
        )

        is Screen.Result -> ResultScreen(
            title = s.set.title,
            session = s.session,
            answers = s.answers,
            onRetry = { screen = Screen.Quiz(s.set, s.set.newSession()) },
            onHome = { screen = Screen.Home }
        )
    }
}
