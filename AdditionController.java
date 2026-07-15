package com.restaurantpro.controller;

import com.restaurantpro.model.*;
import com.restaurantpro.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/additions")
@CrossOrigin(origins = "*")
public class AdditionController {

    private final AdditionRepository additionRepo;
    private final TableRestaurantRepository tableRepo;
    private final CommandeRepository commandeRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final NotificationRepository notifRepo;

    public AdditionController(AdditionRepository additionRepo,
                               TableRestaurantRepository tableRepo,
                               CommandeRepository commandeRepo,
                               UtilisateurRepository utilisateurRepo,
                               NotificationRepository notifRepo) {
        this.additionRepo    = additionRepo;
        this.tableRepo       = tableRepo;
        this.commandeRepo    = commandeRepo;
        this.utilisateurRepo = utilisateurRepo;
        this.notifRepo       = notifRepo;
    }

    @GetMapping("/en-cours")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getEnCours() {
        List<Map<String, Object>> result = tableRepo.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getDemandeAddition()))
                .map(t -> {
                    Optional<Addition> add = additionRepo.findByTable(t);
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("idTable", t.getIdTable());
                    m.put("numero", t.getNumero());
                    add.ifPresent(a -> {
                        m.put("idAddition", a.getIdAddition());
                        m.put("montantTotal", a.getMontantTotal());
                    });
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/table/{idTable}")
    public ResponseEntity<?> getByTable(@PathVariable Long idTable) {
        TableRestaurant t = tableRepo.findById(idTable).orElseThrow();
        Optional<Addition> addition = additionRepo.findByTable(t);
        return addition.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/table/{idTable}/calculer")
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> calculer(@PathVariable Long idTable) {
        TableRestaurant table = tableRepo.findById(idTable).orElseThrow();
        additionRepo.findByTable(table).ifPresent(additionRepo::delete);

        List<Commande> commandes = commandeRepo.findByTableAndStatutNot(table, Commande.Statut.ANNULEE);
        double total = 0;
        for (Commande c : commandes) {
            if (c.getLignes() != null) {
                for (LigneCommande l : c.getLignes()) {
                    total += l.getPrixUnitaire() * l.getQuantite();
                }
            }
        }

        Addition addition = new Addition();
        addition.setTable(table);
        addition.setMontantTotal(total);
        Addition saved = additionRepo.save(addition);

        table.setDemandeAddition(true);
        tableRepo.save(table);

        List<Utilisateur> admins = utilisateurRepo.findByRole(Utilisateur.Role.ADMIN);
        for (Utilisateur admin : admins) {
            Notification n = new Notification();
            n.setDestinataire(admin);
            n.setType(Notification.TypeNotif.ADDITION);
            n.setMessage("Demande addition — Table " + table.getNumero() + " — " + String.format("%.3f TND", total));
            notifRepo.save(n);
        }
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/payer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> payer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Addition a = additionRepo.findById(id).orElseThrow();
        a.setModePaiement(Addition.ModePaiement.valueOf(body.get("modePaiement").toString().toUpperCase()));
        if (body.containsKey("reduction")) {
            double reduction = Double.parseDouble(body.get("reduction").toString());
            a.setReduction(reduction);
            a.setMontantTotal(a.getMontantTotal() * (1 - reduction / 100));
        }
        a.setStatut(Addition.Statut.PAYEE);
        a.setHeurePaiement(LocalDateTime.now());
        if (body.containsKey("idAdmin")) {
            Long idAdmin = Long.valueOf(body.get("idAdmin").toString());
            utilisateurRepo.findById(idAdmin).ifPresent(a::setAdmin);
        }
        additionRepo.save(a);

        TableRestaurant table = a.getTable();
        table.setStatut(TableRestaurant.Statut.LIBRE);
        table.setNbPersonnes(0);
        table.setServeur(null);
        table.setDemandeAddition(false);
        tableRepo.save(table);

        return ResponseEntity.ok(Map.of(
                "message", "Addition payée — Table libérée",
                "montant", a.getMontantTotal(),
                "mode",    a.getModePaiement()
        ));
    }
}
