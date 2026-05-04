package com.niklan.xioui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap

data class AppItem(val name: String, val packageName: String, val icon: Drawable, val installTime: Long)

class XioPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("XioSettings", Context.MODE_PRIVATE)
    fun getPinnedApps(): List<String> {
        val savedString = prefs.getString("pinned_apps", "") ?: ""
        return if (savedString.isEmpty()) emptyList() else savedString.split(",")
    }
    fun savePinnedApps(packages: List<String>) {
        prefs.edit { putString("pinned_apps", packages.joinToString(",")) }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { XioHomeScreen() }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun XioHomeScreen() {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val xioPrefs = remember { XioPreferences(context) }

    var showLeftMenu by remember { mutableStateOf(false) }
    var showRightMenu by remember { mutableStateOf(false) }
    var showAllApps by remember { mutableStateOf(false) }

    // ESTADOS DEL SISTEMA MULTIVENTANA
    var isTaskOpen by remember { mutableStateOf(false) }
    var isTaskMinimized by remember { mutableStateOf(false) }

    // NUEVO: Variables de la consola
    var isConsoleOpen by remember { mutableStateOf(false) }
    var isConsoleMinimized by remember { mutableStateOf(false) }

    var focusedWindow by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val closeAllMenus = { showLeftMenu = false; showRightMenu = false; showAllApps = false }

    val colorPrimary = Color(0xFFE0E0E0)
    val colorBackground = Color.Black
    val colorPanel = Color(0xFF121212)

    var sortMode by remember { mutableStateOf("AZ") }
    val pinnedAppPackages = remember { mutableStateListOf(*xioPrefs.getPinnedApps().toTypedArray()) }

    val allInstalledApps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        resolveInfos.map {
            val pkgName = it.activityInfo.packageName
            val installTime = try { packageManager.getPackageInfo(pkgName, 0).firstInstallTime } catch(_: Exception) { 0L }
            AppItem(name = it.loadLabel(packageManager).toString(), packageName = pkgName, icon = it.loadIcon(packageManager), installTime = installTime)
        }
    }

    val pinnedApps = allInstalledApps.filter { pinnedAppPackages.contains(it.packageName) }
    val displayedApps = if (sortMode == "AZ") allInstalledApps.sortedBy { it.name.lowercase() } else allInstalledApps.sortedByDescending { it.installTime }

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
                            if (dragX > 50) showLeftMenu = true else if (dragX < -50) showRightMenu = true
                        } else {
                            if (dragY < -50) showAllApps = true
                        }
                    }
                ) { change, dragAmount -> change.consume(); dragX += dragAmount.x; dragY += dragAmount.y }
            }
    ) {
        // --- 1. MENÚ IZQUIERDO (Herramientas) ---
        AnimatedVisibility(visible = showLeftMenu, enter = slideInHorizontally(initialOffsetX = { -it }), exit = slideOutHorizontally(targetOffsetX = { -it }), modifier = Modifier.align(Alignment.CenterStart)) {
            Column(modifier = Modifier.width(75.dp).fillMaxHeight(0.6f).background(colorPanel, shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)).padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                XioToolButton("Task", Icons.Default.Info, colorPrimary) {
                    isTaskOpen = true
                    isTaskMinimized = false
                    focusedWindow = "TASK"
                    closeAllMenus()
                }
                XioToolButton("Term", Icons.Default.Build, colorPrimary) {
                    isConsoleOpen = true
                    isConsoleMinimized = false
                    focusedWindow = "TERM"
                    closeAllMenus()
                }
                XioToolButton("SNMP", Icons.Default.Share, colorPrimary) { }
                XioToolButton("Conf", Icons.Default.Settings, colorPrimary) { }
            }
        }

        // --- 2. MENÚ DERECHO (Ruleta de Apps FIJADAS) ---
        AnimatedVisibility(visible = showRightMenu, enter = slideInHorizontally(initialOffsetX = { it }), exit = slideOutHorizontally(targetOffsetX = { it }), modifier = Modifier.align(Alignment.CenterEnd)) {
            if (pinnedApps.isNotEmpty()) {
                val startIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % pinnedApps.size)
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

                LazyColumn(state = listState, modifier = Modifier.width(100.dp).fillMaxHeight(0.6f).background(colorPanel, shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)), contentPadding = PaddingValues(vertical = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(count = Int.MAX_VALUE) { index ->
                        val realIndex = index % pinnedApps.size
                        val app = pinnedApps[realIndex]
                        var dragOffsetX by remember { mutableFloatStateOf(0f) }

                        Box(modifier = Modifier.graphicsLayer {
                            val itemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (itemInfo != null) {
                                val viewportCenter = listState.layoutInfo.viewportEndOffset / 2
                                val itemCenter = itemInfo.offset + (itemInfo.size / 2)
                                val fraction = 1f - (abs(viewportCenter - itemCenter).toFloat() / viewportCenter.toFloat()).coerceIn(0f, 1f)
                                scaleX = 0.6f + (0.4f * fraction); scaleY = 0.6f + (0.4f * fraction)
                                translationX = dragOffsetX
                                alpha = (0.4f + (0.6f * fraction)) * (1f - (abs(dragOffsetX) / 200f).coerceIn(0f, 1f))
                            }
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDrag = { change, dragAmount -> change.consume(); if (dragAmount.x < 0 || dragOffsetX < 0) dragOffsetX += dragAmount.x },
                                        onDragEnd = { if (dragOffsetX < -150f) { pinnedAppPackages.remove(app.packageName); xioPrefs.savePinnedApps(pinnedAppPackages) }; dragOffsetX = 0f },
                                        onDragCancel = { dragOffsetX = 0f }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName); if (launchIntent != null) { context.startActivity(launchIntent); closeAllMenus() } })
                                }
                                .padding(8.dp)
                            ) {
                                Image(bitmap = app.icon.toImageBitmap(), contentDescription = app.name, modifier = Modifier.size(32.dp).padding(bottom = 4.dp), colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
                                Text(text = app.name, color = colorPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, modifier = Modifier.width(50.dp))
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.width(85.dp).fillMaxHeight(0.6f).background(colorPanel, shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)), contentAlignment = Alignment.Center) { Text("Vacío", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
            }
        }

        // --- 3. CAJÓN INFERIOR ---
        if (showAllApps) {
            ModalBottomSheet(onDismissRequest = { showAllApps = false }, sheetState = sheetState, containerColor = Color(0xFF1E1E1E), scrimColor = Color(0xAA000000), dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Sistema", color = colorPrimary, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { sortMode = "AZ" }) { Text("A-Z", color = if (sortMode == "AZ") Color.Green else Color.Gray, fontFamily = FontFamily.Monospace) }
                            TextButton(onClick = { sortMode = "TIME" }) { Text("Recientes", color = if (sortMode == "TIME") Color.Green else Color.Gray, fontFamily = FontFamily.Monospace) }
                        }
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(displayedApps) { app ->
                            var showAppMenu by remember { mutableStateOf(false) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
                                .combinedClickable(
                                    onClick = { val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName); if (launchIntent != null) { context.startActivity(launchIntent); showAllApps = false } },
                                    onLongClick = { showAppMenu = true }
                                ).padding(8.dp)
                            ) {
                                Image(bitmap = app.icon.toImageBitmap(), contentDescription = app.name, modifier = Modifier.size(40.dp).padding(bottom = 4.dp), colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }))
                                Text(text = app.name, color = colorPrimary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                                DropdownMenu(expanded = showAppMenu, onDismissRequest = { showAppMenu = false }, modifier = Modifier.background(Color(0xFF2C2C2C))) {
                                    DropdownMenuItem(text = { Text(if (pinnedAppPackages.contains(app.packageName)) "✅ Fijada" else "📌 Fijar", color = Color.White, fontFamily = FontFamily.Monospace) }, onClick = { if (!pinnedAppPackages.contains(app.packageName)) { pinnedAppPackages.add(app.packageName); xioPrefs.savePinnedApps(pinnedAppPackages) }; showAppMenu = false })
                                    DropdownMenuItem(text = { Text("🗑️ Desinstalar", color = Color.Red, fontFamily = FontFamily.Monospace) }, onClick = { val uri =
                                        "package:${app.packageName}".toUri(); val uninstallIntent = Intent(Intent.ACTION_DELETE, uri); uninstallIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK; context.startActivity(uninstallIntent); showAppMenu = false })
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. RENDERIZADO DE VENTANAS FLOTANTES Y Z-ORDER ---
        // Se instancian las memorias de las ventanas.
        // Sobreviven incluso si la ventana desaparece visualmente.
        val taskState = remember { WindowState() }
        val termState = remember { WindowState() }

        val activeWindows = mutableListOf<String>()
        if (isTaskOpen && !isTaskMinimized) activeWindows.add("TASK")
        if (isConsoleOpen && !isConsoleMinimized) activeWindows.add("TERM")

        // Para evitar problemas de reconstrucción gráfica, envolvemos la llamada en un bloque 'key'
        activeWindows.sortedBy { it == focusedWindow }.forEach { window ->
            key(window) {
                when (window) {
                    "TASK" -> {
                        XioMovableWindow(
                            title = "Task Manager",
                            icon = Icons.Default.Info,
                            state = taskState, // Pasamos su memoria
                            onClose = { isTaskOpen = false },
                            onMinimize = { isTaskMinimized = true },
                            onFocus = { focusedWindow = "TASK" }
                        ) {
                            XioTaskManagerContent()
                        }
                    }
                    "TERM" -> {
                        XioMovableWindow(
                            title = "Xio Terminal",
                            icon = Icons.Default.Build,
                            state = termState, // Pasamos su memoria
                            onClose = { isConsoleOpen = false },
                            onMinimize = { isConsoleMinimized = true },
                            onFocus = { focusedWindow = "TERM" }
                        ) {
                            XioConsoleContent()
                        }
                    }
                }
            }
        }

        // --- 5. EL DOCK DE APPS MINIMIZADAS ---
        AnimatedVisibility(
            visible = isTaskMinimized || isConsoleMinimized, // Modificado para soportar ambos
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            Row(modifier = Modifier.background(Color(0xEE121212), RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (isTaskMinimized) {
                    XioDockIcon(icon = Icons.Default.Info, title = "Task",
                        onRestore = { isTaskMinimized = false; focusedWindow = "TASK" },
                        onClose = { isTaskOpen = false; isTaskMinimized = false }
                    )
                }
                if (isConsoleMinimized) {
                    XioDockIcon(icon = Icons.Default.Build, title = "Term",
                        onRestore = { isConsoleMinimized = false; focusedWindow = "TERM" },
                        onClose = { isConsoleOpen = false; isConsoleMinimized = false }
                    )
                }
            }
        }
    }
}

// COMPONENTES DE INTERFAZ REUTILIZABLES
@Composable
fun XioDockIcon(icon: ImageVector, title: String, onRestore: () -> Unit, onClose: () -> Unit) {
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { translationY = dragOffsetY; alpha = 1f - (abs(dragOffsetY) / 150f).coerceIn(0f, 1f) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount -> change.consume(); if (dragAmount.y < 0 || dragOffsetY < 0) dragOffsetY += dragAmount.y },
                    onDragEnd = { if (dragOffsetY < -100f) onClose() else dragOffsetY = 0f },
                    onDragCancel = { dragOffsetY = 0f }
                )
            }
            .clickable { onRestore() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = title, tint = Color.Green, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.size(4.dp).background(Color.Green, CircleShape))
    }
}

@Composable
fun XioToolButton(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.padding(8.dp)) {
        Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(32.dp).padding(bottom = 4.dp))
        Text(text = title, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, modifier = Modifier.width(60.dp))
    }
}

fun Drawable.toImageBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    if (this is BitmapDrawable) return this.bitmap.asImageBitmap()
    val bitmap = createBitmap(
        if (intrinsicWidth > 0) intrinsicWidth else 1,
        if (intrinsicHeight > 0) intrinsicHeight else 1
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}