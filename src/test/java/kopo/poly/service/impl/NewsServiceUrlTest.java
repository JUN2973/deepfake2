package kopo.poly.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NewsServiceUrlTest {

    @Test
    void keepsHttpAndHttpsUrls() {
        assertEquals("https://news.example.com/a?b=1", NewsService.safeHttpUrl(" https://news.example.com/a?b=1 "));
        assertEquals("http://news.example.com/a", NewsService.safeHttpUrl("http://news.example.com/a"));
        assertEquals("https://news.example.com/A", NewsService.safeHttpUrl("HTTPS://news.example.com/A"));
    }

    @Test
    void dropsScriptAndMalformedUrls() {
        assertEquals("", NewsService.safeHttpUrl("javascript:alert(document.domain)"));
        assertEquals("", NewsService.safeHttpUrl("JavaScript:alert(1)"));
        assertEquals("", NewsService.safeHttpUrl("data:text/html,<script>alert(1)</script>"));
        assertEquals("", NewsService.safeHttpUrl("//evil.example.com"));
        assertEquals("", NewsService.safeHttpUrl("http://bad host"));
        assertEquals("", NewsService.safeHttpUrl(null));
    }
}
