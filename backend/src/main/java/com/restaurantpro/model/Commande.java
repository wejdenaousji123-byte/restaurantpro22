package com.restaurantpro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "commande")
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_commande")
    private Long idCommande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(name = "heure_creation")
    private LocalDateTime heureCreation = LocalDateTime.now();
    @Column(name = "date_paiement")
    private LocalDateTime datePaiement;

    public LocalDateTime getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDateTime datePaiement) { this.datePaiement = datePaiement; }
    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_table", nullable = false)
    private TableRestaurant table;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_serveur")
    private Utilisateur serveur;

    @Column(name = "alerte_attente_envoyee")
    private Boolean alerteAttenteEnvoyee = false;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LigneCommande> lignes;

    public enum Statut { EN_ATTENTE, EN_PREPARATION, PRETE, SERVIE, ANNULEE }

    public Commande() {}

    public Long getIdCommande() { return idCommande; }
    public void setIdCommande(Long idCommande) { this.idCommande = idCommande; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDateTime getHeureCreation() { return heureCreation; }
    public void setHeureCreation(LocalDateTime heureCreation) { this.heureCreation = heureCreation; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public TableRestaurant getTable() { return table; }
    public void setTable(TableRestaurant table) { this.table = table; }
    public Utilisateur getServeur() { return serveur; }
    public void setServeur(Utilisateur serveur) { this.serveur = serveur; }
    public List<LigneCommande> getLignes() { return lignes; }
    public void setLignes(List<LigneCommande> lignes) { this.lignes = lignes; }
    public Boolean getAlerteAttenteEnvoyee() { return alerteAttenteEnvoyee != null && alerteAttenteEnvoyee; }
    public void setAlerteAttenteEnvoyee(Boolean alerteAttenteEnvoyee) { this.alerteAttenteEnvoyee = alerteAttenteEnvoyee; }
    public double getTotal() {
        if (lignes == null) return 0.0;
        return lignes.stream()
                .mapToDouble(l -> l.getPrixUnitaire() * l.getQuantite())
                .sum();
    }
}
