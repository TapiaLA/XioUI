package com.niklan.xioui

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XioConsoleContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isKeyboardOpen = WindowInsets.isImeVisible

    var commandHistory by remember {
        mutableStateOf(listOf(
            "Type 'help' for a list of available commands.",
            "Root privileges: GRANTED",
            "Xio UI Terminal v2.5 (Ubuntu Color Edition)"
        ))
    }
    var currentInput by remember { mutableStateOf("") }

    val executeCommand = {
        val input = currentInput.trim()
        if (input.isNotEmpty()) {
            currentInput = ""

            coroutineScope.launch(Dispatchers.IO) {
                val newBlock = mutableListOf<String>()
                val args = input.lowercase().split(" ")
                val command = args[0]

                if (command != "clear") {
                    newBlock.add("sysadmin@xioui:~$ $input")
                }

                when (command) {
                    "help" -> {
                        newBlock.add("Available commands:")
                        newBlock.add("  clear    - Clear terminal output")
                        newBlock.add("  neofetch - Show system info & ASCII art")
                        newBlock.add("  [+] Plus any native Android Linux command (ls, pwd, id, uname...)")
                    }
                    "clear" -> {
                        withContext(Dispatchers.Main) { commandHistory = emptyList() }
                        return@launch
                    }
                    "neofetch" -> {
                        val model = Build.MODEL
                        val androidVer = Build.VERSION.RELEASE
                        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                        val batLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        val uptimeMillis = SystemClock.elapsedRealtime()
                        val hours = ((uptimeMillis / (1000 * 60 * 60)) % 24).toInt()
                        val mins = ((uptimeMillis / (1000 * 60)) % 60).toInt()

                        val asciiArt = """
            .--.            
     .___. ( XU ) .___.     
     |NIK|  `--´ /LANN|      OS: Xio UI Android
     |LAN| /X||X\NIKLA|      Host: $model
     |NIK|/XX/\XX\NNIK|      Kernel: Android $androidVer
     |LAN|XX//N\XX\LAN|      Uptime: ${hours}h ${mins}m
     |NIK|X//LAN\XX\IK|      Battery: $batLevel%
     |LAN|´/NIK/ `\X\N|      Shell: XioTerm
     |NIK|/LAN/   |\X\|     
     |LANNIKL/    |A\X\     
    /|NIKLA/      |NN\X\    
   /X|LANN/       |INK\X\   
   /´------------------`\   
                        """.trimIndent()

                        // Le agregamos un carácter secreto '§' al inicio para que nuestro
                        // motor de renderizado sepa que esta línea lleva colores especiales.
                        asciiArt.split("\n").forEach { line ->
                            newBlock.add("§$line")
                        }
                    }
                    else -> {
                        try {
                            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", input))
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                            var line: String?
                            var outputCount = 0

                            while (reader.readLine().also { line = it } != null) {
                                newBlock.add(line!!)
                                outputCount++
                            }
                            while (errorReader.readLine().also { line = it } != null) {
                                newBlock.add("bash error: $line")
                                outputCount++
                            }

                            process.waitFor()
                            if (outputCount == 0) {
                                newBlock.add("[Process completed silently]")
                            }
                        } catch (e: Exception) {
                            newBlock.add("bash: $command: command not found or invalid")
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    if (command != "clear") {
                        commandHistory = newBlock + commandHistory
                    }
                    listState.animateScrollToItem(0)
                }
            }
        }
    }

    LaunchedEffect(isKeyboardOpen, commandHistory.size) {
        if (commandHistory.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text("sysadmin@xioui:~$ ", color = Color.Green, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            BasicTextField(
                value = currentInput,
                onValueChange = { currentInput = it },
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                cursorBrush = SolidColor(Color.Green),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { executeCommand() }),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(commandHistory) { originalLine ->
                // MOTOR DE RENDERIZADO CON ANNOTATED STRING
                if (originalLine.startsWith("sysadmin@")) {
                    val annotated = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF4CAF50))) { append("sysadmin@xioui:~$ ") }
                        withStyle(SpanStyle(color = Color.White)) { append(originalLine.removePrefix("sysadmin@xioui:~$ ")) }
                    }
                    Text(text = annotated, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else if (originalLine.startsWith("§")) {
                    val line = originalLine.removePrefix("§")
                    val annotated = buildAnnotatedString {
                        val limit = minOf(line.length, 26) // Respetando tu límite de 26 caracteres
                        val artPart = line.substring(0, limit)
                        val infoPart = line.substring(limit)

                        // 1. RENDERIZAR EL ARTE (La N Roja y el compás Blanco)
                        artPart.forEach { char ->
                            if (char in listOf('N', 'I', 'K', 'L', 'A', 'U')) {
                                // Las letras de tu marca en Rojo Ubuntu
                                withStyle(SpanStyle(color = Color(0xFFFF5252))) { append(char.toString()) }
                            } else if (char == 'X' || !char.isLetterOrDigit()) {
                                // El compás de metal y su estructura en Blanco Puro
                                withStyle(SpanStyle(color = Color.White)) { append(char.toString()) }
                            } else {
                                // Respaldo Gris por si acaso
                                withStyle(SpanStyle(color = Color(0xFFB0BEC5))) { append(char.toString()) }
                            }
                        }

                        // 2. RENDERIZAR LA INFORMACIÓN CON SUBTÍTULOS ROJOS
                        val splitIndex = infoPart.indexOf(":")
                        if (splitIndex != -1) {
                            // Encontramos un ":", dividimos el texto
                            val subtitle = infoPart.substring(0, splitIndex + 1) // Incluye los ":"
                            val value = infoPart.substring(splitIndex + 1)

                            // Pintamos el subtítulo (ej. "OS:") de Rojo Ubuntu
                            withStyle(SpanStyle(color = Color(0xFFFF5252))) { append(subtitle) }
                            // Pintamos el valor (ej. " Xio UI Android") de Gris
                            withStyle(SpanStyle(color = Color(0xFFE0E0E0))) { append(value) }
                        } else {
                            // Si es una línea normal de la derecha sin ":", se queda gris
                            withStyle(SpanStyle(color = Color(0xFFE0E0E0))) { append(infoPart) }
                        }
                    }
                    Text(text = annotated, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                else if (originalLine.startsWith("bash error:")) {
                    Text(text = originalLine, color = Color(0xFFFF5252), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                else {
                    Text(text = originalLine, color = Color(0xFFE0E0E0), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}