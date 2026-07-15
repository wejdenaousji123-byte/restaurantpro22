package com.restaurantpro.controller;

import com.restaurantpro.model.TableRestaurant;
import com.restaurantpro.model.Utilisateur;
import com.restaurantpro.repository.TableRestaurantRepository;
import com.restaurantpro.repository.UtilisateurRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class TableController {

    private final TableRestaurantRepository tableRepo;
    private final UtilisateurRepository utilisateurRepo;

    public TableController(TableRestaurantRepository tableRepo, UtilisateurRepository utilisateurRepo) {
        this.tableRepo = tableRepo;
        this.utilisateurRepo = utilisateurRepo;
    }

    @GetMapping
    public List<TableRestaurant> getTables() { return tableRepo.findAll(); }

    @GetMapping("/libres")
    public List<TableRestaurant> getLibres() { return tableRepo.findByStatut(TableRestaurant.Statut.LIBRE); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTable(@RequestBody Map<String, Object> body) {
        int numero   = (int) body.get("numero");
        int capacite = (int) body.get("capacite");
        if (tableRepo.existsByNumero(numero))
            return ResponseEntity.badRequest().body(Map.of("error", "Numéro de table déjà existant"));
        TableRestaurant t = new TableRestaurant();
        t.setNumero(numero);
        t.setCapacite(capacite);
        if (body.containsKey("emplacement")) t.setEmplacement(body.get("emplacement").toString());
        return ResponseEntity.ok(tableRepo.save(t));
    }

    @PutMapping("/{id}/ouvrir")
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> ouvrirTable(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TableRestaurant t = tableRepo.findById(id).orElseThrow();
        if (t.getStatut() == TableRestaurant.Statut.OCCUPEE)
            return ResponseEntity.badRequest().body(Map.of("error", "Table déjà occupée"));
        int nbPersonnes = (int) body.get("nbPersonnes");
        if (nbPersonnes > t.getCapacite())
            return ResponseEntity.badRequest().body(Map.of("error", "Capacité insuffisante (" + t.getCapacite() + " max)"));
        Long idServeur = Long.valueOf(body.get("idServeur").toString());
        Utilisateur serveur = utilisateurRepo.findById(idServeur).orElse(null);
        t.setStatut(TableRestaurant.Statut.OCCUPEE);
        t.setNbPersonnes(nbPersonnes);
        t.setServeur(serveur);
        return ResponseEntity.ok(tableRepo.save(t));
    }

    @PutMapping("/{id}/liberer")
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> libererTable(@PathVariable Long id) {
        TableRestaurant t = tableRepo.findById(id).orElseThrow();
        t.setStatut(TableRestaurant.Statut.LIBRE);
        t.setNbPersonnes(0);
        t.setServeur(null);
        return ResponseEntity.ok(tableRepo.save(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TableRestaurant t = tableRepo.findById(id).orElseThrow();
        if (body.containsKey("numero")) {
            int newNumero = (int) body.get("numero");
            if (newNumero != t.getNumero() && tableRepo.existsByNumero(newNumero))
                return ResponseEntity.badRequest().body(Map.of("error", "Numéro de table déjà existant"));
            t.setNumero(newNumero);
        }
        if (body.containsKey("capacite"))    t.setCapacite((int) body.get("capacite"));
        if (body.containsKey("emplacement")) t.setEmplacement(body.get("emplacement").toString());
        return ResponseEntity.ok(tableRepo.save(t));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        TableRestaurant table = tableRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Table non trouvée"));

        if (table.getStatut() == TableRestaurant.Statut.OCCUPEE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Impossible de supprimer une table occupée"));
        }

        tableRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Table supprimée"));
    }
}