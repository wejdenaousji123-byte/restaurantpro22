package com.restaurantpro.repository;

import com.restaurantpro.model.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByCategorie(Article.Categorie categorie);
    List<Article> findByDisponible(boolean disponible);
    boolean existsByNom(String nom);
}
