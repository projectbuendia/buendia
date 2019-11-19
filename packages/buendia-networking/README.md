# buendia-networking

The `buendia-networking` package is intended to provide basic network
configuration for a physical site installation.

The reference networking stack consists of an Intel NUC, communicating over
Wi-Fi to a Ubiquiti NanoStation M2, but of course the Buendia platform should
be flexible enough to be readily configured for any reasonable combination of
Linux computing and networking hardware.

The `networking` package assumes that the server hardware has two network
connections: one _wireless_, the other _wired_.

**Currently, the primary assumption is that Buendia server will communicate
with clients over a Wi-Fi network hosted by a dedicated access point.**

DHCP and DNS can be provided either by the wireless AP or by the Buendia server.

## Network design

Our reference network design makes two assumptions:

1. Using a Nanostation as the network router will provide better wireless
   connectivity to clients than using the internal Wi-Fi card on a NUC in
   combination with `hostapd`.
2. The NUC should provide DNS, so that clients can be configured simply to
   connect to the `buendia` hostname on the local network.

Therefore the site should be configured with a file in e.g.
`/usr/share/buendia/site/70-networking-nuc` as follows

```
# Join the existing "buendia" network as provided by the Nanostation
NETWORKING_AP=0
NETWORKING_SSID=buendia
NETWORKING_PASSWORD=password

# Get a static address on the network and provide DNS but not DHCP.
NETWORKING_WIFI_ADDRESS=10.0.0.2
NETWORKING_DHCP_DNS_SERVER=1
NETWORKING_DHCP_RANGE=
```

The Nanostation would then be configured as `10.0.0.1` on the WLAN and deliver
DHCP on the `10.0.0.x` network, with the DNS address hardcoded to `10.0.0.2`.
(Alternately, the DHCP server could be disabled on the Nanostation and enabled
on the NUC as documented below.)

The Nanostation Ethernet address can be left set to the default `192.168.1.20`
to permit mainenance access.

**Critical note**: The default settings on the Nanostation make it impossible
to connect to the Wi-Fi network from some devices. You _must_ make the
following changes from the factory default settings.

1. Disable "AirMAX"
2. Set channel width to **20 MHz**
3. Select a specific Wi-Fi channel -- don't use auto

The Ubiquiti user forums hint at this a bit, and [this blog
post](https://www.stevejenkins.com/blog/2013/07/connecting-ios-devices-iphone-ipad-ipod-to-ubiquiti-nanostation-m5-on-5ghz-channels/)
contains some additional detail.

## Site settings

### `NETWORKING_WIFI_INTERFACE`

The Linux device name for the wireless interface.

### `NETWORKING_SSID`

The SSID for the wireless network. Required if `NETWORKING_WIFI_INTERFACE` is set.

### `NETWORKING_PASSWORD`

The WPA2 password for the wireless network. Required if `NETWORKING_WIFI_INTERFACE` is set.

### `NETWORKING_WIFI_ADDRESS`

The IP address for the Wi-Fi interface. If this is left blank, then the
deprecated setting `NETWORKING_IP_ADDRESS` is used. If both are blank, then the
Wi-Fi device is configured to use DHCP to find an IP address.

### `NETWORKING_ETHERNET_INTERFACE`

The Linux device name for the Ethernet interface.

### `NETWORKING_ETHERNET_ADDRESS`

The IP address for the Ethernet interface. If this is left blank, then the
deprecated setting `NETWORKING_IP_ADDRESS` is used. If both are blank, then the
Ethernet device is configured to use DHCP to find an IP address.

### `NETWORKING_DHCP_DNS_SERVER`

A boolean setting determining whether or not the Buendia server provides DNS
and/or DHCP on its subnet. Defaults to off. If this is set, then
`NETWORKING_IP_ADDRESS` is required.

### `NETWORKING_DHCP_RANGE`

The DHCP assignment range passed to dnsmasq. Must be in the form `<start IP>,
<end IP>, <lease time>`, e.g. `192.168.10.10,192.168.10.50,12h`. If left unset,
then the Buendia server will not provide DHCP. Ignored if
`NETWORKING_DHCP_DNS_SERVER` is unset.

### `NETWORKING_IP_ADDRESS` **(deprecated)**

The static IP address for the "main" network interface, if
`NETWORKING_WIFI_ADDRESS` or `NETWORKING_ETHERNET_ADDRESS` is left unset.

This setting is deprecated and is here only to support older installations. It
will probably be removed in a future version of the package.
