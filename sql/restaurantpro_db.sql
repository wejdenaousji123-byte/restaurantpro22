

CREATE DATABASE IF NOT EXISTS restaurantpro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurantpro;


CREATE TABLE utilisateur (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role        ENUM('ADMIN','SERVEUR','CUISINIER') NOT NULL,
    actif       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gerant (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_gerant_user FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE serveur (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_serveur_user FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE cuisinier (
    id BIGINT PRIMARY KEY,
    CONSTRAINT fk_cuisinier_user FOREIGN KEY (id) REFERENCES utilisateur(id) ON DELETE CASCADE
);


CREATE TABLE table_restaurant (
    id_table    BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero      INT NOT NULL UNIQUE,
    capacite    INT NOT NULL,
    statut      ENUM('LIBRE','OCCUPEE') NOT NULL DEFAULT 'LIBRE',
    nb_personnes INT DEFAULT 0,
    id_serveur  BIGINT,
    CONSTRAINT fk_table_serveur FOREIGN KEY (id_serveur) REFERENCES utilisateur(id) ON DELETE SET NULL
);


CREATE TABLE article (
    id_article  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(150) NOT NULL UNIQUE,
    categorie   ENUM('ENTREE','PLAT','DESSERT','BOISSON','AUTRE') NOT NULL,
    prix        DECIMAL(8,3) NOT NULL,
    disponible  BOOLEAN NOT NULL DEFAULT TRUE,
    image_url   VARCHAR(255),
    description TEXT,
    id_admin    BIGINT,
    CONSTRAINT fk_article_admin FOREIGN KEY (id_admin) REFERENCES utilisateur(id) ON DELETE SET NULL
);


CREATE TABLE commande (
    id_commande     BIGINT AUTO_INCREMENT PRIMARY KEY,
    statut          ENUM('EN_ATTENTE','EN_PREPARATION','PRETE','SERVIE','ANNULEE') NOT NULL DEFAULT 'EN_ATTENTE',
    heure_creation  DATETIME DEFAULT CURRENT_TIMESTAMP,
    notes           TEXT,
    id_table        BIGINT NOT NULL,
    id_serveur      BIGINT NOT NULL,
    CONSTRAINT fk_commande_table   FOREIGN KEY (id_table)   REFERENCES table_restaurant(id_table),
    CONSTRAINT fk_commande_serveur FOREIGN KEY (id_serveur) REFERENCES utilisateur(id)
);


CREATE TABLE ligne_commande (
    id_ligne        BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantite        INT NOT NULL DEFAULT 1,
    note_speciale   VARCHAR(255),
    prix_unitaire   DECIMAL(8,3) NOT NULL,
    nom_article     VARCHAR(150), -- copie figée du nom au moment de la commande (survit à la suppression de l'article)
    id_commande     BIGINT NOT NULL,
    id_article      BIGINT NULL,  -- peut devenir NULL si l'article est supprimé du menu ; nom_article garde l'historique lisible
    CONSTRAINT fk_ligne_commande  FOREIGN KEY (id_commande) REFERENCES commande(id_commande) ON DELETE CASCADE,
    CONSTRAINT fk_ligne_article   FOREIGN KEY (id_article)  REFERENCES article(id_article) ON DELETE SET NULL
);


CREATE TABLE addition (
    id_addition     BIGINT AUTO_INCREMENT PRIMARY KEY,
    montant_total   DECIMAL(10,3) NOT NULL DEFAULT 0,
    reduction       DECIMAL(5,2) NOT NULL DEFAULT 0,
    mode_paiement   ENUM('ESPECES','CARTE','AUTRE') DEFAULT NULL,
    statut          ENUM('EN_COURS','PAYEE') NOT NULL DEFAULT 'EN_COURS',
    heure_paiement  DATETIME DEFAULT NULL,
    id_table        BIGINT NOT NULL UNIQUE,
    id_admin        BIGINT,
    CONSTRAINT fk_addition_table FOREIGN KEY (id_table)  REFERENCES table_restaurant(id_table),
    CONSTRAINT fk_addition_admin FOREIGN KEY (id_admin)  REFERENCES utilisateur(id) ON DELETE SET NULL
);


CREATE TABLE notification (
    id_notif        BIGINT AUTO_INCREMENT PRIMARY KEY,
    message         VARCHAR(500) NOT NULL,
    type            ENUM('COMMANDE_PRETE','NOUVELLE_COMMANDE','COMMANDE_MODIFIEE','COMMANDE_ANNULEE','ADDITION') NOT NULL,
    lue             BOOLEAN NOT NULL DEFAULT FALSE,
    heure_creation  DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_destinataire BIGINT NOT NULL,
    id_commande     BIGINT,
    CONSTRAINT fk_notif_user     FOREIGN KEY (id_destinataire) REFERENCES utilisateur(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_commande FOREIGN KEY (id_commande)     REFERENCES commande(id_commande) ON DELETE SET NULL
);



INSERT INTO utilisateur (nom, email, mot_de_passe, role) VALUES
('Gérant Principal', 'admin@restaurantpro.tn', '$2a$12$qGdDAijGJpNiobPAJT.yiOeUQRNzlLx/FJFjomhvBNHReMJNFc4UO', 'ADMIN');
INSERT INTO gerant (id) VALUES (LAST_INSERT_ID());

INSERT INTO utilisateur (nom, email, mot_de_passe, role) VALUES
('Ahmed Ben Ali',  'ahmed@restaurantpro.tn',  '$2a$12$qGdDAijGJpNiobPAJT.yiOeUQRNzlLx/FJFjomhvBNHReMJNFc4UO', 'SERVEUR'),
('Fatma Riahi',    'fatma@restaurantpro.tn',   '$2a$12$qGdDAijGJpNiobPAJT.yiOeUQRNzlLx/FJFjomhvBNHReMJNFc4UO', 'SERVEUR');
INSERT INTO serveur (id) SELECT id FROM utilisateur WHERE email IN ('ahmed@restaurantpro.tn','fatma@restaurantpro.tn');

INSERT INTO utilisateur (nom, email, mot_de_passe, role) VALUES
('Mohamed Sassi', 'cuisinier@restaurantpro.tn', '$2a$12$qGdDAijGJpNiobPAJT.yiOeUQRNzlLx/FJFjomhvBNHReMJNFc4UO', 'CUISINIER');
INSERT INTO cuisinier (id) SELECT id FROM utilisateur WHERE email = 'cuisinier@restaurantpro.tn';

INSERT INTO table_restaurant (numero, capacite) VALUES
(1,2),(2,2),(3,4),(4,4),(5,4),(6,6),(7,6),(8,8),(9,8),(10,10);

INSERT INTO article (nom, categorie, prix, description) VALUES
('Salade Mechouia',   'ENTREE', 4.500, 'Salade grillée traditionnelle tunisienne'),
('Brik à l\'œuf',    'ENTREE', 3.500, 'Pâte filo croustillante avec œuf et thon'),
('Soupe Tunisienne',  'ENTREE', 3.000, 'Soupe harissa, tomates et pâtes');

INSERT INTO article (nom, categorie, prix, description) VALUES
('Couscous Agneau',   'PLAT', 14.000, 'Couscous traditionnel semoule fine, légumes et viande agneau'),
('Ojja Merguez',      'PLAT', 11.500, 'Œufs pochés en sauce tomate avec merguez'),
('Poisson Grillé',    'PLAT', 18.000, 'Poisson du jour mariné et grillé au charbon'),
('Kafteji',           'PLAT',  9.000, 'Légumes frits avec œuf et thon');

INSERT INTO article (nom, categorie, prix, description) VALUES
('Makroudh',    'DESSERT', 2.500, 'Gâteau semoule dattes et miel'),
('Baklawa',     'DESSERT', 3.000, 'Feuilletés aux pistaches et sirop de fleur d\'oranger'),
('Assiette Fruits', 'DESSERT', 4.000, 'Sélection de fruits frais de saison');

INSERT INTO article (nom, categorie, prix, description) VALUES
('Eau Minérale 50cl', 'BOISSON', 1.000, 'Eau minérale bouteille'),
('Jus Orange Pressé', 'BOISSON', 3.500, 'Orange pressée fraîche'),
('Café Espresso',     'BOISSON', 1.500, 'Expresso serré'),
('Thé à la Menthe',   'BOISSON', 1.500, 'Thé vert à la menthe fraîche'),
('Limonade Maison',   'BOISSON', 2.500, 'Citronnade fraîche maison');
