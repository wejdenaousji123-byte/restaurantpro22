package com.restaurantpro.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "addition")
public class Addition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_addition")
    private Long idAddition;

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal = 0.0;

    @Column(nullable = false)
    private Double reduction = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement")
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_COURS;

    @Column(name = "heure_paiement")
    private LocalDateTime heurePaiement;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_table", nullable = false, unique = true)
    private TableRestaurant table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin")
    private Utilisateur admin;

    public enum ModePaiement { ESPECES, CARTE, AUTRE }
    public enum Statut { EN_COURS, PAYEE }

    public Addition() {}

    public Long getIdAddition() { return idAddition; }
    public void setIdAddition(Long idAddition) { this.idAddition = idAddition; }
    public Double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(Double montantTotal) { this.montantTotal = montantTotal; }
    public Double getReduction() { return reduction; }
    public void setReduction(Double reduction) { this.reduction = reduction; }
    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDateTime getHeurePaiement() { return heurePaiement; }
    public void setHeurePaiement(LocalDateTime heurePaiement) { this.heurePaiement = heurePaiement; }
    public TableRestaurant getTable() { return table; }
    public void setTable(TableRestaurant table) { this.table = table; }
    public Utilisateur getAdmin() { return admin; }
    public void setAdmin(Utilisateur admin) { this.admin = admin; }
}
