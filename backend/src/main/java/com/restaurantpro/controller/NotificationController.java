package com.restaurantpro.controller;

import com.restaurantpro.model.Notification;
import com.restaurantpro.model.Utilisateur;
import com.restaurantpro.repository.NotificationRepository;
import com.restaurantpro.repository.UtilisateurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notifRepo;
    private final UtilisateurRepository utilisateurRepo;

    public NotificationController(NotificationRepository notifRepo, UtilisateurRepository utilisateurRepo) {
        this.notifRepo       = notifRepo;
        this.utilisateurRepo = utilisateurRepo;
    }

    @GetMapping("/user/{idUser}")
    @Transactional(readOnly = true)
    public List<Notification> getByUser(@PathVariable Long idUser) {
        Utilisateur u = utilisateurRepo.findById(idUser).orElseThrow();
        return notifRepo.findByDestinataireOrderByHeureCreationDesc(u);
    }

    @GetMapping("/user/{idUser}/nonlues")
    public List<Notification> getNonLues(@PathVariable Long idUser) {
        Utilisateur u = utilisateurRepo.findById(idUser).orElseThrow();
        return notifRepo.findByDestinataireAndLueFalseOrderByHeureCreationDesc(u);
    }

    @GetMapping("/user/{idUser}/count")
    public Map<String, Long> countNonLues(@PathVariable Long idUser) {
        Utilisateur u = utilisateurRepo.findById(idUser).orElseThrow();
        return Map.of("count", notifRepo.countByDestinataireAndLueFalse(u));
    }

    @PutMapping("/{id}/lire")
    public ResponseEntity<?> marquerLue(@PathVariable Long id) {
        Notification n = notifRepo.findById(id).orElseThrow();
        n.setLue(true);
        return ResponseEntity.ok(notifRepo.save(n));
    }

    @PutMapping("/user/{idUser}/lire-tout")
    public ResponseEntity<?> lireTout(@PathVariable Long idUser) {
        Utilisateur u = utilisateurRepo.findById(idUser).orElseThrow();
        List<Notification> nonLues = notifRepo.findByDestinataireAndLueFalseOrderByHeureCreationDesc(u);
        nonLues.forEach(n -> n.setLue(true));
        notifRepo.saveAll(nonLues);
        return ResponseEntity.ok(Map.of("message", nonLues.size() + " notification(s) marquée(s) comme lues"));
    }
}
