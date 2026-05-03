package com.example.grader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.grader.ui.DashboardScreen
import com.example.grader.ui.LoginScreen
import com.example.grader.ui.UserProfileScreen
import com.example.grader.ui.components.NavRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                // Estado mockeado: false = sin sesión, true = sesión activa
                var isUserLoggedIn by remember { mutableStateOf(false) }
                
                if (isUserLoggedIn) {
                    var currentRoute by remember { mutableStateOf(NavRoute.EXAMS) }
                    
                    when (currentRoute) {
                        NavRoute.EXAMS, NavRoute.STATS -> {
                            DashboardScreen(
                                currentRoute = currentRoute,
                                onNavigate = { currentRoute = it }
                            )
                        }
                        NavRoute.PROFILE -> {
                            UserProfileScreen(
                                currentRoute = currentRoute,
                                onNavigate = { currentRoute = it }
                            )
                        }
                    }
                } else {
                    LoginScreen(
                        onSignInClick = { _, _ -> 
                            // Simulamos inicio de sesión
                            isUserLoggedIn = true 
                        }
                    )
                }
            }
        }
    }
}