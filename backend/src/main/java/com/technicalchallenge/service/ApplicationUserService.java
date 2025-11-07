package com.technicalchallenge.service;

import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.repository.ApplicationUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApplicationUserService {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationUserService.class);
    private final ApplicationUserRepository applicationUserRepository;
    // Inject the application's PasswordEncoder so credential checks use the same
    // encoder that Spring Security is configured with. This allows stored values
    // like "{noop}password" (used in dev seed data) or stronger hashes (bcrypt)
    // to be verified via passwordEncoder.matches(plain, encoded).
    private final PasswordEncoder passwordEncoder;

    public boolean validateCredentials(String loginId, String password) {
        logger.debug("Validating credentials for user: {}", loginId);
        // refactored to accept either loginId or first name. Avoids type mismatch
        // errors.Returns the first matching user
        Optional<ApplicationUser> user = applicationUserRepository.findAll().stream()
                .filter(u -> loginId.equals(u.getLoginId()) || loginId.equals(u.getFirstName()))
                .findFirst();
        // Use PasswordEncoder.matches to support encoded stored passwords.
        // (This is the important change: previously a plain-string compare would
        // fail when passwords in the DB are encoded or prefixed with an encoding
        // id like {noop}.)
        return user.map(applicationUser -> {
            String stored = applicationUser.getPassword();
            if (stored == null)
                return false;
            try {
                // Compare plain password with stored (possibly encoded) password.
                boolean matches = passwordEncoder.matches(password, stored);

                // log a masked representation of the stored value and the
                // match result to help debug authentication failures during
                // development. The full encoded password is never logged.
                String masked = stored.length() <= 6 ? "***" : stored.substring(0, 4) + "***";
                logger.debug("Password match check for user '{}': matches={} stored={}", loginId, matches, masked);
                return matches;
            } catch (Exception e) {
                // If the PasswordEncoder throws (malformed value, unknown id,
                // etc.) fall back to a plain equals check. This fallback is
                // only intended to assist with legacy seed.
                boolean eq = stored.equals(password);
                logger.debug("Password encoder failed for user '{}', falling back to plain equals: {}", loginId, eq);
                return eq;
            }
        }).orElse(false);
    }

    public List<ApplicationUser> getAllUsers() {
        logger.info("Retrieving all users");
        return applicationUserRepository.findAll();
    }

    public Optional<ApplicationUser> getUserById(Long id) {
        logger.debug("Retrieving user by id: {}", id);
        return applicationUserRepository.findById(id);
    }

    public Optional<ApplicationUser> getUserByLoginId(String loginId) {
        logger.debug("Retrieving user by login id: {}", loginId);
        return applicationUserRepository.findByLoginId(loginId);
    }

    public ApplicationUser saveUser(ApplicationUser user) {
        logger.info("Saving user: {}", user);
        return applicationUserRepository.save(user);
    }

    public void deleteUser(Long id) {
        logger.warn("Deleting user with id: {}", id);
        applicationUserRepository.deleteById(id);
    }

    public ApplicationUser updateUser(Long id, ApplicationUser user) {
        logger.info("Updating user with id: {}", id);
        ApplicationUser existingUser = applicationUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        // Update fields
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setLoginId(user.getLoginId());
        existingUser.setActive(user.isActive());
        existingUser.setUserProfile(user.getUserProfile());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existingUser.setPassword(user.getPassword());
        }
        // version and lastModifiedTimestamp handled by entity listeners
        return applicationUserRepository.save(existingUser);
    }
}
