package com.santhosh.fixture.urlshortener;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortUrlServiceTest {
    private final ShortUrlService service = new ShortUrlService();

    @Test
    void shortensAndResolvesAbsoluteHttpsUrl() {
        URI target = URI.create("https://example.com/articles/agentic-engineering");
        String code = service.shorten(target);
        assertThat(code).isNotBlank();
        assertThat(service.resolve(code)).contains(target);
    }

    @Test
    void rejectsUnsupportedSchemes() {
        assertThatThrownBy(() -> service.shorten(URI.create("file:///secret")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
