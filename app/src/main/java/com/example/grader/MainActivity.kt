package com.example.grader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.grader.firebase.FirestoreAuth
import com.example.grader.navigation.NavKey
import com.example.grader.ui.DashboardScreen
import com.example.grader.ui.ExamCreatorScreen
import com.example.grader.ui.LoginScreen
import com.example.grader.ui.QuizScreen
import com.example.grader.ui.RegisterScreen
import com.example.grader.ui.StatsView
import com.example.grader.ui.StudentScreen
import com.example.grader.ui.UserProfileScreen
import com.example.grader.ui.components.NavRoute
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val firestoreAuth = FirestoreAuth()

        setContent {
            MaterialTheme {
                var isUserLoggedIn by remember {
                    mutableStateOf(firestoreAuth.currentUser != null)
                }

                if (isUserLoggedIn) {
                    GraderNavDisplay(
                        firestoreAuth = firestoreAuth,
                        onLogout = { isUserLoggedIn = false }
                    )
                } else {
                    AuthNavDisplay(
                        firestoreAuth = firestoreAuth,
                        onAuthenticated = { isUserLoggedIn = true }
                    )
                }
            }
        }
    }

    // ─── Authenticated Navigation ────────────────────────────────────────────
    @Composable
    private fun GraderNavDisplay(
        firestoreAuth: FirestoreAuth,
        onLogout: () -> Unit
    ) {
        val isAdmin = firestoreAuth.currentUser?.email == "administrador@gmail.com"

        // Determine initial destination based on role
        val startKey: NavKey = if (isAdmin) NavKey.ExamCreator else NavKey.Exams
        val backStack: SnapshotStateList<NavKey> = remember {
            mutableListOf(startKey).toMutableStateList()
        }

        /**
         * Helper that navigates bottom-nav style:
         * replaces the whole back stack with a single tab key
         * to avoid stacking tabs on top of each other.
         */
        fun navigateToTab(route: NavRoute) {
            val target: NavKey = route.toNavKey()
            // Only navigate if we're not already on the target tab
            if (backStack.lastOrNull() != target) {
                backStack.clear()
                backStack.add(target)
            }
        }

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLast()
                }
            },
            entryProvider = entryProvider {
                // ── ExamCreator (Admin "home") ──────────────────────────────
                entry<NavKey.ExamCreator> { _ ->
                    ExamCreatorScreen(
                        onNavigateBack = {
                            navigateToTab(NavRoute.EXAMS)
                        }
                    )
                }

                // ── Exams / Student list ─────────────────────────────────────
                entry<NavKey.Exams> { _ ->
                    if (isAdmin) {
                        DashboardScreen(
                            currentRoute = NavRoute.EXAMS,
                            onNavigate = { route -> navigateToTab(route) },
                            onNavigateToCreateExam = {
                                backStack.add(NavKey.ExamCreator)
                            }
                        )
                    } else {
                        StudentScreen(
                            currentRoute = NavRoute.EXAMS,
                            onNavigate = { route -> navigateToTab(route) },
                            onStartExam = { examId ->
                                backStack.add(NavKey.Quiz(examId))
                            }
                        )
                    }
                }

                // ── Quiz (pushed on top of Exams) ────────────────────────────
                entry<NavKey.Quiz> { key ->
                    QuizScreen(
                        examId = key.examId,
                        onNavigateBack = {
                            if (backStack.size > 1) backStack.removeLast()
                        },
                        onQuizFinished = { _, _ ->
                            if (backStack.size > 1) backStack.removeLast()
                        }
                    )
                }

                // ── Stats tab ────────────────────────────────────────────────
                entry<NavKey.Stats> { _ ->
                    StatsView(
                        currentRoute = NavRoute.STATS,
                        onNavigate = { route -> navigateToTab(route) }
                    )
                }

                // ── Profile tab ──────────────────────────────────────────────
                entry<NavKey.Profile> { _ ->
                    UserProfileScreen(
                        currentRoute = NavRoute.PROFILE,
                        onNavigate = { route -> navigateToTab(route) }
                    )
                }
            }
        )
    }

    // ─── Authentication Navigation ───────────────────────────────────────────
    @Composable
    private fun AuthNavDisplay(
        firestoreAuth: FirestoreAuth,
        onAuthenticated: () -> Unit
    ) {
        val coroutineScope = rememberCoroutineScope()
        val backStack: SnapshotStateList<NavKey> = remember {
            mutableListOf<NavKey>(NavKey.Login).toMutableStateList()
        }

        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLast()
                }
            },
            entryProvider = entryProvider {
                entry<NavKey.Login> { _ ->
                    LoginScreen(
                        onSignInClick = { email, password ->
                            coroutineScope.launch {
                                val result = firestoreAuth.loginUser(email, password)
                                if (result.isSuccess) {
                                    onAuthenticated()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Error al iniciar sesión",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onRegisterClick = {
                            backStack.add(NavKey.Register)
                        }
                    )
                }

                entry<NavKey.Register> { _ ->
                    RegisterScreen(
                        onRegisterClick = { email, password ->
                            coroutineScope.launch {
                                val result = firestoreAuth.registerUser(email, password)
                                if (result.isSuccess) {
                                    onAuthenticated()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Error al registrar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onBackToLoginClick = {
                            if (backStack.size > 1) backStack.removeLast()
                        }
                    )
                }
            }
        )
    }
}

// ─── Extension: bridge old NavRoute enum → new Nav3 keys ─────────────────────
private fun NavRoute.toNavKey(): NavKey = when (this) {
    NavRoute.EXAMS   -> NavKey.Exams
    NavRoute.STATS   -> NavKey.Stats
    NavRoute.PROFILE -> NavKey.Profile
}