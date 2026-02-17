package com.app.controller;

import com.app.config.JwtUtil;
import com.app.persistence.entity.LoginCreds;
import com.app.persistence.entity.UserEntity;
import com.app.persistence.repository.UserRepository;
import com.app.service.RedisLoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@PreAuthorize("denyAll()")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisLoginAttemptService loginAttemptService;

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    public Map<String, Object> registerHandler(
            @RequestBody LoginCreds creds) {
        UserEntity user = new UserEntity();
        user.setUsername(creds.getUsername());
        String encodedPassword = passwordEncoder.encode(creds.getPassword());
        user.setPassword(encodedPassword);
        user.setEnabled(true);
        user.setAccountNoExpired(true);
        user.setAccountNoLocked(true);
        user.setCredentialNoExpired(true);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername());
        return Map.of("token", "Bearer " + token);
    }


    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> loginHandler(
            @RequestBody LoginCreds loginCreds) {

        String username = loginCreds.getUsername();

        // Revisar si el usuario está bloqueado
        if (loginAttemptService.isBlocked(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message",
                            "Account locked. Try again later.")
                    );
        }

        try {
            // Intentar autenticar
            UsernamePasswordAuthenticationToken authInputToken =
                    new UsernamePasswordAuthenticationToken(
                            username, loginCreds.getPassword()
                    );

            authenticationManager.authenticate(authInputToken);

            // Login exitoso, entonces limpiar contador
            loginAttemptService.loginSucceeded(username);

            // Generar JWT
            String token = jwtUtil.generateToken(username);

            return ResponseEntity.ok(
                    Map.of("token", "Bearer " + token)
            );

        } catch (AuthenticationException authExc) {
            // Login fallido, entonces incrementar contador
            loginAttemptService.loginFailed(username);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials."));
        }
    }
}
