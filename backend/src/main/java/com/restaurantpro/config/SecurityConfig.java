package com.restaurantpro.config;

import com.restaurantpro.model.Utilisateur;
import com.restaurantpro.repository.UtilisateurRepository;
import com.restaurantpro.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String email = jwtUtil.extractEmail(token);
                if (email != null) {
                    UserDetails ud = userDetailsService.loadUserByUsername(email);
                    if (jwtUtil.isTokenValid(token, ud)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        System.err.println("[JwtAuthFilter] Token invalide pour " + email + " sur " + req.getRequestURI());
                    }
                } else {
                    System.err.println("[JwtAuthFilter] Email null extrait du token pour " + req.getRequestURI());
                }
            } catch (Exception e) {
                System.err.println("[JwtAuthFilter] Erreur validation token sur " + req.getRequestURI() + ": " + e);
            }
        } else {
            System.err.println("[JwtAuthFilter] Pas de header Authorization Bearer pour " + req.getRequestURI());
        }
        try {
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UtilisateurRepository utilisateurRepository;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UtilisateurRepository utilisateurRepository) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            Utilisateur u = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + email));
            return new User(
                    u.getEmail(), u.getMotDePasse(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration conf) throws Exception {
        return conf.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .cors(cors -> cors.disable())
                .httpBasic(hb -> hb.disable())
                .formLogin(fl -> fl.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/tables", "/api/tables/**", "/tables", "/tables/**").permitAll()
                        .requestMatchers("/api/auth/**", "/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}