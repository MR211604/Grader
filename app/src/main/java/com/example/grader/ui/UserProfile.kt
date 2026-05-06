package com.example.grader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grader.contants.BACKGROUND_COLOR
import com.example.grader.contants.DIVIDER_COLOR

import com.example.grader.ui.components.GraderBottomNavigation
import com.example.grader.ui.components.NavRoute
import java.util.Locale
import java.util.Locale.getDefault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    currentRoute: NavRoute = NavRoute.PROFILE,
    onNavigate: (NavRoute) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    userName: String = "",
    userEmail: String = "",
    isAdmin: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    val borderSize = 1.dp.toPx()
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = borderSize
                    )
                },
                title = {
                    Text(
                        "Perfil",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            GraderBottomNavigation(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                isAdmin = isAdmin
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BACKGROUND_COLOR)
        ) {
            HorizontalDivider(color = DIVIDER_COLOR, thickness = 1.dp)
            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    ProfileHeader(userEmail, userName, isAdmin)
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))
                    LogoutButton(onLogoutClick = onLogoutClick)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

    }
}

@Composable
fun ProfileHeader(
    userEmail: String = "",
    userName: String = "",
    isAdmin: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(100.dp)
        ) {
            Surface(
                shape = CircleShape,
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.Center),
                color = Color(0xFFD3D3D3)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Avatar",
                    modifier = Modifier.padding(16.dp),
                    tint = Color.DarkGray
                )
            }

            // Online Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = userEmail,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    label = "ID DE ESTUDIANTE",
                    value = "${userName.subSequence(1, 3).toString().uppercase(getDefault())}-2026-GRADER"
                )
                InfoCard(
                    modifier = Modifier.weight(1f),
                    label = "MATRÍCULA",
                    value = "2026"
                )
            }
        }
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(true).copy(brush = androidx.compose.ui.graphics.SolidColor(Color.LightGray.copy(alpha = 0.5f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun LogoutButton(onLogoutClick: () -> Unit) {
    OutlinedButton(
        onClick = { onLogoutClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFE53935)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cerrar sesión",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfilePreview() {
    MaterialTheme {
        UserProfileScreen()
    }
}
