package com.restaurantpro.fx.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpro.fx.MainApp;
import com.restaurantpro.fx.util.ApiClient;
import com.restaurantpro.fx.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.util.*;

public class ServeurDashboardController {
    @FXML private TextField notesCommandeField;
    @FXML private Label nomServeurLabel;
    @FXML private Button btnNC, btnMC;
    @FXML private StackPane mainStack;
    @FXML private HBox paneNC;
    @FXML private VBox paneMC;
    @FXML private GridPane planSalle;
    @FXML private Label tableSelectionneeLabel;
    @FXML private ListView<String> menuListView, panierListView;
    @FXML private TextField nbPersonnesField;
    @FXML private Label totalLabel;
    @FXML private TableView<Map<String,Object>> mesCommandesTable;
    @FXML private TableColumn<Map<String,Object>,String> colMCTable, colMCStatut, colMCHeure;

    private final ObjectMapper mapper = new ObjectMapper();
    private ObservableList<String> menuItems   = FXCollections.observableArrayList();
    private ObservableList<String> panierItems = FXCollections.observableArrayList();
    private ObservableList<Map<String,Object>> mesCommandes = FXCollections.observableArrayList();

    private Long tableSelectionneeId = null;
    private List<Map<String,Object>> articlesDisponibles = new ArrayList<>();
    private List<Map<String,Object>> panier = new ArrayList<>();
    private double total = 0.0;
    private String idCommandeEnModification = null;
    @FXML
    public void initialize() {
        nomServeurLabel.setText(SessionManager.getInstance().getNom());
        menuListView.setItems(menuItems);
        panierListView.setItems(panierItems);
        setupMesCommandes();
        showNouvelleCommande();
        loadPlanSalle();
        loadMenu();
        loadMesCommandes();
    }

    private void showPane(Node pane) {
        paneNC.setVisible(false);
        paneMC.setVisible(false);
        pane.setVisible(true);
    }

