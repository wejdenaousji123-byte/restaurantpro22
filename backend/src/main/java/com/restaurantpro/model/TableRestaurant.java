package com.restaurantpro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "table_restaurant")
public class TableRestaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_table")
    private Long idTable;

    @Column(nullable = false, unique = true)
    private Integer numero;

    @Column(nullable = false)
    private Integer capacite;

    @Column(length = 100)
    private String emplacement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.LIBRE;

    @Column(name = "nb_personnes")
    private Integer nbPersonnes = 0;

    @Column(name = "demande_addition", nullable = false)
    private Boolean demandeAddition = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_serveur")
    private Utilisateur serveur;

    public enum Statut { LIBRE, OCCUPEE, PAYEE }

    public TableRestaurant() {}

    public Long getIdTable() { return idTable; }
    public void setIdTable(Long idTable) { this.idTable = idTable; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public Integer getCapacite() { return capacite; }
    public void setCapacite(Integer capacite) { this.capacite = capacite; }
    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public Integer getNbPersonnes() { return nbPersonnes; }
    public void setNbPersonnes(Integer nbPersonnes) { this.nbPersonnes = nbPersonnes; }
    public Boolean getDemandeAddition() { return demandeAddition != null && demandeAddition; }
    public void setDemandeAddition(Boolean demandeAddition) { this.demandeAddition = demandeAddition; }
    public Utilisateur getServeur() { return serveur; }
    public void setServeur(Utilisateur serveur) { this.serveur = serveur; }
}
