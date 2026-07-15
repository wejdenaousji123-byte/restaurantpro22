package com.restaurantpro.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notif")
    private Long idNotif;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotif type;

    @Column(nullable = false)
    private Boolean lue = false;

    @Column(name = "heure_creation")
    private LocalDateTime heureCreation = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_destinataire", nullable = false)
    @JsonIgnore
    private Utilisateur destinataire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_commande")
    @JsonIgnore
    private Commande commande;

    public enum TypeNotif {
        COMMANDE_PRETE, NOUVELLE_COMMANDE, COMMANDE_MODIFIEE, COMMANDE_ANNULEE, ADDITION, ALERTE_ATTENTE
    }

    public Notification() {}

    public Long getIdNotif() { return idNotif; }
    public void setIdNotif(Long idNotif) { this.idNotif = idNotif; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public TypeNotif getType() { return type; }
    public void setType(TypeNotif type) { this.type = type; }
    public Boolean getLue() { return lue; }
    public void setLue(Boolean lue) { this.lue = lue; }
    public LocalDateTime getHeureCreation() { return heureCreation; }
    public void setHeureCreation(LocalDateTime heureCreation) { this.heureCreation = heureCreation; }
    public Utilisateur getDestinataire() { return destinataire; }
    public void setDestinataire(Utilisateur destinataire) { this.destinataire = destinataire; }
    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }
}
