package org.purplei2p.i2pd

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.purplei2p.i2pd.config.OptionGroup
import org.purplei2p.i2pd.nativelib.I2pdNative
import org.purplei2p.i2pd.service.AppSettingsStore
import org.purplei2p.i2pd.service.KeepAwake
import org.purplei2p.i2pd.service.OemAutostart
import org.purplei2p.i2pd.service.RouterController
import org.purplei2p.i2pd.service.ThemeMode

private enum class SettingsPage(val title: Int) {
    MAIN(R.string.tab_settings),
    ROUTER_BASIC(R.string.router_basic),
    ROUTER_ADVANCED(R.string.router_advanced),
    ROUTER_RAW(R.string.router_raw),
    RESEED_FILE(R.string.reseed_file_title),
    STORAGE(R.string.data_storage),
    FILES(R.string.data_files),
    VPN_ROUTING(R.string.vpn_routing_title),
    LICENSES(R.string.about_licenses),
}

/** Places the settings tab can be opened at from other screens; passed as a saveable string. */
object SettingsTarget {
    private const val GROUP = "group:"

    /** A group of the basic router settings. */
    fun group(group: OptionGroup) = GROUP + group.name

    fun groupOf(target: String): OptionGroup? =
        target.takeIf { it.startsWith(GROUP) }?.let { name -> OptionGroup.entries.firstOrNull { it.name == name.removePrefix(GROUP) } }
}

/** Router (i2pd.conf) and application settings. [target] (see SettingsTarget) opens a page at once. */
@Composable
fun SettingsScreen(checks: SystemChecks, target: String?, onTargetOpened: () -> Unit, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.MAIN) }
    var focusGroup by rememberSaveable { mutableStateOf<OptionGroup?>(null) }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        SettingsTarget.groupOf(target)?.let {
            focusGroup = it
            page = SettingsPage.ROUTER_BASIC
        }
        onTargetOpened()
    }
    val close = {
        page = SettingsPage.MAIN
        focusGroup = null
    }
    if (page == SettingsPage.MAIN) {
        SettingsMain(checks, onOpen = { page = it }, modifier = modifier)
        return
    }
    BackHandler(onBack = close)
    val session = rememberConfigSession()
    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = close) { Text(stringResource(R.string.action_back)) }
            Text(stringResource(page.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        when (page) {
            SettingsPage.ROUTER_BASIC -> RouterBasicScreen(session, focusGroup)
            SettingsPage.ROUTER_ADVANCED -> RouterAdvancedScreen(session)
            SettingsPage.ROUTER_RAW -> RawConfigScreen()
            SettingsPage.RESEED_FILE -> ReseedFileScreen()
            SettingsPage.STORAGE -> StorageScreen()
            SettingsPage.FILES -> FilesScreen()
            SettingsPage.VPN_ROUTING -> VpnRoutingScreen(RouterController.settings(LocalContext.current), modifier)
            SettingsPage.LICENSES -> LicensesScreen()
            SettingsPage.MAIN -> Unit
        }
    }
}

