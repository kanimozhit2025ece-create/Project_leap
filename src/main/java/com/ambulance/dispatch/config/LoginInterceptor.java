
package com.ambulance.dispatch.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {

        HttpSession session = request.getSession(false);

        if (session == null ||
                session.getAttribute("userId") == null) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Please login"
            );
            return false;
        }

        String role = (String) session.getAttribute("role");
        String path = request.getRequestURI();
        String method = request.getMethod();

        // ADMIN: Full access
        if ("ADMIN".equals(role)) {
            return true;
        }

        // STAFF: View ambulances, zones and emergency calls
        if ("STAFF".equals(role)) {

            if ("GET".equals(method) &&
                    (path.startsWith("/ambulance") ||
                            path.startsWith("/zone") ||
                            path.startsWith("/emergency"))) {
                return true;
            }

            // Update ambulance location
            if ("PUT".equals(method) &&
                    path.matches(
                            "/ambulance/[0-9]+/location/[0-9]+"
                    )) {
                return true;
            }

            // Mark emergency arrived or completed
            if ("PUT".equals(method) &&
                    (path.matches(
                            "/emergency/[0-9]+/arrived"
                    ) ||
                            path.matches(
                                    "/emergency/[0-9]+/complete"
                            ))) {
                return true;
            }
        }

        if ("PUBLIC".equals(role)) {

            // View zones.
            if ("GET".equals(method) &&
                    "/zone".equals(path)) {
                return true;
            }

            // Register emergency.
            if ("POST".equals(method) &&
                    path.matches("/emergency/[0-9]+")) {
                return true;
            }

            // View only own emergency requests.
            if ("GET".equals(method) &&
                    "/emergency/my".equals(path)) {
                return true;
            }
        }

        response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "You do not have permission for this action"
        );

        return false;
    }
}