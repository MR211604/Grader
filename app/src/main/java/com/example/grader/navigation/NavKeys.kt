package com.example.grader.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation keys for Navigation 3.
 *
 * Each sealed interface groups related destinations.
 * Keys are serializable data classes/objects that carry
 * any data needed by the destination screen.
 */
sealed interface NavKey {

    // ─── Authentication Flow ─────────────────────────────────────────────────
    @Serializable
    data object Login : NavKey

    @Serializable
    data object Register : NavKey

    // ─── Main App (Bottom-Nav Tabs) ──────────────────────────────────────────
    @Serializable
    data object Exams : NavKey

    @Serializable
    data object Stats : NavKey

    @Serializable
    data object Profile : NavKey

    // ─── Sub-destinations pushed on top of a tab ─────────────────────────────
    @Serializable
    data object ExamCreator : NavKey

    @Serializable
    data class Quiz(val examId: String) : NavKey
}
