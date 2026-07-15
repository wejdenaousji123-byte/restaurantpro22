package com.restaurantpro.repository;

import com.restaurantpro.model.Commande;
import com.restaurantpro.model.TableRestaurant;
import com.restaurantpro.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByStatut(Commande.Statut statut);
    List<Commande> findByTable(TableRestaurant table);
    List<Commande> findByStatutIn(List<Commande.Statut> statuts);

    @Query("SELECT COUNT(c) FROM Commande c JOIN c.lignes l WHERE l.article.idArticle = :idArticle AND c.statut IN ('EN_ATTENTE','EN_PREPARATION','PRETE')")
    long countCommandesActivesWithArticle(@Param("idArticle") Long idArticle);
    List<Commande> findByTableAndStatutNot(TableRestaurant table, Commande.Statut statut);

    @Query("SELECT DISTINCT c FROM Commande c LEFT JOIN FETCH c.lignes l LEFT JOIN FETCH l.article WHERE c.serveur = :serveur ORDER BY c.heureCreation DESC")
    List<Commande> findByServeurWithLignes(Utilisateur serveur);

    @Query("SELECT DISTINCT c FROM Commande c LEFT JOIN FETCH c.lignes l LEFT JOIN FETCH l.article WHERE c.statut IN ('EN_ATTENTE','EN_PREPARATION','PRETE')")
    List<Commande> findActiveWithLignes();

    @Query("SELECT c FROM Commande c WHERE c.table.idTable = :tableId AND c.statut NOT IN ('SERVIE','ANNULEE')")
    List<Commande> findActiveByTableId(Long tableId);
    @Query("SELECT DISTINCT c FROM Commande c LEFT JOIN FETCH c.lignes l LEFT JOIN FETCH l.article ORDER BY c.heureCreation DESC")
    List<Commande> findAllForGerant();
    @Query("SELECT SUM(l.prixUnitaire * l.quantite) FROM Commande c JOIN c.lignes l WHERE c.statut = 'PAYEE' AND FUNCTION('MONTH', c.heureCreation) = FUNCTION('MONTH', CURRENT_DATE) AND FUNCTION('YEAR', c.heureCreation) = FUNCTION('YEAR', CURRENT_DATE)")
    Double getChiffreAffairesMois();

    @Query("SELECT SUM(l.prixUnitaire * l.quantite) FROM Commande c JOIN c.lignes l WHERE c.statut = 'PAYEE' AND FUNCTION('YEAR', c.heureCreation) = FUNCTION('YEAR', CURRENT_DATE)")
    Double getChiffreAffairesAnnee();

    @Query(value = "SELECT COALESCE(a.nom, l.nom_article) as nom, SUM(l.quantite) as total " +
            "FROM ligne_commande l " +
            "LEFT JOIN article a ON l.id_article = a.id_article " +
            "JOIN commande c ON l.id_commande = c.id_commande " +
            "WHERE c.statut = 'SERVIE' AND COALESCE(a.nom, l.nom_article) IS NOT NULL " +
            "GROUP BY COALESCE(a.nom, l.nom_article) " +
            "ORDER BY total DESC", nativeQuery = true)
    List<Object[]> getTopArticles();
}
