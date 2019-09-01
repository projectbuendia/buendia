# buendia-networking

The `buendia-networking` package is intended to provide basic network
configuration for a physical site installation.

The reference networking stack consists of an Intel NUC, communicating over
Wi-Fi to a Ubiquiti NanoStation, but of course the Buendia platform should be
flexible enough to be readily configured for any reasonable combination of
Linux computing and networking hardware.

The `networking` package assumes that the server hardware has two network connections: one _wireless_, the other _wired_.

**Currently, the primary assumption is that Buendia server will communicate with clients
over a Wi-Fi network hosted by a dedicated access point.**

DHCP and DNS can be provided either by the wireless AP or by the Buendia server.



## Site settings

### `NETWORKING_ETHERNET_INTERFACE` (required)

The Linux device name for the ethernet interface. Currently, this interface is always configured to request an address over DHCP.

### `NETWORKING_WIFI_INTERFACE` (required)

The Linux device name for the wireless interface.

### `NETWORKING_SSID` (required)

The SSID for the wireless network.

### `NETWORKING_PASSWORD` (required)

The WPA2 password for the wireless network.

### `NETWORKING_IP_ADDRESS` (optional)

The static IP address for the `NETWORKING_WIFI_INTERFACE` device. If set, the netmask is assumed to be 24 bits.

If this setting is left blank, then the Wi-Fi interface is configured to request an address via DHCP.

### `NETWORKING_DHCP_DNS_SERVER` (optional)

A boolean setting determining whether or not the Buendia server provides DNS and/or DHCP on its subnet. Defaults to off. If this is set, then `NETWORKING_IP_ADDRESS` is required.

### `NETWORKING_DHCP_RANGE` (optional)

The DHCP assignment range passed to dnsmasq. Must be in the form `<start IP>, <end IP>, <lease time>`, e.g. `192.168.10.10,192.168.10.50,12h`. If left unset, then the Buendia server will not provide DHCP. Ignored if `NETWORKING_DHCP_DNS_SERVER` is unset.
