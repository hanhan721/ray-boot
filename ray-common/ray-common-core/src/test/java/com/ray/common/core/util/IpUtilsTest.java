package com.ray.common.core.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IpUtilsTest {

    @Test
    void appendedClientAddressOverridesForgedPrefix() {
        var request = request("10.0.0.5", "203.0.113.123, 198.51.100.7");

        assertEquals("198.51.100.7", IpUtils.resolveClientIp(request, proxies("10.0.0.5")));
    }

    @Test
    void onlyExplicitProxiesCanSupplyForwardingHeaders() {
        var request = request("192.168.1.20", "203.0.113.123");

        assertEquals("192.168.1.20", IpUtils.resolveClientIp(request, proxies("10.0.0.5")));
    }

    @Test
    void stripsMultipleTrustedProxiesFromRightToLeft() {
        var request = request("10.0.0.5", "203.0.113.123, 198.51.100.7, 10.0.0.6");

        assertEquals("198.51.100.7", IpUtils.resolveClientIp(request, proxies("10.0.0.5", "10.0.0.6")));
    }

    @Test
    void malformedNearestHopFallsBackToConnectionAddress() {
        var request = request("10.0.0.5", "198.51.100.7, unknown");

        assertEquals("10.0.0.5", IpUtils.resolveClientIp(request, proxies("10.0.0.5")));
    }

    @Test
    void ipv6ProxyAddressesAreComparedByAddress() {
        var request = request("0:0:0:0:0:0:0:1", "2001:db8::7");

        assertEquals(InetAddress.ofLiteral("2001:db8::7").getHostAddress(),
                IpUtils.resolveClientIp(request, proxies("::1")));
    }

    private HttpServletRequest request(String remote, String forwardedFor) {
        var request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(remote);
        when(request.getHeader("X-Forwarded-For")).thenReturn(forwardedFor);
        return request;
    }

    private Set<InetAddress> proxies(String... values) {
        return java.util.Arrays.stream(values).map(InetAddress::ofLiteral)
                .collect(java.util.stream.Collectors.toSet());
    }
}
