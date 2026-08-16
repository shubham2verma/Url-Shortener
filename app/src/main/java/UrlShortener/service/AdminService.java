package UrlShortener.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import UrlShortener.exception.InvalidRoleException;
import UrlShortener.exception.UserNotFoundException;
import UrlShortener.model.User;
import UrlShortener.repository.UserRepository;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private static final Set<String> ALLOWED_ROLES = new HashSet<>(
            Arrays.asList("ROLE_READ", "ROLE_WRITE", "ROLE_ADMIN"));

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateUserRole(String username, String newRole) {
        if (!ALLOWED_ROLES.contains(newRole)) {
            throw new InvalidRoleException(newRole);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role updated for user '{}': {}", username, newRole);
    }
}
