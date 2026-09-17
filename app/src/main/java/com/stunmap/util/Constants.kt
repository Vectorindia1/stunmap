package com.stunmap.util

object Constants {
    const val STUN_MAGIC_COOKIE = 0x2112A442L

    const val ATTR_XOR_MAPPED_ADDRESS = 0x0020
    const val ATTR_MAPPED_ADDRESS = 0x0001
    const val ATTR_SOFTWARE = 0x8022
    const val ATTR_USERNAME = 0x0006
    const val ATTR_MESSAGE_INTEGRITY = 0x0008
    const val ATTR_FINGERPRINT = 0x8028

    const val MSG_BINDING_REQUEST = 0x0001
    const val MSG_BINDING_RESPONSE = 0x0101
    const val MSG_BINDING_ERROR = 0x0111

    const val STUN_HEADER_SIZE = 20
    const val UDP_HEADER_SIZE = 8

    const val IPINFO_BASE_URL = "https://ipinfo.io"
    const val SELF_IP_URL = "https://ipinfo.io/ip"

    const val META_ASN_UPDATE_URL =
        "https://raw.githubusercontent.com/veenu-infosec/stunmap-assets/main/meta_asn_cidr.txt"

    const val META_ASN_ASSET_FILE = "meta_asn_cidr.txt"
    const val GEOIP_DB_ASSET_FILE = "GeoLite2-City.mmdb"

    const val NOTIFICATION_CHANNEL_ID = "stunmap_capture"
    const val NOTIFICATION_ID = 1001

    const val SELF_IP_TIMEOUT_MS = 5000L
    const val GEOIP_TIMEOUT_MS = 10000L
}