@Composable
private fun SettingsMain(checks: SystemChecks, onOpen: (SettingsPage) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val store = RouterController.settings(context)
    val settings by store.settings.collectAsStateWithLifecycle()
    var choosingTimeout by rememberSaveable { mutableStateOf(false) }
    var choosingTheme by rememberSaveable { mutableStateOf(false) }
    var choosingKeepAwake by rememberSaveable { mutableStateOf(false) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            store.update { it.copy(vpnEnabled = true) }
            ContextCompat.startForegroundService(
                context,
                Intent(context, I2pdVpnService::class.java).setAction(I2pdVpnService.ACTION_START),
            )
        } else {
            store.update { it.copy(vpnEnabled = false) }
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeading(R.string.tab_settings, "04")
        RestartBanner()
        Section(R.string.settings_section_router)
        TextRow(stringResource(R.string.router_basic), stringResource(R.string.router_basic_summary)) { onOpen(SettingsPage.ROUTER_BASIC) }
        TextRow(stringResource(R.string.router_advanced), stringResource(R.string.router_advanced_summary)) { onOpen(SettingsPage.ROUTER_ADVANCED) }
        TextRow(stringResource(R.string.reseed_file_title), stringResource(R.string.reseed_file_summary)) { onOpen(SettingsPage.RESEED_FILE) }
        TextRow(stringResource(R.string.router_raw), stringResource(R.string.router_raw_summary)) { onOpen(SettingsPage.ROUTER_RAW) }

        Section(R.string.settings_section_vpn)
        SwitchRow(R.string.settings_vpn, R.string.settings_vpn_summary, settings.vpnEnabled) { enabled ->
            if (enabled) {
                val current = store.settings.value
                if (!current.vpnGlobal && current.vpnApps.isEmpty()) {
                    Toast.makeText(context, R.string.vpn_choose_first, Toast.LENGTH_LONG).show()
                    onOpen(SettingsPage.VPN_ROUTING)
                } else {
                    val permissionIntent = VpnService.prepare(context)
                    if (permissionIntent == null) {
                        store.update { it.copy(vpnEnabled = true) }
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, I2pdVpnService::class.java).setAction(I2pdVpnService.ACTION_START),
                        )
                    } else {
                        vpnPermissionLauncher.launch(permissionIntent)
                    }
                }
            } else {
                store.update { it.copy(vpnEnabled = false) }
                context.stopService(Intent(context, I2pdVpnService::class.java))
            }
        }
        TextRow(
            stringResource(R.string.vpn_routing_title),
            stringResource(if (settings.vpnGlobal) R.string.vpn_route_all else R.string.vpn_route_selected, settings.vpnApps.size),
        ) { onOpen(SettingsPage.VPN_ROUTING) }

        Section(R.string.settings_section_data)
        TextRow(stringResource(R.string.data_storage), stringResource(R.string.data_storage_summary)) { onOpen(SettingsPage.STORAGE) }
        TextRow(stringResource(R.string.data_files), stringResource(R.string.data_files_summary)) { onOpen(SettingsPage.FILES) }

        Section(R.string.settings_section_start)
        SwitchRow(R.string.settings_autostart, R.string.settings_autostart_summary, settings.autostartOnBoot) { value ->
            store.update { it.copy(autostartOnBoot = value) }
        }
        SwitchRow(R.string.settings_auto_restart, R.string.settings_auto_restart_summary, settings.autoRestart) { value ->
            store.update { it.copy(autoRestart = value) }
        }

        Section(R.string.settings_section_stop)
        TextRow(
            title = stringResource(R.string.settings_graceful_timeout),
            summary = stringResource(R.string.settings_minutes, settings.gracefulTimeoutMinutes),
            onClick = { choosingTimeout = true },
        )

        Section(R.string.settings_section_background)
        TextRow(stringResource(R.string.settings_keep_awake), stringResource(settings.keepAwake.label)) { choosingKeepAwake = true }
        TextRow(
            title = stringResource(R.string.settings_battery),
            summary = stringResource(if (checks.batteryOptimized) R.string.settings_battery_on else R.string.settings_battery_off),
            onClick = if (checks.batteryOptimized) ({ requestIgnoreBatteryOptimizations(context) }) else null,
        )
        OemAutostart.current?.let { oem ->
            TextRow(
                title = stringResource(R.string.settings_oem_autostart, oem.displayName),
                summary = stringResource(R.string.settings_oem_autostart_summary, oem.displayName),
            ) {
                oem.open(context)
                store.update { it.copy(oemAutostartOpened = true) }
            }
        }

        Section(R.string.settings_section_interface)
        TextRow(stringResource(R.string.settings_theme), stringResource(settings.theme.label)) { choosingTheme = true }
        SwitchRow(R.string.settings_secure_screen, R.string.settings_secure_screen_summary, settings.secureScreen) { value ->
            store.update { it.copy(secureScreen = value) }
        }

        Section(R.string.settings_section_about)
        AppHeader(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Text(
            stringResource(R.string.about_versions, BuildConfig.VERSION_NAME, I2pdNative.version(), I2pdNative.abi()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        TextRow(stringResource(R.string.about_website), WEBSITE_URL) { openUrl(context, WEBSITE_URL) }
        TextRow(stringResource(R.string.about_source), SOURCE_URL) { openUrl(context, SOURCE_URL) }
        TextRow(stringResource(R.string.about_issues), ISSUES_URL) { openUrl(context, ISSUES_URL) }
        TextRow(stringResource(R.string.about_licenses), stringResource(R.string.about_licenses_summary)) { onOpen(SettingsPage.LICENSES) }
    }

    if (choosingTimeout) {
        ChoiceDialog(
            title = R.string.settings_graceful_timeout,
            choices = AppSettingsStore.GRACEFUL_TIMEOUT_CHOICES,
            selected = settings.gracefulTimeoutMinutes,
            label = { stringResource(R.string.settings_minutes, it) },
            onSelect = { minutes -> store.update { it.copy(gracefulTimeoutMinutes = minutes) } },
            onDismiss = { choosingTimeout = false },
        )
    }
    if (choosingKeepAwake) {
        ChoiceDialog(
            title = R.string.settings_keep_awake,
            description = stringResource(R.string.settings_keep_awake_summary),
            choices = KeepAwake.entries,
            selected = settings.keepAwake,
            label = { stringResource(it.label) },
            onSelect = { mode -> store.update { it.copy(keepAwake = mode) } },
            onDismiss = { choosingKeepAwake = false },
        )
    }
    if (choosingTheme) {
        ChoiceDialog(
            title = R.string.settings_theme,
            choices = ThemeMode.entries,
            selected = settings.theme,
            label = { stringResource(it.label) },
            onSelect = { theme -> store.update { it.copy(theme = theme) } },
            onDismiss = { choosingTheme = false },
        )
    }
}

private data class VpnAppItem(val packageName: String, val label: String)

@Composable
private fun VpnRoutingScreen(store: AppSettingsStore, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by store.settings.collectAsStateWithLifecycle()
    val apps = remember(context) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { VpnAppItem(it.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
    val reconfigure = {
        if (store.settings.value.vpnEnabled) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, I2pdVpnService::class.java).setAction(I2pdVpnService.ACTION_RECONFIGURE),
            )
        }
    }
    Column(modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.vpn_routing_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Section(R.string.vpn_routing_mode)
        Column(Modifier.selectableGroup()) {
            VpnModeRow(R.string.vpn_mode_all, R.string.vpn_mode_all_summary, settings.vpnGlobal) {
                store.update { it.copy(vpnGlobal = true) }
                reconfigure()
            }
            VpnModeRow(R.string.vpn_mode_selected, R.string.vpn_mode_selected_summary, !settings.vpnGlobal) {
                if (settings.vpnEnabled && settings.vpnApps.isEmpty()) {
                    store.update { it.copy(vpnGlobal = false, vpnEnabled = false) }
                    context.stopService(Intent(context, I2pdVpnService::class.java))
                    Toast.makeText(context, R.string.vpn_choose_first, Toast.LENGTH_LONG).show()
                } else {
                    store.update { it.copy(vpnGlobal = false) }
                    reconfigure()
                }
            }
        }
        if (!settings.vpnGlobal) {
            Section(R.string.vpn_select_apps)
            Text(
                pluralStringResource(R.plurals.vpn_app_count, settings.vpnApps.size, settings.vpnApps.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(apps, key = { it.packageName }) { app ->
                    val checked = app.packageName in settings.vpnApps
                    VpnAppRow(app, checked) { selected ->
                        val newApps = if (selected) settings.vpnApps + app.packageName else settings.vpnApps - app.packageName
                        val stopVpn = settings.vpnEnabled && !selected && newApps.isEmpty()
                        store.update { it.copy(vpnApps = newApps, vpnEnabled = if (stopVpn) false else it.vpnEnabled) }
                        if (stopVpn) context.stopService(Intent(context, I2pdVpnService::class.java)) else reconfigure()
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                }
                if (apps.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.vpn_no_apps),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VpnModeRow(title: Int, summary: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VpnAppRow(app: VpnAppItem, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
    }
}

private val KeepAwake.label: Int
    get() = when (this) {
        KeepAwake.OFF -> R.string.settings_keep_awake_off
        KeepAwake.ALWAYS -> R.string.settings_keep_awake_always
        KeepAwake.WHILE_CHARGING -> R.string.settings_keep_awake_charging
    }

private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@Composable
private fun <T> ChoiceDialog(
    title: Int,
    description: String? = null,
    choices: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                choices.forEach { choice ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = choice == selected, role = Role.RadioButton) {
                            onSelect(choice)
                            onDismiss()
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == selected, onClick = null)
                        Text(label(choice), Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun Section(title: Int) {
    HorizontalDivider(thickness = 0.75.dp, color = MaterialTheme.colorScheme.outline)
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 0.45.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 5.dp).semantics { heading() },
    )
}

@Composable
private fun SwitchRow(title: Int, summary: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun TextRow(title: String, summary: String, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        Modifier.fillMaxWidth().then(clickable).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp).clearAndSetSemantics { },
            )
        }
    }
}
