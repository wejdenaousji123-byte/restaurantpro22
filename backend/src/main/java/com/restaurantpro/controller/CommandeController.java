package com.restaurantpro.controller;

import com.restaurantpro.model.*;
import com.restaurantpro.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/commandes")
@CrossOrigin(origins = "*")
public class CommandeController {

    @PersistenceContext
    private EntityManager em;

    private final CommandeRepository commandeRepo;
    private final TableRestaurantRepository tableRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final ArticleRepository articleRepo;
    private final NotificationRepository notifRepo;
    private final AdditionRepository additionRepo;

    public CommandeController(CommandeRepository commandeRepo,
                               TableRestaurantRepository tableRepo,
                               UtilisateurRepository utilisateurRepo,
                               ArticleRepository articleRepo,
                               NotificationRepository notifRepo,
                               AdditionRepository additionRepo) {
        this.commandeRepo    = commandeRepo;
        this.tableRepo       = tableRepo;
        this.utilisateurRepo = utilisateurRepo;
        this.articleRepo     = articleRepo;
        this.notifRepo       = notifRepo;
        this.additionRepo    = additionRepo;
    }

    @GetMapping
    public List<Commande> getAll() { return commandeRepo.findAll(); }

    @GetMapping("/toutes")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getToutesLesCommandes() {
        List<Commande> commandes = commandeRepo.findAllForGerant();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Commande c : commandes) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("idCommande", c.getIdCommande());
            dto.put("statut", c.getStatut());
            dto.put("heureCreation", c.getHeureCreation());
            dto.put("notes", c.getNotes());

