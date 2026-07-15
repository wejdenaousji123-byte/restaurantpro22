package com.restaurantpro.controller;

import com.restaurantpro.model.*;
import com.restaurantpro.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepo;
    private final PasswordEncoder passwordEncoder;
    private final CommandeRepository commandeRepo;
    private final TableRestaurantRepository tableRepo;
    private final NotificationRepository notifRepo;

    public UtilisateurController(UtilisateurRepository utilisateurRepo,
                                  PasswordEncoder passwordEncoder,
                                  CommandeRepository commandeRepo,
                                  TableRestaurantRepository tableRepo,
                                  NotificationRepository notifRepo) {
        this.utilisateurRepo = utilisateurRepo;
        this.passwordEncoder = passwordEncoder;
        this.commandeRepo = commandeRepo;
        this.tableRepo = tableRepo;
        this.notifRepo = notifRepo;
    }

    /** Liste tous les serveurs et cuisiniers (pas les autres admins, pour rester focalisé) */
    @GetMapping
    public List<Map<String, Object>> getAll() {
        return utilisateurRepo.findAll().stream()
                .filter(u -> u.getRole() == Utilisateur.Role.SERVEUR || u.getRole() == Utilisateur.Role.CUISINIER)
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("nom", u.getNom());
                    m.put("email", u.getEmail());
                    m.put("role", u.getRole().name());
                    m.put("actif", u.getActif());
                    boolean aDesCommandes = !commandeRepo.findByServeurWithLignes(u).isEmpty();
                    m.put("aDesCommandes", aDesCommandes);
                    return m;
                })
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> creerUtilisateur(@RequestBody Map<String, Object> body) {
        String nom   = body.get("nom")   != null ? body.get("nom").toString().trim()   : "";
        String email = body.get("email") != null ? body.get("email").toString().trim() : "";
        String motDePasse = body.get("motDePasse") != null ? body.get("motDePasse").toString() : "";
        String roleStr = body.get("role") != null ? body.get("role").toString().toUpperCase() : "";
        if (!email.endsWith("@restaurantpro.tn")) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'email doit impérativement se terminer par @restaurantpro.tn"));
        }
        if (nom.isEmpty() || email.isEmpty() || motDePasse.isEmpty() || roleStr.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Tous les champs sont obligatoires"));

        if (motDePasse.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));

        Utilisateur.Role role;
        try {
            role = Utilisateur.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rôle invalide"));
        }
        if (role == Utilisateur.Role.ADMIN)
            return ResponseEntity.badRequest().body(Map.of("error", "Création d'administrateur non autorisée ici"));

        if (utilisateurRepo.existsByEmail(email))
            return ResponseEntity.badRequest().body(Map.of("error", "Cet email est déjà utilisé"));

        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(motDePasse));
        u.setRole(role);
        u.setActif(true);

        Utilisateur saved = utilisateurRepo.save(u);
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "nom", saved.getNom(),
                "email", saved.getEmail(),
                "role", saved.getRole().name(),
                "actif", saved.getActif()
        ));
    }

    /** Activer / désactiver un compte (évite de supprimer l'historique lié) */
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changerStatut(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Utilisateur u = utilisateurRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if (u.getRole() == Utilisateur.Role.ADMIN)
            return ResponseEntity.badRequest().body(Map.of("error", "Action non autorisée sur un administrateur"));

        if (body.containsKey("actif")) {
            u.setActif(Boolean.TRUE.equals(body.get("actif")));
            utilisateurRepo.save(u);
        }
        return ResponseEntity.ok(Map.of("message", "Statut mis à jour", "actif", u.getActif()));
    }

    /** Réinitialiser le mot de passe d'un compte */
    @PutMapping("/{id}/mot-de-passe")
    public ResponseEntity<?> changerMotDePasse(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Utilisateur u = utilisateurRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();

        String nouveauMdp = body.get("motDePasse") != null ? body.get("motDePasse").toString() : "";
        if (nouveauMdp.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));

        u.setMotDePasse(passwordEncoder.encode(nouveauMdp));
        utilisateurRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour"));
    }
    @PutMapping("/{id}/profil")
    public ResponseEntity<?> modifierProfil(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Utilisateur u = utilisateurRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();

        if (body.containsKey("nom")) {
            String nouveauNom = body.get("nom").toString().trim();
            if (!nouveauNom.isEmpty()) u.setNom(nouveauNom);
        }

        if (body.containsKey("email")) {
            String nouvelEmail = body.get("email").toString().trim();
            if (!nouvelEmail.endsWith("@restaurantpro.tn")) {
                return ResponseEntity.badRequest().body(Map.of("error", "L'email doit se terminer par @restaurantpro.tn"));
            }
            if (utilisateurRepo.existsByEmail(nouvelEmail) && !u.getEmail().equals(nouvelEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cet email est déjà utilisé"));
            }
            u.setEmail(nouvelEmail);
        }

        utilisateurRepo.save(u);
        return ResponseEntity.ok(Map.of("message", "Profil mis à jour avec succès"));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        Utilisateur u = utilisateurRepo.findById(id).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();
        if (u.getRole() == Utilisateur.Role.ADMIN)
            return ResponseEntity.badRequest().body(Map.of("error", "Suppression d'administrateur non autorisée"));

        List<Commande> commandesActives = commandeRepo.findByServeurWithLignes(u).stream()
                .filter(c -> c.getStatut() == Commande.Statut.EN_ATTENTE
                          || c.getStatut() == Commande.Statut.EN_PREPARATION
                          || c.getStatut() == Commande.Statut.PRETE)
                .toList();
        if (!commandesActives.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Impossible de supprimer ce compte : il a " + commandesActives.size()
                    + " commande(s) en cours. Attendez qu'elles soient terminées (servies ou annulées)."));
        }

        List<Commande> historique = commandeRepo.findByServeurWithLignes(u);
        for (Commande c : historique) {
            c.setServeur(null);
            commandeRepo.save(c);
        }

        for (TableRestaurant t : tableRepo.findAll()) {
            if (u.equals(t.getServeur())) {
                t.setServeur(null);
                tableRepo.save(t);
            }
        }

        List<Notification> notifs = notifRepo.findByDestinataireOrderByHeureCreationDesc(u);
        notifRepo.deleteAll(notifs);

        utilisateurRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }
}
