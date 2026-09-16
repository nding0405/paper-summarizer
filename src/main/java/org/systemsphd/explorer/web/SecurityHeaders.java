package org.systemsphd.explorer.web;

import jakarta.servlet.http.HttpServletResponse;

/** Baseline response headers shared by every page. */
final class SecurityHeaders {
    private SecurityHeaders() {
    }

    static void apply(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }
}
