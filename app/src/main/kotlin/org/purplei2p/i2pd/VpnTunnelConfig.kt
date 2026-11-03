package org.purplei2p.i2pd

/** Builds the local Hev tunnel configuration used to forward Android VPN traffic to i2pd SOCKS. */
internal object VpnTunnelConfig {
    const val MTU = 1500
    const val IPV4 = "198.18.0.1"
    const val IPV6 = "fd00:198:18::1"
    const val MAPPED_DNS = "198.18.0.2"

    // Match HevSocks5Tunnel's documented mapdns example. Do not use 240/4 (reserved IPv4 space)
    // for synthetic DNS answers: Android resolvers and apps may reject or mishandle those answers.
    private const val MAPPED_NETWORK = "100.64.0.0"
    private const val MAPPED_NETMASK = "255.192.0.0"

    fun render(socksAddress: String, socksPort: Int): String {
        val safeAddress = socksAddress.replace("'", "")
        return """tunnel:
  name: i2pd0
  mtu: $MTU
  ipv4: $IPV4
  ipv6: '$IPV6'
  icmp: 'off'
socks5:
  address: '$safeAddress'
  port: $socksPort
  udp: 'udp'
mapdns:
  address: $MAPPED_DNS
  port: 53
  network: $MAPPED_NETWORK
  netmask: $MAPPED_NETMASK
  cache-size: 10000
"""
    }
}
