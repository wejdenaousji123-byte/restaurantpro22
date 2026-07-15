package com.restaurantpro.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "ligne_commande")
public class LigneCommande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ligne")
    private Long idLigne;

    @Column(nullable = false)
    private Integer quantite = 1;

    @Column(name = "note_speciale", length = 255)
    private String noteSpeciale;

    @Column(name = "prix_unitaire", nullable = false)
    private Double prixUnitaire;

    // Copie figée du nom de l'article au moment de la commande.
    // Permet de garder l'historique lisible même si l'article est
    // supprimé du menu par la suite (l'article n'est alors plus jamais
    // perdu des commandes déjà servies).
    @Column(name = "nom_article", length = 150)
    private String nomArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_commande", nullable = false)
    @JsonIgnore
    private Commande commande;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_article")
    private Article article;

    public LigneCommande() {}

    public Long getIdLigne() { return idLigne; }
    public void setIdLigne(Long idLigne) { this.idLigne = idLigne; }
    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }
    public String getNoteSpeciale() { return noteSpeciale; }
    public void setNoteSpeciale(String noteSpeciale) { this.noteSpeciale = noteSpeciale; }
    public Double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public String getNomArticle() { return nomArticle; }
    public void setNomArticle(String nomArticle) { this.nomArticle = nomArticle; }
    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
}
