package com.restaurantpro.controller;

import com.restaurantpro.model.Utilisateur;
import com.restaurantpro.repository.UtilisateurRepository;
import com.restaurantpro.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UtilisateurRepository utilisateurRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authManager,
                          UtilisateurRepository utilisateurRepo,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.authManager = authManager;
        this.utilisateurRepo = utilisateurRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        System.out.println("=== LOGIN: " + email + " / " + password);
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            System.out.println("=== BAD CREDENTIALS ===");
            return ResponseEntity.status(401).body(Map.of("error", "Identifiants incorrects"));
        } catch (Exception e) {
            System.out.println("=== ERROR: " + e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
        Utilisateur u = utilisateurRepo.findByEmail(email).orElseThrow();
        if (!u.getActif()) {
            return ResponseEntity.status(403).body(Map.of("error", "Compte désactivé"));
        }
        UserDetails ud = new User(u.getEmail(), u.getMotDePasse(),
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        String token = jwtUtil.generateToken(ud, u.getRole().name());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role",  u.getRole().name(),
                "nom",   u.getNom(),
                "id",    u.getId()
        ));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (utilisateurRepo.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email déjà utilisé"));
        }
        Utilisateur u = new Utilisateur();
        u.setNom(body.get("nom"));
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(body.get("password")));
        u.setRole(Utilisateur.Role.valueOf(body.get("role").toUpperCase()));
        utilisateurRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "Utilisateur créé avec succès"));
    }
}
