package UrlShortener.controller;

import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import UrlShortener.service.AdminService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @PatchMapping("/users/{username}/role")
    public ResponseEntity<Map<String, String>> updateUserRole(
            @PathVariable String username,
            @RequestBody RoleUpdateRequest request) {
        log.info("Role update requested for user '{}' -> '{}'", username, request.getRole());
        adminService.updateUserRole(username, request.getRole());
        return ResponseEntity.ok(Collections.singletonMap("message", "Role updated successfully"));
    }
}
