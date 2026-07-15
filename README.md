# RestaurantPro — Guide d'installation et d'exécution

## 🏗 Architecture du projet

```
restaurantpro/
├── sql/
│   └── restaurantpro_db.sql        ← Script de création de la BD
├── backend/                        ← API REST Spring Boot (Java 17)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/restaurantpro/
│       │   ├── RestaurantProApplication.java
│       │   ├── model/              ← Entités JPA
│       │   ├── repository/         ← Repositories Spring Data
│       │   ├── controller/         ← Endpoints REST
│       │   ├── security/           ← JWT + Spring Security
│       │   └── config/
│       └── resources/
│           └── application.properties
└── frontend/                       ← Interface JavaFX
    ├── pom.xml
    └── src/main/
        ├── java/com/restaurantpro/fx/
        │   ├── MainApp.java
        │   ├── controller/         ← Controllers JavaFX
        │   └── util/               ← ApiClient, SessionManager
        └── resources/
            ├── fxml/               ← Interfaces FXML
            └── css/style.css
```

---

## ⚙ Prérequis

| Outil         | Version minimale |
|---------------|-----------------|
| Java (JDK)    | 17+             |
| Maven         | 3.8+            |
| MySQL         | 8.0+            |
| IntelliJ IDEA | 2023+ (recommandé) |

---

## 🗃 Étape 1 — Créer la base de données

```sql
-- Dans MySQL Workbench ou ligne de commande :
mysql -u root -p < sql/restaurantpro_db.sql
```

Cela crée :
- La base `restaurantpro`
- Toutes les tables (utilisateur, table_restaurant, article, commande…)
- Des données de test (admin, 2 serveurs, 1 cuisinier, 10 tables, 15 articles du menu)

### Comptes de démonstration

| Email                          | Mot de passe | Rôle       |
|-------------------------------|--------------|------------|
| admin@restaurantpro.tn        | admin123     | Admin      |
| ahmed@restaurantpro.tn        | admin123     | Serveur    |
| fatma@restaurantpro.tn        | admin123     | Serveur    |
| cuisinier@restaurantpro.tn    | admin123     | Cuisinier  |

---

## 🚀 Étape 2 — Lancer le Backend

```bash
cd backend
# Modifier src/main/resources/application.properties si besoin
# (URL MySQL, username, password)

mvn spring-boot:run
```

Le serveur démarre sur `http://localhost:8080`

### Endpoints principaux

| Méthode | URL                                  | Rôle requis        |
|---------|--------------------------------------|--------------------|
| POST    | /api/auth/login                      | Public             |
| GET     | /api/tables                          | Tous               |
| PUT     | /api/tables/{id}/ouvrir              | Admin, Serveur     |
| PUT     | /api/tables/{id}/liberer             | Admin, Serveur     |
| GET     | /api/articles                        | Tous               |
| POST    | /api/articles                        | Admin              |
| POST    | /api/commandes                       | Admin, Serveur     |
| GET     | /api/commandes/actives               | Tous               |
| PUT     | /api/commandes/{id}/statut           | Admin, Cuisinier   |
| POST    | /api/additions/table/{id}/calculer   | Admin, Serveur     |
| PUT     | /api/additions/{id}/payer            | Admin              |
| GET     | /api/stats/dashboard                 | Admin              |

---

## 🖥 Étape 3 — Lancer le Frontend JavaFX

```bash
cd frontend
mvn javafx:run
```

---

## 🔄 Flux d'utilisation typique

```
1. Serveur se connecte
2. Serveur clique sur une table libre → saisit nb. personnes
3. Serveur sélectionne des articles → Envoie en cuisine
4. Cuisinier voit la commande → Passe à "En préparation"
5. Cuisinier clique "Prête" → Serveur est notifié
6. Serveur demande l'addition
7. Admin voit la demande → Choisit mode paiement → Clôture
8. La table est automatiquement libérée
```

---

## 📦 Technologies utilisées

| Couche     | Technologie                        |
|------------|------------------------------------|
| Backend    | Java 17, Spring Boot 3.2, Maven    |
| Sécurité   | Spring Security, JWT (JJWT 0.11)   |
| ORM        | Hibernate / Spring Data JPA        |
| Base de données | MySQL 8.0                    |
| Frontend   | JavaFX 21, FXML, CSS               |
| HTTP Client | Java 11 HttpClient                |
| JSON       | Jackson ObjectMapper               |

---

*RestaurantPro — Projet d'intégration 2025/2026*  
*Ousji Wejdene & Ajabi Wael*
