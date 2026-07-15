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
                        m.put("modePaiement", a.getModePaiement() != null ? a.getModePaiement().name() : null);
                    });
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/table/{idTable}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> getByTable(@PathVariable Long idTable) {
        TableRestaurant t = tableRepo.findById(idTable).orElse(null);
        if (t == null) return ResponseEntity.notFound().build();
        Optional<Addition> addition = additionRepo.findByTable(t);
        return addition.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/table/{idTable}/calculer")
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> calculer(@PathVariable Long idTable,
                                      @RequestBody(required = false) Map<String, Object> body) {
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

        if (body != null && body.get("modePaiement") != null) {
            try {
                addition.setModePaiement(Addition.ModePaiement.valueOf(body.get("modePaiement").toString().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        Addition saved = additionRepo.save(addition);

        table.setDemandeAddition(true);
        tableRepo.save(table);

        List<Utilisateur> admins = utilisateurRepo.findByRole(Utilisateur.Role.ADMIN);
        String modeLabel = addition.getModePaiement() != null
                ? (addition.getModePaiement() == Addition.ModePaiement.CARTE ? "carte bancaire" : "espèces")
                : "?";
        for (Utilisateur admin : admins) {
            Notification n = new Notification();
            n.setDestinataire(admin);
            n.setType(Notification.TypeNotif.ADDITION);
            n.setMessage("Demande addition — Table " + table.getNumero()
                    + " — " + String.format("%.3f TND", total)
                    + " — paiement par " + modeLabel);
            notifRepo.save(n);
        }
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/payer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> payer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Addition a = additionRepo.findById(id).orElse(null);
            if (a == null) return ResponseEntity.status(404).body(Map.of("error", "Addition introuvable (id=" + id + ")"));

            if (body.get("modePaiement") != null) {
                a.setModePaiement(Addition.ModePaiement.valueOf(body.get("modePaiement").toString().toUpperCase()));
            }
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
            Utilisateur serveurDeLaTable = table.getServeur();

            table.setStatut(TableRestaurant.Statut.PAYEE);
            table.setNbPersonnes(0);
            table.setServeur(null);
            table.setDemandeAddition(false);
            tableRepo.save(table);

            List<Commande> commandes = commandeRepo.findByTableAndStatutNot(table, Commande.Statut.ANNULEE);
            for (Commande c : commandes) {
                c.setStatut(Commande.Statut.SERVIE);
                commandeRepo.save(c);
            }

            if (serveurDeLaTable != null) {
                Notification n = new Notification();
                n.setDestinataire(serveurDeLaTable);
                n.setType(Notification.TypeNotif.ADDITION);
                String modeLabel = a.getModePaiement() != null
                        ? (a.getModePaiement() == Addition.ModePaiement.CARTE ? "carte bancaire" : "espèces")
                        : "";
                n.setMessage("Addition clôturée — Table " + table.getNumero()
                        + " — " + String.format("%.3f TND", a.getMontantTotal())
                        + (modeLabel.isEmpty() ? "" : " — payé par " + modeLabel));
                notifRepo.save(n);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Addition payée — Table libérée",
                    "montant", a.getMontantTotal(),
                    "mode",    a.getModePaiement() != null ? a.getModePaiement().name() : ""
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }
}
