package com.restaurantpro.scheduler;

import com.restaurantpro.model.*;
import com.restaurantpro.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class CommandeAttenteScheduler {

    private final CommandeRepository commandeRepo;
    private final UtilisateurRepository utilisateurRepo;
    private final NotificationRepository notifRepo;

    public CommandeAttenteScheduler(CommandeRepository commandeRepo,
                                     UtilisateurRepository utilisateurRepo,
                                     NotificationRepository notifRepo) {
        this.commandeRepo    = commandeRepo;
        this.utilisateurRepo = utilisateurRepo;
        this.notifRepo       = notifRepo;
    }


    @Scheduled(fixedRate = 60000)
    public void verifierCommandesEnAttente() {
        List<Commande> enAttente = commandeRepo.findByStatut(Commande.Statut.EN_ATTENTE);
        LocalDateTime maintenant = LocalDateTime.now();

        List<Utilisateur> cuisiniers = utilisateurRepo.findByRole(Utilisateur.Role.CUISINIER);
        if (cuisiniers.isEmpty()) return;

        for (Commande c : enAttente) {
            if (Boolean.TRUE.equals(c.getAlerteAttenteEnvoyee())) continue;
            if (c.getHeureCreation() == null) continue;

            long minutesEcoulees = ChronoUnit.MINUTES.between(c.getHeureCreation(), maintenant);
            if (minutesEcoulees >= 30) {
                String numTable = c.getTable() != null ? c.getTable().getNumero().toString() : "?";
                for (Utilisateur cuisinier : cuisiniers) {
                    Notification n = new Notification();
                    n.setDestinataire(cuisinier);
                    n.setType(Notification.TypeNotif.ALERTE_ATTENTE);
                    n.setMessage("⏰ Commande Table " + numTable + " en attente depuis " + minutesEcoulees + " min !");
                    notifRepo.save(n);
                }
                c.setAlerteAttenteEnvoyee(true);
                commandeRepo.save(c);
            }
        }
    }
}
