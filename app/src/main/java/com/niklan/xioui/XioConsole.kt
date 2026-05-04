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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
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
            "Xio UI Terminal v2.0 (Native Shell Integration)"
        ))
    }
    var currentInput by remember { mutableStateOf("") }

    val executeCommand = {
        val input = currentInput.trim()
        if (input.isNotEmpty()) {
            currentInput = "" // Limpiamos la barra de inmediato para mayor fluidez

            // Lanzamos un hilo secundario para no congelar la UI con comandos pesados
            coroutineScope.launch(Dispatchers.IO) {
                val newBlock = mutableListOf<String>()
                val args = input.lowercase().split(" ")
                val command = args[0]

                // 1. EL ECO DEL COMANDO
                if (command != "clear") {
                    newBlock.add("sysadmin@xioui:~$ $input")
                }

                // 2. LÓGICA DE COMANDOS PROPIOS (Los que creamos nosotros)
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
                    "ping" -> {
                        val target = if (args.size > 1) args[1] else "8.8.8.8"
                        newBlock.add("PING $target (Native Kotlin ICMP/TCP)...")

                        try {
                            // Usamos la librería nativa de red para hacer el ping real
                            val inet = java.net.InetAddress.getByName(target)
                            val isReachable = inet.isReachable(3000) // 3 segundos de timeout

                            if (isReachable) {
                                newBlock.add("64 bytes from $target: icmp_seq=1 time < 3000 ms")
                                newBlock.add("64 bytes from $target: icmp_seq=2 time < 3000 ms")
                                newBlock.add("64 bytes from $target: icmp_seq=3 time < 3000 ms")
                                newBlock.add("--- $target ping statistics ---")
                                newBlock.add("3 packets transmitted, 3 received, 0% packet loss")
                            } else {
                                newBlock.add("From $target icmp_seq=1 Destination Host Unreachable")
                                newBlock.add("1 packets transmitted, 0 received, 100% packet loss")
                            }
                        } catch (e: Exception) {
                            newBlock.add("ping: $target: Name or service not known")
                        }
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
                        newBlock.addAll(asciiArt.split("\n"))
                    }
                    else -> {
                        // 3. MAGIA PURA: Si no es un comando nuestro, se lo pasamos al kernel real de Linux/Android
                        try {
                            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", input))
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                            var line: String?
                            var outputCount = 0

                            // Leemos la respuesta exitosa del sistema
                            while (reader.readLine().also { line = it } != null) {
                                newBlock.add(line!!)
                                outputCount++
                            }
                            // Leemos los errores del sistema (ej. Permission denied)
                            while (errorReader.readLine().also { line = it } != null) {
                                newBlock.add("bash error: $line")
                                outputCount++
                            }

                            process.waitFor() // Esperamos a que termine de ejecutarse

                            if (outputCount == 0) {
                                newBlock.add("[Process completed silently]")
                            }
                        } catch (e: Exception) {
                            newBlock.add("bash: $command: command not found or invalid")
                        }
                    }
                }

                // 4. REGRESAMOS AL HILO PRINCIPAL PARA ACTUALIZAR LA PANTALLA
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
            items(commandHistory) { line ->
                // Si la línea es un error, la pintamos de rojo. Si es el usuario, verde. Si es salida normal, gris.
                val textColor = when {
                    line.startsWith("sysadmin@") -> Color(0xFF4CAF50) // Verde
                    line.startsWith("bash error:") -> Color(0xFFFF5252) // Rojo
                    else -> Color(0xFFE0E0E0) // Gris claro
                }
                Text(text = line, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                if (line.startsWith("sysadmin@")) Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}