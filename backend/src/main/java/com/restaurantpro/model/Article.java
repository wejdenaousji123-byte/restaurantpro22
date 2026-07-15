package com.restaurantpro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_article")
    private Long idArticle;

    @Column(nullable = false, unique = true, length = 150)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    @Column(nullable = false)
    private Double prix;

    @Column(nullable = false)
    private Boolean disponible = true;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin")
    private Utilisateur admin;

    public enum Categorie { ENTREE, PLAT, DESSERT, BOISSON, AUTRE }

    public Article() {}

    public Long getIdArticle() { return idArticle; }
    public void setIdArticle(Long idArticle) { this.idArticle = idArticle; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }
    public Double getPrix() { return prix; }
    public void setPrix(Double prix) { this.prix = prix; }
    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Utilisateur getAdmin() { return admin; }
    public void setAdmin(Utilisateur admin) { this.admin = admin; }
}
