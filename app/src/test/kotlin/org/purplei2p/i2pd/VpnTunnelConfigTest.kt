package org.purplei2p.i2pd

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnTunnelConfigTest {
    @Test
    fun `mapped DNS uses the documented shared address pool and SOCKS target`() {
        val config = VpnTunnelConfig.render("127.0.0.1", 4447)

        assertTrue(config.contains("address: ${VpnTunnelConfig.MAPPED_DNS}"))
        assertTrue(config.contains("network: 100.64.0.0"))
        assertTrue(config.contains("netmask: 255.192.0.0"))
        assertTrue(config.contains("address: '127.0.0.1'"))
        assertTrue(config.contains("port: 4447"))
        assertFalse(config.contains("240.0.0.0"))
    }
}
