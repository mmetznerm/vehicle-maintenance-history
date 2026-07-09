package com.mmetzner.vmh.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTests {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void shouldUseIncomingRequestIdAndAddItToResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/vehicles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "mobile-request-123");

        AtomicReference<String> mdcRequestId = new AtomicReference<>();

        FilterChain filterChain = (servletRequest, servletResponse) ->
                mdcRequestId.set(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER))
                .isEqualTo("mobile-request-123");

        assertThat(mdcRequestId.get())
                .isEqualTo("mobile-request-123");

        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY))
                .isNull();
    }

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/vehicles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER))
                .isNotBlank();

        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY))
                .isNull();
    }
}