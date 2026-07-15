package com.restaurantpro.controller;

import com.restaurantpro.model.Article;
import com.restaurantpro.model.LigneCommande;
import com.restaurantpro.repository.ArticleRepository;
import com.restaurantpro.repository.CommandeRepository;
import com.restaurantpro.repository.LigneCommandeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*")
public class ArticleController {

    private final ArticleRepository articleRepo;
    private final CommandeRepository commandeRepo;
    private final LigneCommandeRepository ligneCommandeRepo;

    public ArticleController(ArticleRepository articleRepo,
                              CommandeRepository commandeRepo,
                              LigneCommandeRepository ligneCommandeRepo) {
        this.articleRepo       = articleRepo;
        this.commandeRepo      = commandeRepo;
        this.ligneCommandeRepo = ligneCommandeRepo;
    }

    @GetMapping
    public List<Article> getAll() { return articleRepo.findAll(); }

    @GetMapping("/disponibles")
    public List<Article> getDisponibles() { return articleRepo.findByDisponible(true); }

    @GetMapping("/categorie/{cat}")
    public List<Article> getByCategorie(@PathVariable String cat) {
        return articleRepo.findByCategorie(Article.Categorie.valueOf(cat.toUpperCase()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Article article) {
        if (articleRepo.existsByNom(article.getNom()))
            return ResponseEntity.badRequest().body(Map.of("error", "Article avec ce nom existe déjà"));
        return ResponseEntity.ok(articleRepo.save(article));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Article updated) {
        Article a = articleRepo.findById(id).orElseThrow();

        boolean nomChange       = !a.getNom().equals(updated.getNom());
        boolean categorieChange = !a.getCategorie().equals(updated.getCategorie());
        boolean descChange      = updated.getDescription() != null && !updated.getDescription().equals(a.getDescription());

        if ((nomChange || categorieChange || descChange) && commandeRepo.countCommandesActivesWithArticle(id) > 0)
            return ResponseEntity.badRequest().body(Map.of("error",
                "Impossible de modifier le nom, la catégorie ou la description de cet article : il est présent dans une commande en cours. Seul le prix peut être modifié."));

        a.setNom(updated.getNom());
        a.setCategorie(updated.getCategorie());
        a.setPrix(updated.getPrix());
        a.setDisponible(updated.getDisponible());
        a.setDescription(updated.getDescription());
        if (updated.getImageUrl() != null) a.setImageUrl(updated.getImageUrl());
        return ResponseEntity.ok(articleRepo.save(a));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();

        if (commandeRepo.countCommandesActivesWithArticle(id) > 0)
            return ResponseEntity.badRequest().body(Map.of("error",
                "Impossible de supprimer cet article : il est présent dans une commande en cours."));

        List<LigneCommande> lignes = ligneCommandeRepo.findByArticle(a);
        for (LigneCommande l : lignes) {
            // On garde une trace du nom avant de couper le lien vers l'article :
            // les commandes déjà servies doivent continuer à afficher ce plat.
            if (l.getNomArticle() == null) l.setNomArticle(a.getNom());
            l.setArticle(null);
            ligneCommandeRepo.save(l);
        }

        articleRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Article supprimé"));
    }

    @PatchMapping("/{id}/disponibilite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleDispo(@PathVariable Long id) {
        Article a = articleRepo.findById(id).orElseThrow();
        a.setDisponible(!a.getDisponible());
        return ResponseEntity.ok(articleRepo.save(a));
    }
}
