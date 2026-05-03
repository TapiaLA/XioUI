package com.niklan.xioui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XioHomeScreen()
        }
    }
}

@Composable
fun XioHomeScreen() {
    var showLeftMenu by remember { mutableStateOf(false) }
    var showRightMenu by remember { mutableStateOf(false) }
    var showAllApps by remember { mutableStateOf(false) }

    val closeAllMenus = {
        showLeftMenu = false
        showRightMenu = false
        showAllApps = false
    }

    // PALETA DE COLORES CANSANCIO VISUAL CERO (Silver/Gris)
    val colorPrimary = Color(0xFFE0E0E0) // Gris Plata Claro (Textos principales)
    val colorSecondary = Color(0xFFA0A0A0) // Gris Medio (Iconos no seleccionados)
    val colorBackground = Color.Black
    val colorPanel = Color(0xFF121212) // Gris casi negro para paneles

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorBackground)
            .clickable { closeAllMenus() }
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f

                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDragEnd = {
                        val isHorizontal = abs(dragX) > abs(dragY)
                        if (isHorizontal) {
                            if (dragX > 50) showLeftMenu = true
                            else if (dragX < -50) showRightMenu = true
                        } else {
                            if (dragY < -50) showAllApps = true
                            else if (dragY > 50) closeAllMenus()
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragX += dragAmount.x
                    dragY += dragAmount.y
                }
            }
    ) {
        // --- 1. MENÚ IZQUIERDO (Herramientas Xio) ---
        AnimatedVisibility(
            visible = showLeftMenu,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(colorPanel)
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                XioAppButton("Task", Icons.Default.Info, colorPrimary) { }
                XioAppButton("Term", Icons.Default.Build, colorPrimary) { }
                XioAppButton("SNMP", Icons.Default.Share, colorPrimary) { }
                XioAppButton("Conf", Icons.Default.Settings, colorPrimary) { }
            }
        }

        // --- 2. MENÚ DERECHO (RULETA INFINITA DE APPS FIJADAS) ---
        AnimatedVisibility(
            visible = showRightMenu,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            // Simulamos 8 apps fijadas por ahora
            val mockApps = listOf("WhatsApp", "Termux", "Cámara", "Chrome", "Spotify", "Archivos", "GitHub", "Ajustes")

            // Calculamos un punto medio gigante para que puedas scrollear hacia arriba desde el segundo 1
            val startIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % mockApps.size)
            val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .background(colorPanel),
                contentPadding = PaddingValues(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(count = Int.MAX_VALUE) { index ->
                    // El truco mágico: envuelve el número infinito al tamaño real de tu lista
                    val realIndex = index % mockApps.size
                    XioAppButton(mockApps[realIndex], Icons.Default.PlayArrow, colorSecondary) {
                        // Aquí abriremos la app
                    }
                }
            }
        }

        // --- 3. CAJÓN INFERIOR (Todas las Apps) ---
        AnimatedVisibility(
            visible = showAllApps,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                Text("Todas las aplicaciones", color = colorPrimary, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun XioAppButton(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(28.dp).padding(bottom = 4.dp)
        )
        Text(
            text = title,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}