    @FXML public void showNouvelleCommande() {
        showPane(paneNC);
        btnNC.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        btnMC.setStyle("-fx-background-color:transparent;-fx-text-fill:#ccc;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
    }

    @FXML public void showMesCommandes() {
        showPane(paneMC);
        btnMC.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        btnNC.setStyle("-fx-background-color:transparent;-fx-text-fill:#ccc;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadMesCommandes();
    }

    private void loadPlanSalle() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/tables");
                List<Map<String,Object>> tables = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> buildPlanSalle(tables));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void buildPlanSalle(List<Map<String,Object>> tables) {
        planSalle.getChildren().clear();
        int col = 0, row = 0;
        for (Map<String,Object> t : tables) {
            VBox card = createTableCard(t);
            planSalle.add(card, col, row);
            col++; if (col >= 4) { col = 0; row++; }
        }
    }

    private VBox createTableCard(Map<String,Object> t) {
        boolean libre = "LIBRE".equals(t.get("statut").toString());
        VBox card = new VBox(5);
        card.setPrefSize(130, 95);
        card.setStyle("-fx-background-color:" + (libre ? "#1e8449" : "#c0392b")
                + ";-fx-background-radius:10;-fx-padding:10;");

        Label num  = new Label("Table " + t.get("numero"));
        num.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13;");

        Label cap  = new Label("Capacite : " + t.get("capacite"));
        cap.setStyle("-fx-text-fill:white;-fx-font-size:11;");

        String zone = t.get("emplacement") != null ? t.get("emplacement").toString() : ".";
        Label emp = new Label(zone);
        emp.setStyle("-fx-text-fill:#ddd; -fx-font-size:11; -fx-font-style:italic;");

        Label stat = new Label(libre ? "Libre" : "Occupee");
        stat.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11;");

        card.getChildren().addAll(num, cap, emp, stat);

        Long idTable = Long.valueOf(t.get("idTable").toString());
        if (libre) {
            card.setStyle(card.getStyle() + "-fx-cursor:hand;");
            card.setOnMouseClicked(e -> {
                tableSelectionneeId = idTable;
                tableSelectionneeLabel.setText("Table " + t.get("numero")
                        + " selectionnee (cap. " + t.get("capacite") + ")");
                tableSelectionneeLabel.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;-fx-font-size:13;");
            });
        }
        return card;
    }
    private void loadMenu() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/articles/disponibles");
                articlesDisponibles = mapper.readValue(json, new TypeReference<>(){});
                List<String> noms = articlesDisponibles.stream()
                        .map(a -> a.get("nom") + "  -  " + a.get("prix") + " TND"
                                + (a.get("description") != null && !a.get("description").toString().isEmpty()
                                ? "  [" + a.get("description").toString().substring(0,
                                    Math.min(30, a.get("description").toString().length())) + "]" : ""))
                        .toList();
                Platform.runLater(() -> menuItems.setAll(noms));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void ajouterAuPanier() {
        int idx = menuListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        Map<String,Object> article = articlesDisponibles.get(idx);
        panier.add(article);
        total += Double.parseDouble(article.get("prix").toString());
        panierItems.add(article.get("nom") + "  -  " + article.get("prix") + " TND");
        totalLabel.setText(String.format("Total : %.3f TND", total));
    }

    @FXML void retirerDuPanier() {
        int idx = panierListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        total -= Double.parseDouble(panier.get(idx).get("prix").toString());
        panier.remove(idx);
        panierItems.remove(idx);
        totalLabel.setText(String.format("Total : %.3f TND", total));
    }

    @FXML void ouvrirEtCommander() {
        if (tableSelectionneeId == null) { showAlert("Selectionnez une table d'abord."); return; }
        if (panier.isEmpty())            { showAlert("Le panier est vide.");             return; }
        String nbStr = nbPersonnesField.getText().trim();
        if (nbStr.isEmpty())             { showAlert("Indiquez le nombre de personnes."); return; }

        int nbPers;
        try { nbPers = Integer.parseInt(nbStr); }
        catch (NumberFormatException e) { showAlert("Nombre de personnes invalide."); return; }
        if (nbPers <= 0) { showAlert("Le nombre de personnes doit être supérieur à 0."); return; }

        if (panier.size() < nbPers) {
            showAlert("⚠ Le nombre d'articles (" + panier.size() + ") doit être ≥ au nombre de personnes (" + nbPers + ").");
            return;
        }

        final int nbPersFinal = nbPers;

        Map<Long,Integer> compteur = new LinkedHashMap<>();
        for (Map<String,Object> a : panier) {
            Long id = Long.valueOf(a.get("idArticle").toString());
            compteur.merge(id, 1, Integer::sum);
        }
        List<Map<String,Object>> lignes = new ArrayList<>();
        for (Map.Entry<Long,Integer> e : compteur.entrySet()) {
            Map<String,Object> l = new HashMap<>();
            l.put("idArticle", e.getKey());
            l.put("quantite",  e.getValue());
            lignes.add(l);
        }

        if (idCommandeEnModification != null) {
            final String idModif = idCommandeEnModification;
            final List<Map<String,Object>> lignesFinal = lignes;
            new Thread(() -> {
                try {
                    Map<String,Object> body = new LinkedHashMap<>();
                    body.put("lignes",      lignesFinal);
                    body.put("notes",       notesCommandeField.getText().trim());
                    body.put("nbPersonnes", nbPersFinal);
                    String resp = ApiClient.put("/commandes/" + idModif, body);
                    if (resp != null && resp.contains("\"error\"")) {
                        String msg = extraireMessageErreur(resp);
                        Platform.runLater(() -> showAlert("⚠ " + msg));
                        return;
                    }
                    Platform.runLater(() -> {
                        idCommandeEnModification = null;
                        panier.clear(); panierItems.clear();
                        total = 0; totalLabel.setText("Total : 0.000 TND");
                        tableSelectionneeId = null;
                        tableSelectionneeLabel.setText("Aucune table selectionnee");
                        tableSelectionneeLabel.setStyle("-fx-text-fill:#f39c12;-fx-font-weight:bold;-fx-font-size:13;");
                        nbPersonnesField.clear(); notesCommandeField.clear();
                        showAlert("Commande mise à jour !");
                        loadPlanSalle();
                        loadMesCommandes();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert("Erreur : " + e.getMessage()));
                }
            }).start();
            return;
        }

        final List<Map<String,Object>> lignesFinal = lignes;
        new Thread(() -> {
            try {
                Map<String,Object> ouvrirBody = new HashMap<>();
                ouvrirBody.put("nbPersonnes", nbPersFinal);
                ouvrirBody.put("idServeur", SessionManager.getInstance().getUserId());
                String ouvrirResp = ApiClient.put("/tables/" + tableSelectionneeId + "/ouvrir", ouvrirBody);

                if (ouvrirResp != null && ouvrirResp.contains("\"error\"")) {
                    String msg = extraireMessageErreur(ouvrirResp);
                    Platform.runLater(() -> showAlert("⚠ " + msg));
                    return;
                }

                Map<String,Object> cmdBody = new LinkedHashMap<>();
                cmdBody.put("idTable",   tableSelectionneeId);
                cmdBody.put("idServeur", SessionManager.getInstance().getUserId());
                cmdBody.put("lignes",    lignesFinal);
                cmdBody.put("notes",     notesCommandeField.getText().trim());

                String cmdResp = ApiClient.post("/commandes", cmdBody);
                if (cmdResp != null && cmdResp.contains("\"error\"")) {
                    String msg = extraireMessageErreur(cmdResp);
                    Platform.runLater(() -> showAlert("⚠ Erreur commande : " + msg));
                    return;
                }

                Platform.runLater(() -> {
                    panier.clear(); panierItems.clear();
                    total = 0; totalLabel.setText("Total : 0.000 TND");
                    tableSelectionneeId = null;
                    tableSelectionneeLabel.setText("Aucune table selectionnee");
                    tableSelectionneeLabel.setStyle("-fx-text-fill:#f39c12;-fx-font-weight:bold;-fx-font-size:13;");
                    nbPersonnesField.clear(); notesCommandeField.clear();
                    loadPlanSalle(); loadMesCommandes();
                    showAlert("Commande envoyee en cuisine !");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur : " + e.getMessage()));
            }
        }).start();
    }

    private String extraireMessageErreur(String json) {
        try {
            Map<String,Object> m = mapper.readValue(json, new TypeReference<>(){});
            return m.getOrDefault("error", json).toString();
        } catch (Exception e) { return json; }
    }

    private void setupMesCommandes() {
        colMCTable.setCellValueFactory(c -> {
            Object t = c.getValue().get("table");
            if (t instanceof Map) return new SimpleStringProperty("Table " + ((Map<?,?>)t).get("numero"));
            return new SimpleStringProperty("--");
        });
        colMCStatut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrDefault("statut","").toString()));
        colMCHeure.setCellValueFactory(c -> {
            Object h = c.getValue().get("heureCreation");
            if (h != null) return new SimpleStringProperty(h.toString().substring(0,16).replace("T"," "));
            return new SimpleStringProperty("--");
        });
        mesCommandesTable.setItems(mesCommandes);

        mesCommandesTable.setRowFactory(tv -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Map<String, Object> commandeSelectionnee = row.getItem();
                    ouvrirFenetreDetailsCommande(commandeSelectionnee);
                }
            });
            return row;
        });
    }

    private void loadMesCommandes() {
        Long id = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            try {
                String json = ApiClient.get("/commandes/serveur/" + id);
                List<Map<String,Object>> cmds = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> mesCommandes.setAll(cmds));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void demanderAddition() {
        if (tableSelectionneeId == null) { showAlert("Selectionnez d'abord une table occupee."); return; }
        new Thread(() -> {
            try {
                String resp = ApiClient.post("/additions/table/" + tableSelectionneeId + "/calculer", null);
                System.out.println("Addition: " + resp);
                Platform.runLater(() -> showAlert("Demande d'addition envoyee au gerant."));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur: " + e.getMessage()));
            }
        }).start();
    }

    @FXML void rafraichir() {
        loadPlanSalle();
        loadMesCommandes();
        loadMenu();
    }

    @FXML void logout() {
        SessionManager.getInstance().logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg); a.showAndWait();
    }
    private void ouvrirFenetreDetailsCommande(Map<String, Object> cmd) {
        Object idObj = cmd.get("idCommande");
        if (idObj == null) idObj = cmd.get("id");
        if (idObj == null) {
            showAlert("Impossible de trouver l'identifiant de cette commande.");
            return;
        }

        final String idCommande = idObj.toString();
        String statut = cmd.getOrDefault("statut", "INCONNU").toString();

        boolean estModifiable = "EN_ATTENTE".equals(statut);
        boolean estPrete = "PRETE".equals(statut);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails de la Commande #" + idCommande);
        dialog.setHeaderText("Statut actuel : " + statut);

        VBox content = new VBox(10);
        content.setPrefWidth(400);
        content.setStyle("-fx-padding: 15;");

        ListView<String> lignesListView = new ListView<>();
        ObservableList<String> itemsLignes = FXCollections.observableArrayList();
        lignesListView.setItems(itemsLignes);

        Object notesObj = cmd.get("notes");
        Label notesLabel = new Label("Notes : " + (notesObj != null && !notesObj.toString().isEmpty() ? notesObj.toString() : "Aucune note"));
        notesLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #aaa;");

        content.getChildren().addAll(new Label("Plats commandés :"), lignesListView, notesLabel);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        final List<Map<String, Object>>[] lignesCommandeConteneur = new List[]{new ArrayList<>()};

        new Thread(() -> {
            try {
                String json = ApiClient.get("/commandes/" + idCommande);
                if (json != null && !json.trim().isEmpty() && !json.contains("error")) {
                    Map<String, Object> details = mapper.readValue(json, new TypeReference<>(){});
                    lignesCommandeConteneur[0] = (List<Map<String, Object>>) details.get("lignes");
                } else {
                    lignesCommandeConteneur[0] = (List<Map<String, Object>>) cmd.get("lignes");
                }
                Platform.runLater(() -> afficherLesLignes(lignesCommandeConteneur[0], itemsLignes));
            } catch (Exception e) {
                lignesCommandeConteneur[0] = (List<Map<String, Object>>) cmd.get("lignes");
                Platform.runLater(() -> afficherLesLignes(lignesCommandeConteneur[0], itemsLignes));
            }
        }).start();

        if (estModifiable) {
            ButtonType btnModifierType = new ButtonType("✏️ Modifier la Commande", ButtonBar.ButtonData.LEFT);
            ButtonType btnAnnulerType = new ButtonType("❌ Annuler la Commande", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().addAll(0, List.of(btnModifierType, btnAnnulerType));

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnModifierType) {
                    basculerCommandeVersPanier(cmd, lignesCommandeConteneur[0]);
                } else if (result.get() == btnAnnulerType) {
                    gererAnnulationCommande(idCommande);
                }
            }

        } else if (estPrete) {
            ButtonType btnClotureType = new ButtonType("🧾 Demander Clôture Addition", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(0, btnClotureType);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == btnClotureType) {
                gererDemandeClotureAddition(cmd);
            }
        } else {
            dialog.showAndWait();
        }
    }

    private void gererDemandeClotureAddition(Map<String, Object> cmd) {
        Object tableObj = cmd.get("table");
        if (!(tableObj instanceof Map)) { showAlert("Erreur : table introuvable."); return; }
        final String idTable = ((Map<?,?>) tableObj).get("idTable").toString();

        new Thread(() -> {
            try {
                String resp = ApiClient.post("/additions/table/" + idTable + "/calculer", null);
                Platform.runLater(() -> {
                    loadMesCommandes();
                    loadPlanSalle();
                    showAlert("Demande d'addition envoyée au gérant !");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur lors de la demande de clôture."));
            }
        }).start();
    }
    private void basculerCommandeVersPanier(Map<String, Object> cmd, List<Map<String, Object>> lignes) {
        if (lignes == null || lignes.isEmpty()) {
            lignes = (List<Map<String, Object>>) cmd.get("lignes");
        }

        if (lignes == null || lignes.isEmpty()) {
            showAlert("Impossible de charger les articles de cette commande.");
            return;
        }

        Object idObj = cmd.get("idCommande");
        if (idObj == null) idObj = cmd.get("id");
        idCommandeEnModification = (idObj != null) ? idObj.toString() : null;

        Object tableObj = cmd.get("table");
        if (tableObj instanceof Map) {
            Map<?, ?> tMap = (Map<?, ?>) tableObj;
            tableSelectionneeId = Long.valueOf(tMap.get("idTable").toString());
            Platform.runLater(() -> {
                tableSelectionneeLabel.setText("Modification Commande - Table " + tMap.get("numero"));
                tableSelectionneeLabel.setStyle("-fx-text-fill:#f39c12;-fx-font-weight:bold;-fx-font-size:13;");
            });
        } else if (cmd.get("idTable") != null) {
            tableSelectionneeId = Long.valueOf(cmd.get("idTable").toString());
        }

        panier.clear();
        panierItems.clear();
        total = 0.0;

        for (Map<String, Object> l : lignes) {
            Map<String, Object> articleTarget = null;
            Object artObj = l.get("article");

            if (artObj instanceof Map) {
                articleTarget = (Map<String, Object>) artObj;
            } else if (l.get("idArticle") != null) {
                articleTarget = l;
            }

            if (articleTarget != null) {
                Object prixObj = articleTarget.getOrDefault("prix", 0.0);
                Object nomObj = articleTarget.getOrDefault("nom", "Article");
                int qte = 1;
                if (l.get("quantite") != null) {
                    qte = ((Number) l.get("quantite")).intValue();
                }

                for (int i = 0; i < qte; i++) {
                    panier.add(articleTarget);
                    total += Double.parseDouble(prixObj.toString());
                    panierItems.add(nomObj.toString() + "  -  " + prixObj.toString() + " TND");
                }
            }
        }

        final double totalFinal = total;
        final List<String> itemsAAfficher = new ArrayList<>(panierItems);
        Object noteObj = cmd.get("notes");
        final String noteTexte = (noteObj != null) ? noteObj.toString() : "";
        Object nbPersObj = cmd.getOrDefault("nbPersonnes", "2");
        final String nbPersTexte = nbPersObj != null ? nbPersObj.toString() : "";

        Platform.runLater(() -> {
            totalLabel.setText(String.format("Total : %.3f TND", totalFinal));
            panierListView.getItems().setAll(itemsAAfficher);
            if (notesCommandeField != null) {
                notesCommandeField.setText(noteTexte);
            }
            if (nbPersonnesField != null) {
                nbPersonnesField.setText(nbPersTexte);
            }

            showNouvelleCommande();

            showAlert("Commande chargée. Modifiez puis validez !");
        });
    }
    private void afficherLesLignes(List<Map<String, Object>> lignes, ObservableList<String> itemsLignes) {
        List<String> affichageLignes = new ArrayList<>();
        if (lignes != null && !lignes.isEmpty()) {
            double totalCmd = 0.0;
            for (Map<String, Object> l : lignes) {
                // 1. Récupérer le nom
                Object artObj = l.get("article");
                String nomPlat = (artObj instanceof Map) ? ((Map<?,?>) artObj).get("nom").toString() : "Article";

                // 2. Récupérer le prix unitaire (en testant les deux variantes possibles)
                double prixUnit = 0.0;
                if (l.get("prixUnitaire") != null) {
                    prixUnit = Double.parseDouble(l.get("prixUnitaire").toString());
                } else if (l.get("prix_unitaire") != null) {
                    prixUnit = Double.parseDouble(l.get("prix_unitaire").toString());
                }

                int qte = ((Number) l.getOrDefault("quantite", 1)).intValue();
                double sousTotal = prixUnit * qte;
                totalCmd += sousTotal;

                affichageLignes.add(nomPlat + "  x" + qte + "   →  " + String.format("%.3f TND", sousTotal));
            }
            affichageLignes.add("─────────────────────────────");
            affichageLignes.add("TOTAL :  " + String.format("%.3f TND", totalCmd));
        } else {
            affichageLignes.add("Aucun article trouvé.");
        }
        Platform.runLater(() -> itemsLignes.setAll(affichageLignes));
    }

    private void gererModificationNotes(String idCommande, String noteActuelle) {
        TextInputDialog dialog = new TextInputDialog(noteActuelle);
        dialog.setTitle("Modifier les instructions");
        dialog.setHeaderText("Instructions pour la cuisine (Commande #" + idCommande + ")");
        dialog.setContentText("Notes :");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("notes", result.get());

            new Thread(() -> {
                try {
                    ApiClient.put("/commandes/" + idCommande + "/notes", body);
                    Platform.runLater(() -> {
                        loadMesCommandes();
                        showAlert("Notes de la commande mises à jour avec succès !");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert("Erreur lors de la modification des notes."));
                }
            }).start();
        }
    }

    private void gererAnnulationCommande(String idCommande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment annuler la commande #" + idCommande + " ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation d'annulation");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        ApiClient.delete("/commandes/" + idCommande);
                        Platform.runLater(() -> {
                            loadMesCommandes();
                            showAlert("Commande annulée.");
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> showAlert("Erreur lors de l'annulation."));
                    }
                }).start();
            }
        });
    }
}
