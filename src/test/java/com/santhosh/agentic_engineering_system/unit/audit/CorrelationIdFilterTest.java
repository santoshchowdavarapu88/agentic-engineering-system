package com.santhosh.agentic_engineering_system.unit.audit;

import com.santhosh.agentic_engineering_system.audit.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void acceptsSafeCallerCorrelationAndClearsThreadContext() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "review-run-123");
        var response = new MockHttpServletResponse();
        FilterChain chain = (ignoredRequest, ignoredResponse) ->
                assertThat(MDC.get("correlationId")).isEqualTo("review-run-123");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo("review-run-123");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void replacesUnsafeCorrelationValue() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "invalid value with spaces");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isNotBlank().doesNotContain(" ");
    }
}
