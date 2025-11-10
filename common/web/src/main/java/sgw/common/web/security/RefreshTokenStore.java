package sgw.common.web.security;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory storage for refresh tokens.
 */
@Component
public class RefreshTokenStore {

    private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();

    /**
     * Save refresh token for a user
     * @param username the username
     * @param refreshToken the refresh token
     */
    public void saveRefreshToken(String username, String refreshToken) {
        tokenStore.put(username, refreshToken);
    }

    /**
     * Get refresh token for a user
     * @param username the username
     * @return the refresh token, or null if not found
     */
    public String getRefreshToken(String username) {
        return tokenStore.get(username);
    }

    /**
     * Delete refresh token for a user
     * @param username the username
     */
    public void deleteRefreshToken(String username) {
        tokenStore.remove(username);
    }

    /**
     * Validate that the token matches the stored token
     * @param username the username
     * @param refreshToken the refresh token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateRefreshToken(String username, String refreshToken) {
        String storedToken = tokenStore.get(username);
        return storedToken != null && storedToken.equals(refreshToken);
    }
}

