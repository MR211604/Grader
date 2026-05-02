package com.example.grader.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class NavRoute(val title: String, val icon: ImageVector) {
    EXAMS("Exams", Icons.Outlined.Description),
    STATS("Stats", Icons.Outlined.GridView),
    PROFILE("Profile", Icons.Outlined.Person)
}

@Composable
fun GraderBottomNavigation(
    currentRoute: NavRoute,
    onNavigate: (NavRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryBlue = Color(0xFF0C5CBF)

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 8.dp
    ) {
        NavRoute.entries.forEach { route ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = { Icon(route.icon, contentDescription = route.title) },
                label = { Text(route.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = primaryBlue,
                    selectedTextColor = primaryBlue,
                    indicatorColor = primaryBlue.copy(alpha = 0.1f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