            if (c.getTable() != null) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("idTable", c.getTable().getIdTable());
                t.put("numero", c.getTable().getNumero());
                dto.put("table", t);
            }
            if (c.getServeur() != null) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("idServeur", c.getServeur().getId());
                s.put("nom", c.getServeur().getNom());
                dto.put("serveur", s);
            }

            List<Map<String, Object>> lignesDto = new ArrayList<>();
            double sousTotalLignes = 0;
            if (c.getLignes() != null) {
                for (LigneCommande l : c.getLignes()) {
                    Map<String, Object> ld = new LinkedHashMap<>();
                    ld.put("quantite", l.getQuantite());
                    ld.put("prixUnitaire", l.getPrixUnitaire());
                    if (l.getArticle() != null) {
                        Map<String, Object> art = new LinkedHashMap<>();
                        art.put("idArticle", l.getArticle().getIdArticle());
                        art.put("nom", l.getArticle().getNom());
                        art.put("prix", l.getArticle().getPrix());
                        ld.put("article", art);
                    } else if (l.getNomArticle() != null) {
                        // Article supprimé du menu depuis : on retombe sur le nom figé
                        // pour ne pas faire disparaître la ligne de l'historique.
                        Map<String, Object> art = new LinkedHashMap<>();
                        art.put("idArticle", null);
                        art.put("nom", l.getNomArticle());
                        art.put("prix", l.getPrixUnitaire());
                        art.put("supprime", true);
                        ld.put("article", art);
                    }
                    lignesDto.add(ld);
                    sousTotalLignes += l.getPrixUnitaire() * l.getQuantite();
                }
            }
            dto.put("lignes", lignesDto);

            // Chercher l'addition PAYEE de la table pour récupérer la réduction appliquée
            double totalFinal = sousTotalLignes;
            if (c.getTable() != null && c.getStatut() == Commande.Statut.SERVIE) {
                Optional<Addition> addition = additionRepo.findByTableAndStatut(c.getTable(), Addition.Statut.PAYEE);
                if (addition.isPresent() && addition.get().getReduction() > 0) {
                    totalFinal = sousTotalLignes * (1 - addition.get().getReduction() / 100.0);
                }
            }
            dto.put("totalFinal", totalFinal);
            result.add(dto);
        }
        return result;
    }
    @GetMapping("/actives")
    @Transactional(readOnly = true)
    public List<Commande> getActives() {
        return commandeRepo.findActiveWithLignes();
    }

    @GetMapping("/table/{idTable}")
    public List<Commande> getByTable(@PathVariable Long idTable) {
        return commandeRepo.findActiveByTableId(idTable);
    }

    @GetMapping("/serveur/{idServeur}")
    @Transactional(readOnly = true)
    public List<Commande> getByServeur(@PathVariable Long idServeur) {
        Utilisateur s = utilisateurRepo.findById(idServeur).orElseThrow();
        return commandeRepo.findByServeurWithLignes(s);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> createCommande(@RequestBody Map<String, Object> body) {
        Long idTable   = Long.valueOf(body.get("idTable").toString());
        Long idServeur = Long.valueOf(body.get("idServeur").toString());
        TableRestaurant table   = tableRepo.findById(idTable).orElseThrow();
        Utilisateur serveur     = utilisateurRepo.findById(idServeur).orElseThrow();

        Commande commande = new Commande();
        commande.setTable(table);
        commande.setServeur(serveur);
        commande.setNotes(body.containsKey("notes") ? body.get("notes").toString() : null);

        List<Map<String, Object>> lignesData = (List<Map<String, Object>>) body.get("lignes");
        List<LigneCommande> lignes = new ArrayList<>();
        for (Map<String, Object> ld : lignesData) {
            Long idArticle  = Long.valueOf(ld.get("idArticle").toString());
            int quantite    = Integer.parseInt(ld.get("quantite").toString());
            Article article = articleRepo.findById(idArticle).orElseThrow();
            LigneCommande ligne = new LigneCommande();
            ligne.setCommande(commande);
            ligne.setArticle(article);
            ligne.setNomArticle(article.getNom());
            ligne.setQuantite(quantite);
            ligne.setPrixUnitaire(article.getPrix());
            if (ld.containsKey("noteSpeciale")) ligne.setNoteSpeciale(ld.get("noteSpeciale").toString());
            lignes.add(ligne);
        }
        commande.setLignes(lignes);
        Commande saved = commandeRepo.save(commande);

        List<Utilisateur> cuisiniers = utilisateurRepo.findByRole(Utilisateur.Role.CUISINIER);
        for (Utilisateur c : cuisiniers) {
            Notification n = new Notification();
            n.setDestinataire(c);
            n.setCommande(saved);
            n.setType(Notification.TypeNotif.NOUVELLE_COMMANDE);
            n.setMessage("Nouvelle commande — Table " + table.getNumero());
            notifRepo.save(n);
        }
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','CUISINIER')")
    public ResponseEntity<?> changerStatut(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Commande c = commandeRepo.findById(id).orElseThrow();
        Commande.Statut newStatut = Commande.Statut.valueOf(body.get("statut").toUpperCase());
        c.setStatut(newStatut);
        commandeRepo.save(c);
        if (newStatut == Commande.Statut.PRETE) {
            Notification n = new Notification();
            n.setDestinataire(c.getServeur());
            n.setCommande(c);
            n.setType(Notification.TypeNotif.COMMANDE_PRETE);
            n.setMessage("Commande prête — Table " + c.getTable().getNumero());
            notifRepo.save(n);
        }
        return ResponseEntity.ok(c);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> updateCommande(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Commande c = commandeRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        if (c.getStatut() != Commande.Statut.EN_ATTENTE)
            return ResponseEntity.badRequest().body(Map.of("error", "Seules les commandes EN_ATTENTE peuvent être modifiées"));

        if (body.containsKey("notes")) c.setNotes(body.get("notes") != null ? body.get("notes").toString() : null);

        if (body.containsKey("nbPersonnes")) {
            int nbPers = ((Number) body.get("nbPersonnes")).intValue();
            TableRestaurant table = c.getTable();
            if (nbPers > table.getCapacite())
                return ResponseEntity.badRequest().body(Map.of("error", "Capacité insuffisante (" + table.getCapacite() + " max)"));
            table.setNbPersonnes(nbPers);
            tableRepo.save(table);
        }

        if (body.containsKey("lignes")) {
            List<Map<String, Object>> lignesData = (List<Map<String, Object>>) body.get("lignes");
            c.getLignes().clear();
            commandeRepo.saveAndFlush(c);
            for (Map<String, Object> ld : lignesData) {
                Long idArticle = Long.valueOf(ld.get("idArticle").toString());
                int quantite   = ((Number) ld.get("quantite")).intValue();
                Article article = articleRepo.findById(idArticle).orElseThrow();
                LigneCommande ligne = new LigneCommande();
                ligne.setCommande(c);
                ligne.setArticle(article);
                ligne.setNomArticle(article.getNom());
                ligne.setQuantite(quantite);
                ligne.setPrixUnitaire(article.getPrix());
                if (ld.containsKey("noteSpeciale")) ligne.setNoteSpeciale(ld.get("noteSpeciale").toString());
                c.getLignes().add(ligne);
            }
        }

        Commande saved = commandeRepo.saveAndFlush(c);
        em.refresh(saved);

        List<Utilisateur> cuisiniers = utilisateurRepo.findByRole(Utilisateur.Role.CUISINIER);
        String numTable = saved.getTable() != null ? saved.getTable().getNumero().toString() : "?";
        for (Utilisateur cuisinier : cuisiniers) {
            Notification n = new Notification();
            n.setDestinataire(cuisinier);
            n.setType(Notification.TypeNotif.COMMANDE_MODIFIEE);
            n.setMessage("✏️ Commande Table " + numTable + " modifiée par le serveur");
            notifRepo.save(n);
        }

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> deleteCommande(@PathVariable Long id) {
        Commande c = commandeRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        TableRestaurant table = c.getTable();
        String numTable = table != null ? table.getNumero().toString() : "?";

        List<Utilisateur> cuisiniers = utilisateurRepo.findByRole(Utilisateur.Role.CUISINIER);
        for (Utilisateur cu : cuisiniers) {
            Notification n = new Notification();
            n.setDestinataire(cu);
            n.setType(Notification.TypeNotif.COMMANDE_ANNULEE);
            n.setMessage("❌ Commande Table " + numTable + " annulée par le serveur");
            notifRepo.save(n);
        }

        commandeRepo.deleteById(id);
        commandeRepo.flush();

        if (table != null) {
            List<Commande> restantes = commandeRepo.findByTableAndStatutNot(table, Commande.Statut.ANNULEE)
                    .stream()
                    .filter(cmd -> cmd.getStatut() != Commande.Statut.SERVIE)
                    .toList();
            System.out.println("DEBUG annulation: table=" + numTable + " commandes restantes=" + restantes.size());
            if (restantes.isEmpty()) {
                table.setStatut(TableRestaurant.Statut.LIBRE);
                table.setNbPersonnes(0);
                table.setServeur(null);
                table.setDemandeAddition(false);
                tableRepo.save(table);
                System.out.println("DEBUG annulation: table " + numTable + " libérée");
            }
        }

        return ResponseEntity.ok(Map.of("message", "Commande supprimée"));
    }

    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN','SERVEUR')")
    public ResponseEntity<?> annuler(@PathVariable Long id) {
        Commande c = commandeRepo.findById(id).orElseThrow();
        c.setStatut(Commande.Statut.ANNULEE);
        commandeRepo.save(c);
        List<Utilisateur> cuisiniers = utilisateurRepo.findByRole(Utilisateur.Role.CUISINIER);
        for (Utilisateur cu : cuisiniers) {
            Notification n = new Notification();
            n.setDestinataire(cu);
            n.setCommande(c);
            n.setType(Notification.TypeNotif.COMMANDE_ANNULEE);
            n.setMessage("Commande annulée — Table " + c.getTable().getNumero());
            notifRepo.save(n);
        }

        TableRestaurant table = c.getTable();
        if (table != null) {
            List<Commande> restantes = commandeRepo.findByTableAndStatutNot(table, Commande.Statut.ANNULEE)
                    .stream()
                    .filter(cmd -> cmd.getStatut() != Commande.Statut.SERVIE)
                    .toList();
            if (restantes.isEmpty()) {
                table.setStatut(TableRestaurant.Statut.LIBRE);
                table.setNbPersonnes(0);
                table.setServeur(null);
                table.setDemandeAddition(false);
                tableRepo.save(table);
            }
        }

        return ResponseEntity.ok(Map.of("message", "Commande annulée"));
    }
}
