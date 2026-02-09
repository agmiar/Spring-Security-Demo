package com.app.controller;

import com.app.config.JwtUtil;
import com.app.persistence.entity.LoginCreds;
import com.app.persistence.entity.UserEntity;
import com.app.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/login")
    @PreAuthorize("permitAll()")
    public Map<String,Object> loginHandler(
            @RequestBody LoginCreds loginCreds){
        try{
            UsernamePasswordAuthenticationToken authInputToken =
                    new UsernamePasswordAuthenticationToken(
                            loginCreds.getUsername(), loginCreds.getPassword()
                    );
            authenticationManager.authenticate(authInputToken);
            String token =
                    jwtUtil.generateToken(loginCreds.getUsername());

            return Collections.singletonMap("token", "Bearer " + token);
        }
        catch(AuthenticationException authExc){
            throw new
                    RuntimeException("Invalid username/password.");
        }
    }
}
