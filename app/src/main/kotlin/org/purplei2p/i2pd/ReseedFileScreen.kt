package org.purplei2p.i2pd

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.purplei2p.i2pd.config.IniDocument
import org.purplei2p.i2pd.service.RouterController

@Composable
fun ReseedFileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = remember(context) { RouterController.config(context) }
    val currentText by config.currentText.collectAsStateWithLifecycle()
    val source = remember(currentText) { currentReseedSource(currentText) }
    var message by remember { mutableStateOf<ReseedScreenMessage?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(config) {
        runCatching { config.load() }.onFailure {
            message = ReseedScreenMessage.Failed(it.message ?: it.javaClass.simpleName)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = null
            try {
                val imported = RouterController.data(context).importReseedFile(uri)
                message = ReseedScreenMessage.Imported(imported.displayName)
            } catch (error: Exception) {
                message = ReseedScreenMessage.Failed(error.message ?: error.javaClass.simpleName)
            } finally {
                busy = false
            }
        }
    }

    val sourceText = if (source == null) {
        stringResource(R.string.reseed_file_default)
    } else {
        val kind = stringResource(if (source.optionName == "reseed.file") R.string.reseed_file_su3 else R.string.reseed_file_zip)
        stringResource(R.string.reseed_file_current, kind, source.value)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RestartBanner()
        Text(stringResource(R.string.reseed_file_intro), style = MaterialTheme.typography.bodyMedium)
        Text(sourceText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.reseed_file_trust), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                enabled = !busy,
                onClick = { picker.launch(arrayOf("application/octet-stream", "application/zip", "application/x-zip-compressed", "*/*")) },
            ) { Text(stringResource(R.string.reseed_file_choose)) }
            TextButton(
                enabled = !busy && source != null,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            RouterController.data(context).useNetworkReseedServers()
                            message = ReseedScreenMessage.Cleared
                        } catch (error: Exception) {
                            message = ReseedScreenMessage.Failed(error.message ?: error.javaClass.simpleName)
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text(stringResource(R.string.reseed_file_use_servers)) }
        }
        message?.let { currentMessage ->
            val messageText = when (currentMessage) {
                is ReseedScreenMessage.Imported -> stringResource(R.string.reseed_file_imported, currentMessage.fileName)
                ReseedScreenMessage.Cleared -> stringResource(R.string.reseed_file_cleared)
                is ReseedScreenMessage.Failed -> stringResource(R.string.reseed_file_failed, currentMessage.detail)
            }
            Text(
                messageText,
                color = if (currentMessage is ReseedScreenMessage.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private sealed interface ReseedScreenMessage {
    data class Imported(val fileName: String) : ReseedScreenMessage
    data object Cleared : ReseedScreenMessage
    data class Failed(val detail: String) : ReseedScreenMessage
}

private data class CurrentReseedSource(val optionName: String, val value: String)

private fun currentReseedSource(text: String?): CurrentReseedSource? {
    if (text == null) return null
    val entries = IniDocument.parse(text).entries
    fun value(option: String): String? = entries.firstNotNullOfOrNull { entry ->
        val name = if (entry.section.isEmpty()) entry.key else "${entry.section}.${entry.key}"
        option.takeIf { name == option }?.let { entry.value }
    }?.takeIf { it.isNotBlank() }
    val su3 = value("reseed.file")
    if (su3 != null) return CurrentReseedSource("reseed.file", su3)
    val zip = value("reseed.zipfile")
    if (zip != null) return CurrentReseedSource("reseed.zipfile", zip)
    return null
}
