package com.restaurantpro.repository;

import com.restaurantpro.model.Article;
import com.restaurantpro.model.LigneCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
    List<LigneCommande> findByArticle(Article article);
}
