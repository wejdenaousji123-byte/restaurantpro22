package com.restaurantpro.controller;

import com.restaurantpro.model.*;
import com.restaurantpro.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class StatistiquesController {

    private final CommandeRepository commandeRepo;
    private final TableRestaurantRepository tableRepo;
    private final ArticleRepository articleRepo;
    private final AdditionRepository additionRepo;
    private final UtilisateurRepository utilisateurRepo;

    public StatistiquesController(CommandeRepository commandeRepo,
                                   TableRestaurantRepository tableRepo,
                                   ArticleRepository articleRepo,
                                   AdditionRepository additionRepo,
                                   UtilisateurRepository utilisateurRepo) {
        this.commandeRepo    = commandeRepo;
        this.tableRepo       = tableRepo;
        this.articleRepo     = articleRepo;
        this.additionRepo    = additionRepo;
        this.utilisateurRepo = utilisateurRepo;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalTables   = tableRepo.count();
        long tablesOccupees = tableRepo.findByStatut(TableRestaurant.Statut.OCCUPEE).size();
        stats.put("totalTables",    totalTables);
        stats.put("tablesOccupees", tablesOccupees);
        stats.put("tablesLibres",   totalTables - tablesOccupees);

        List<Commande> actives = commandeRepo.findByStatutIn(
                List.of(Commande.Statut.EN_ATTENTE, Commande.Statut.EN_PREPARATION));
        stats.put("commandesEnCours", actives.size());

        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime debutMois = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime debutAnnee = LocalDate.now().withDayOfYear(1).atStartOfDay();
        List<Addition> additions = additionRepo.findAll();
        double caJour = additionRepo.findAll().stream()
                .filter(a -> a.getStatut() == Addition.Statut.PAYEE
                        && a.getHeurePaiement() != null
                        && a.getHeurePaiement().isAfter(debutJour))
                .mapToDouble(Addition::getMontantTotal)
                .sum();
        double caMois = additions.stream()
                .filter(a -> a.getStatut() == Addition.Statut.PAYEE && a.getHeurePaiement() != null && a.getHeurePaiement().isAfter(debutMois))
                .mapToDouble(Addition::getMontantTotal).sum();

        double caAnnee = additions.stream()
                .filter(a -> a.getStatut() == Addition.Statut.PAYEE && a.getHeurePaiement() != null && a.getHeurePaiement().isAfter(debutAnnee))
                .mapToDouble(Addition::getMontantTotal).sum();
        stats.put("chiffreAffairesJour", String.format("%.3f", caJour));
        stats.put("chiffreAffairesMois", String.format("%.3f", caMois));
        stats.put("chiffreAffairesAnnee", String.format("%.3f", caAnnee));
        List<Object[]> results = commandeRepo.getTopArticles();
        List<Map<String, Object>> topList = new ArrayList<>();

        if (results != null) {
            for (Object[] obj : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("nom", obj[0].toString());
                item.put("quantite", ((Number) obj[1]).longValue());
                topList.add(item);
            }
        }
        stats.put("bestSellers", topList);
        long nbServeurs = utilisateurRepo.findByRole(Utilisateur.Role.SERVEUR)
                .stream().filter(u -> u.getActif()).count();
        stats.put("serveursActifs", nbServeurs);

        return stats;
    }
}
