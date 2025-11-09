package sgw.app.api.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sgw.common.web.security.JwtTokenProvider;
import sgw.common.web.security.RefreshTokenStore;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    /**
     * Login endpoint
     * For demo purposes, accepts any username/password
     * In production, validate against a user database
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // TODO: Validate credentials against database
        // For now, accept any credentials for testing
        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(createErrorResponse("Username is required"));
        }

        String accessToken = jwtTokenProvider.generateAccessToken(username);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        // Store refresh token
        refreshTokenStore.saveRefreshToken(username, refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("username", username);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh token endpoint
     * Generates new access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestHeader("Authorization") String authHeader) {
        try {
            String refreshToken = extractToken(authHeader);

            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(401).body(createErrorResponse("Invalid refresh token"));
            }

            String tokenType = jwtTokenProvider.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                return ResponseEntity.status(401).body(createErrorResponse("Token is not a refresh token"));
            }

            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

            // Validate stored refresh token
            if (!refreshTokenStore.validateRefreshToken(username, refreshToken)) {
                return ResponseEntity.status(401).body(createErrorResponse("Refresh token not found or expired"));
            }

            // Generate new access token
            String newAccessToken = jwtTokenProvider.generateAccessToken(username);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("tokenType", "Bearer");
            response.put("username", username);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(createErrorResponse("Invalid token: " + e.getMessage()));
        }
    }

    /**
     * Logout endpoint
     * Invalidates refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            String username = jwtTokenProvider.getUsernameFromToken(token);

            // Remove refresh token
            refreshTokenStore.deleteRefreshToken(username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logged out successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(createErrorResponse("Logout failed: " + e.getMessage()));
        }
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}

