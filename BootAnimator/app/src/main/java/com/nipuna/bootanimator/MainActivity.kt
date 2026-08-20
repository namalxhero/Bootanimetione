package com.nipuna.bootanimator

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nipuna.bootanimator.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private var onFilePicked: ((Uri) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pickLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let { onFilePicked?.invoke(it) }
        }

        setContent {
            BootAnimatorTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    BootAnimatorScreen(
                        onPickZip = { callback ->
                            onFilePicked = callback
                            pickLauncher.launch(arrayOf("application/zip"))
                        }
                    )
                }
            }
        }
    }
}

private enum class RootState { UNKNOWN, CHECKING, GRANTED, DENIED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BootAnimatorScreen(onPickZip: (onPicked: (Uri) -> Unit) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var rootState by remember { mutableStateOf(RootState.UNKNOWN) }
    var detectedPath by remember { mutableStateOf<String?>(null) }
    var hasBackup by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }
    var busy by remember { mutableStateOf(false) }

    fun addLog(line: String) {
        log = log + line
    }

    fun checkRoot() {
        rootState = RootState.CHECKING
        scope.launch {
            val granted = withContext(Dispatchers.IO) { RootUtils.hasRoot() }
            rootState = if (granted) RootState.GRANTED else RootState.DENIED
            addLog(if (granted) "Root access granted." else "Root access denied. Grant it in Magisk.")
            if (granted) {
                hasBackup = withContext(Dispatchers.IO) { BootAnimManager.hasBackup() }
            }
        }
    }

    fun runDetect() {
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { BootAnimManager.autoDetect() }
            log = log + result.log
            detectedPath = result.path
            busy = false
        }
    }

    fun runBackup() {
        val path = detectedPath ?: return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { BootAnimManager.backupCurrent(path) }
            addLog(if (ok) "Backup saved to ${BootAnimManager.BACKUP_FILE}" else "Backup failed.")
            hasBackup = ok || hasBackup
            busy = false
        }
    }

    fun runRestore() {
        val path = detectedPath ?: return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { BootAnimManager.restore(path) }
            addLog(if (ok) "Original boot animation restored. Reboot to see it." else "Restore failed.")
            busy = false
        }
    }

    fun applyPickedZip(uri: Uri) {
        val path = detectedPath ?: return
        busy = true
        scope.launch {
            addLog("Copying selected zip...")
            val staged = withContext(Dispatchers.IO) {
                try {
                    val out = File(context.cacheDir, "staged_bootanimation.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                    out.absolutePath
                } catch (e: Exception) {
                    null
                }
            }
            if (staged == null) {
                addLog("Failed to read selected file.")
                busy = false
                return@launch
            }
            val ok = withContext(Dispatchers.IO) { BootAnimManager.apply(staged, path) }
            addLog(if (ok) "New boot animation applied! Reboot to see it." else "Apply failed - check root grant.")
            busy = false
        }
    }

    LaunchedEffect(Unit) { checkRoot() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Boot Animator",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            "One UI boot animation switcher",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
        )

        // Root status card
        StatusCard(
            icon = when (rootState) {
                RootState.GRANTED -> Icons.Filled.CheckCircle
                RootState.DENIED -> Icons.Filled.Cancel
                else -> Icons.Filled.Security
            },
            iconTint = when (rootState) {
                RootState.GRANTED -> SuccessGreen
                RootState.DENIED -> ErrorRed
                else -> TextSecondary
            },
            title = "Root Access",
            subtitle = when (rootState) {
                RootState.UNKNOWN -> "Not checked yet"
                RootState.CHECKING -> "Requesting Magisk permission..."
                RootState.GRANTED -> "Granted"
                RootState.DENIED -> "Denied - tap to retry"
            },
            onClick = { checkRoot() }
        )

        Spacer(Modifier.height(12.dp))

        // Detect card
        StatusCard(
            icon = Icons.Filled.TravelExplore,
            iconTint = if (detectedPath != null) SuccessGreen else OneUIBlue,
            title = "Auto-Detect Boot Animation",
            subtitle = detectedPath ?: "Tap to scan system partitions",
            onClick = { if (rootState == RootState.GRANTED && !busy) runDetect() }
        )

        Spacer(Modifier.height(20.dp))

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { runBackup() },
                enabled = detectedPath != null && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = CardDarkAlt),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup")
            }
            Button(
                onClick = {
                    onPickZip { uri -> applyPickedZip(uri) }
                },
                enabled = detectedPath != null && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Apply New")
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { runRestore() },
                enabled = detectedPath != null && hasBackup && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = CardDarkAlt),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore")
            }
            Button(
                onClick = { BootAnimManager.reboot() },
                enabled = rootState == RootState.GRANTED && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reboot", color = Color.Black)
            }
        }

        Spacer(Modifier.height(20.dp))

        if (busy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = OneUIBlue
            )
            Spacer(Modifier.height(12.dp))
        }

        Text("Log", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(log) { line ->
                    Text(line, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 15.sp)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

