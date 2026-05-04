package com.niklan.xioui

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class WindowState {
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var isMaximized by mutableStateOf(false)
}

@Composable
fun XioMovableWindow(
    title: String,
    icon: ImageVector,
    state: WindowState,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onFocus: () -> Unit,
    content: @Composable () -> Unit
) {
    // MODIFICACIÓN: Altura y anchura fijas para hacerlas "cuadraditas"
    val windowModifier = if (state.isMaximized) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .offset { IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()) }
            .width(320.dp)
            .height(340.dp) // <--- Aquí está la altura que las hace cuadradas
            .padding(bottom = 16.dp)
    }

    Box(
        modifier = windowModifier
            .pointerInput(Unit) { detectTapGestures(onPress = { onFocus() }) }
            .background(Color(0xFF181818), shape = if (state.isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(state.isMaximized) {
                        if (!state.isMaximized) {
                            detectDragGestures(
                                onDragStart = { onFocus() }
                            ) { change, dragAmount ->
                                change.consume()
                                state.offsetX += dragAmount.x
                                state.offsetY += dragAmount.y
                            }
                        }
                    }
                    .background(Color(0xFF222222), shape = if (state.isMaximized) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = title, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { onMinimize() })
                    Box(modifier = Modifier.size(14.dp).border(1.5.dp, Color.Gray, RoundedCornerShape(2.dp)).clickable {
                        state.isMaximized = !state.isMaximized
                        onFocus()
                    })
                    Icon(Icons.Default.Clear, contentDescription = "Cerrar", tint = Color.Red, modifier = Modifier.size(18.dp).clickable { onClose() })
                }
            }

            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun XioTaskManagerContent() {
    val context = LocalContext.current
    var usedRamGb by remember { mutableFloatStateOf(0f) }
    var totalRamGb by remember { mutableFloatStateOf(0f) }
    var ramPercentage by remember { mutableFloatStateOf(0f) }
    var usedStorageGb by remember { mutableFloatStateOf(0f) }
    var totalStorageGb by remember { mutableFloatStateOf(0f) }
    var storagePercentage by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        val path = Environment.getDataDirectory()

        while (true) {
            activityManager.getMemoryInfo(memoryInfo)
            totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)
            usedRamGb = totalRamGb - (memoryInfo.availMem / (1024f * 1024f * 1024f))
            ramPercentage = usedRamGb / totalRamGb

            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            totalStorageGb = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
            usedStorageGb = totalStorageGb - ((stat.availableBlocksLong * blockSize) / (1024f * 1024f * 1024f))
            storagePercentage = usedStorageGb / totalStorageGb
            delay(1000)
        }
    }

    // MODIFICACIÓN: Agregado verticalScroll por si la ventana cuadradita es muy pequeña en tu A30
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Memoria RAM", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { ramPercentage }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = if (ramPercentage > 0.8f) Color.Red else Color.Green, trackColor = Color(0xFF2C2C2C))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = String.format("%.1f GB / %.1f GB (%.0f%%)", usedRamGb, totalRamGb, ramPercentage * 100), color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Almacenamiento Interno", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { storagePercentage }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = if (storagePercentage > 0.9f) Color.Red else Color(0xFF00BFFF), trackColor = Color(0xFF2C2C2C))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = String.format("%.1f GB / %.1f GB (%.0f%%)", usedStorageGb, totalStorageGb, storagePercentage * 100), color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}