package org.purplei2p.i2pd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import hev.htproxy.TProxyService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.purplei2p.i2pd.config.ConfigEditor
import org.purplei2p.i2pd.service.AppSettingsStore
import org.purplei2p.i2pd.service.Lifecycle
import org.purplei2p.i2pd.service.RouterController
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/** Device VPN front end; hev-socks5-tunnel carries the TUN streams to i2pd's local SOCKS proxy. */
class I2pdVpnService : VpnService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val tunnelMutex = Mutex()
    private var tunnelDescriptor: ParcelFileDescriptor? = null
    private var connectionJob: Job? = null
    private var generation = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            generation++
            connectionJob?.cancel()
            scope.launch {
                stopTunnel()
                AppSettingsStore.get(this@I2pdVpnService).update { it.copy(vpnEnabled = false) }
                stopForegroundCompat()
                stopSelfResult(startId)
            }
            return START_NOT_STICKY
        }

        startForegroundCompat()
        val token = ++generation
        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                connect(token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "Could not start i2pd VPN", error)
                stopTunnel()
                if (token == generation) {
                    AppSettingsStore.get(this@I2pdVpnService).update { it.copy(vpnEnabled = false) }
                    Toast.makeText(this@I2pdVpnService, R.string.vpn_start_failed, Toast.LENGTH_LONG).show()
                    stopForegroundCompat()
                    stopSelfResult(startId)
                }
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        generation++
        connectionJob?.cancel()
        AppSettingsStore.get(this).update { it.copy(vpnEnabled = false) }
        scope.launch {
            stopTunnel()
            AppSettingsStore.get(this@I2pdVpnService).update { it.copy(vpnEnabled = false) }
            stopForegroundCompat()
            stopSelf()
        }
        super.onRevoke()
    }

    override fun onDestroy() {
        generation++
        connectionJob?.cancel()
        scope.cancel()
        stopNativeAndClose()
        super.onDestroy()
    }

    private suspend fun connect(token: Int) = tunnelMutex.withLock {
        withContext(Dispatchers.IO) { stopNativeAndClose() }
        val settings = AppSettingsStore.get(this).settings.value
        if (!settings.vpnEnabled || token != generation) return@withLock
        if (!settings.vpnGlobal && settings.vpnApps.isEmpty()) {
            throw IllegalStateException("Select at least one app for per-app VPN mode")
        }
        if (VpnService.prepare(this) != null) throw IllegalStateException("VPN permission was not granted")

        // Read the user's live configuration rather than assuming the default SOCKS port.
        val socks = RouterController.config(this).load()
        if (!ConfigEditor.parseBool(socks.effectiveValue("socksproxy.enabled"))) {
            throw IllegalStateException("The i2pd SOCKS proxy is disabled")
        }
        val socksPort = socks.effectiveValue("socksproxy.port")?.toIntOrNull()
            ?: throw IllegalStateException("Invalid i2pd SOCKS port")
        val configuredAddress = socks.effectiveValue("socksproxy.address")?.trim().orEmpty()
        val socksAddress = when (configuredAddress) {
            "", "0.0.0.0", "::", "[::]" -> "127.0.0.1"
            else -> configuredAddress.removePrefix("[").removeSuffix("]")
        }

        RouterController.start(this)
        val routerState = withTimeout(ROUTER_START_TIMEOUT_MS) {
            RouterController.state(this@I2pdVpnService).first {
                it.lifecycle == Lifecycle.Running || it.lifecycle is Lifecycle.Failed
            }
        }
        if (routerState.lifecycle !is Lifecycle.Running) {
            throw IllegalStateException("i2pd did not start: ${(routerState.lifecycle as? Lifecycle.Failed)?.reason}")
        }
        awaitSocks(socksAddress, socksPort)
        if (token != generation || !AppSettingsStore.get(this).settings.value.vpnEnabled) return@withLock

        val builder = Builder()
            .setSession(getString(R.string.vpn_session))
            .setBlocking(false)
            .setMtu(VpnTunnelConfig.MTU)
            .addAddress(VpnTunnelConfig.IPV4, 15)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(VpnTunnelConfig.MAPPED_DNS)
            .addAddress(VpnTunnelConfig.IPV6, 64)
            .addRoute("::", 0)

        if (settings.vpnGlobal) {
            // The app's own router and SOCKS sockets must stay outside this VPN to avoid a loop.
            builder.addDisallowedApplication(packageName)
        } else {
            var added = 0
            for (appPackage in settings.vpnApps.filterNot { it == packageName }) {
                try {
                    packageManager.getApplicationInfo(appPackage, 0)
                    builder.addAllowedApplication(appPackage)
                    added++
                } catch (_: PackageManager.NameNotFoundException) {
                    // An app may have been removed since the user selected it.
                }
            }
            if (added == 0) throw IllegalStateException("No selected apps are installed")
        }

        val descriptor = builder.establish() ?: throw IllegalStateException("Android did not create the VPN interface")
        val configFile = writeTunnelConfig(socksAddress, socksPort)
        val started = try {
            TProxyService.TProxyStartService(configFile.absolutePath, descriptor.fd)
        } catch (error: Throwable) {
            descriptor.close()
            throw error
        }
        if (!started) {
            descriptor.close()
            throw IllegalStateException("tun2socks could not start")
        }
        tunnelDescriptor = descriptor
    }

    private suspend fun awaitSocks(address: String, port: Int) = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + SOCKS_WAIT_TIMEOUT_MS
        var lastError: Exception? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(address, port), SOCKS_CONNECT_TIMEOUT_MS)
                }
                return@withContext
            } catch (error: Exception) {
                lastError = error
                kotlinx.coroutines.delay(SOCKS_RETRY_DELAY_MS)
            }
        }
        throw IllegalStateException("The local i2pd SOCKS proxy did not become ready", lastError)
    }

    private fun writeTunnelConfig(address: String, port: Int): File {
        val configFile = File(filesDir, "i2pd-vpn-tun.yml")
        configFile.writeText(VpnTunnelConfig.render(address, port))
        return configFile
    }

    private suspend fun stopTunnel() = tunnelMutex.withLock {
        withContext(Dispatchers.IO) { stopNativeAndClose() }
    }

    private fun stopNativeAndClose() {
        runCatching {
            if (TProxyService.TProxyIsRunning()) TProxyService.TProxyStopService()
        }.onFailure { Log.w(TAG, "Could not stop tun2socks cleanly", it) }
        runCatching { tunnelDescriptor?.close() }
            .onFailure { Log.w(TAG, "Could not close VPN interface", it) }
        tunnelDescriptor = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.vpn_notification_channel), NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    private fun startForegroundCompat() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(org.purplei2p.i2pd.service.R.drawable.ic_notification_icon)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val ACTION_START = "org.purplei2p.i2pd.vpn.START"
        const val ACTION_RECONFIGURE = "org.purplei2p.i2pd.vpn.RECONFIGURE"
        const val ACTION_STOP = "org.purplei2p.i2pd.vpn.STOP"
        private const val TAG = "i2pd-vpn"
        private const val CHANNEL_ID = "i2pd-vpn"
        private const val NOTIFICATION_ID = 0x12d
        private const val ROUTER_START_TIMEOUT_MS = 120_000L
        private const val SOCKS_WAIT_TIMEOUT_MS = 20_000L
        private const val SOCKS_CONNECT_TIMEOUT_MS = 1_000
        private const val SOCKS_RETRY_DELAY_MS = 500L
    }
}
