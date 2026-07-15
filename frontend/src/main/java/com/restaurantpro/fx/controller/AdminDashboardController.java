package com.restaurantpro.fx.controller;

import javafx.scene.control.TextInputDialog;
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

public class AdminDashboardController {
    @FXML private Button btnAdditions;
    @FXML private GridPane grilleTablesAdditions;
    @FXML private VBox containerFacture;
    @FXML private Label labelSelectionTableMessage;
    @FXML private VBox paneAdditions;
    @FXML private Label nomAdminLabel;
    @FXML private Label caMoisLabel, caAnneeLabel, bestSellerLabel, bestSellerQteLabel;
    @FXML private VBox paneNotifications, listeNotifications;
    @FXML private Button btnNotifications;
    @FXML private Label badgeNotifCount;

    @FXML private VBox paneUtilisateurs;
    @FXML private Button btnUtilisateurs;
    @FXML private TextField uNomField, uEmailField;
    @FXML private PasswordField uMotDePasseField;
    @FXML private ComboBox<String> uRoleBox;
    @FXML private Label utilisateurActionLabel;
    @FXML private TableView<Map<String,Object>> utilisateursTable;
    @FXML private TableColumn<Map<String,Object>,String> colUNom, colUEmail, colURole, colUActif;
    @FXML private TableColumn<Map<String,Object>,Void> colUActions, colUMotDePasse;
    @FXML private Button btnTables, btnMenu, btnCommandes, btnStats;

    @FXML private TextField tNumeroField, tCapaciteField, tEmplacementField;
    @FXML private Label tableActionLabel, tableSelectionneeAdminLabel;
    private Long selectedTableId = null;

    @FXML private StackPane mainStack;
    @FXML private VBox paneTables;
    @FXML private VBox paneMenu, paneCommandes, paneStats;
    @FXML private GridPane planSalle;

    @FXML private TableView<Map<String,Object>> menuTable;
    @FXML private TableColumn<Map<String,Object>,String> colNom, colCategorie, colDispo, colDesc;
    @FXML private TableColumn<Map<String,Object>,String> colPrix;
    @FXML private TextField nomField, prixField, descField;
    @FXML private ComboBox<String> categorieBox;

    @FXML private TableView<Map<String,Object>> commandesTable;
    @FXML private TableColumn<Map<String,Object>,String> colCId, colCTable, colCStatut, colCServeur, colCHeure, colCTotal;

    @FXML private Label caJourLabel, tablesOcclabel, cmdEnCours;

    private final ObjectMapper mapper = new ObjectMapper();
    private ObservableList<Map<String,Object>> menuItems = FXCollections.observableArrayList();
    private ObservableList<Map<String,Object>> commandeItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        nomAdminLabel.setText(SessionManager.getInstance().getNom());
        setupMenuTable();
        setupCommandesTable();
        setupUtilisateursTable();
        loadPlanSalle();
        loadUtilisateurs();
        rafraichirBadgeNotifications();

        javafx.animation.Timeline pollNotifs = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(15),
                        e -> rafraichirBadgeNotifications()));
        pollNotifs.setCycleCount(javafx.animation.Animation.INDEFINITE);
        pollNotifs.play();
        menuTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nomField.setText(newSelection.getOrDefault("nom", "").toString());
                prixField.setText(newSelection.getOrDefault("prix", "").toString());
                categorieBox.setValue(newSelection.getOrDefault("categorie", "PLAT").toString());
                descField.setText(newSelection.getOrDefault("description", "").toString());
            } }) ;
    }

    private void showPane(Node pane) {
        for (Node p : List.of(paneTables, paneMenu, paneCommandes, paneStats, paneAdditions, paneNotifications, paneUtilisateurs)) {
            if (p != null) {
                p.setVisible(false);
                p.setManaged(false);
            }
        }

        pane.setVisible(true);
        pane.setManaged(true);


        for (Button b : List.of(btnTables, btnMenu, btnCommandes, btnStats, btnAdditions, btnNotifications, btnUtilisateurs)) {
            if (b != null) {
                b.setStyle("-fx-background-color:transparent;-fx-text-fill:#c3cadb;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
            }
        }
    }

    @FXML public void showTables() {

        showPane(paneTables);
        btnTables.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadPlanSalle();
    }

    @FXML public void showMenu() {

        showPane(paneMenu);
        btnMenu.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadMenu();
    }

    @FXML public void showCommandes() {

        showPane(paneCommandes);
        btnCommandes.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadCommandes();
    }

    @FXML public void showStats() {
        showPane(paneStats);
        btnStats.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadStats();
    }

    @FXML public void showNotifications() {

        showPane(paneNotifications);
        btnNotifications.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadNotifications();
    }

    @FXML public void showUtilisateurs() {

        showPane(paneUtilisateurs);
        btnUtilisateurs.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        if (uRoleBox.getItems().isEmpty()) {
            uRoleBox.setItems(FXCollections.observableArrayList("SERVEUR", "CUISINIER"));
        }
        setupUtilisateursTable();
        loadUtilisateurs();
    }
    @FXML
    public void showGestionAdditions() {
        showPane(paneAdditions);

            btnAdditions.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");

        if (containerFacture != null && labelSelectionTableMessage != null) {
            containerFacture.getChildren().setAll(labelSelectionTableMessage);
            labelSelectionTableMessage.setText("Aucune table sélectionnée pour le moment.");
        }

        loadTablesPourAdditions();
    }
    private void loadTablesPourAdditions() {
        new Thread(() -> {
            try {
                String jsonAdditions = ApiClient.get("/additions/en-cours");
                List<Map<String, Object>> additionsEnCours = mapper.readValue(jsonAdditions,
                        new com.fasterxml.jackson.core.type.TypeReference<>(){});

                java.util.Set<Long> tablesAvecAddition = new java.util.HashSet<>();
                java.util.Map<Long, Long> tableIdToAdditionId = new java.util.HashMap<>();
                for (Map<String, Object> a : additionsEnCours) {
                    Long idTable = Long.valueOf(a.get("idTable").toString());
                    tablesAvecAddition.add(idTable);
                    if (a.get("idAddition") != null)
                        tableIdToAdditionId.put(idTable, Long.valueOf(a.get("idAddition").toString()));
                }

                String jsonTables = ApiClient.get("/tables");
                List<Map<String, Object>> tables = mapper.readValue(jsonTables,
                        new com.fasterxml.jackson.core.type.TypeReference<>(){});

                Platform.runLater(() -> {
                    grilleTablesAdditions.getChildren().clear();
                    int col = 0, row = 0;

                    for (Map<String, Object> t : tables) {
                        String numTable = t.get("numero").toString().trim();
                        final Long idTable = Long.valueOf(t.get("idTable").toString());
                        boolean aAddition = tablesAvecAddition.contains(idTable);

                        Button btnTable = new Button("Table " + numTable);
                        btnTable.setPrefSize(115, 85);

                        if (aAddition) {
                            btnTable.setStyle(
                                "-fx-background-color: #3b82f6;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13;" +
                                "-fx-background-radius: 10;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian,#3b82f6,8,0.5,0,0);"
                            );
                            btnTable.setOnAction(ev -> afficherFactureTable(idTable, numTable));
                        } else {
                            btnTable.setStyle(
                                "-fx-background-color: #22c55e;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 10;" +
                                "-fx-opacity: 0.45;" +
                                "-fx-cursor: hand;"
                            );
                            btnTable.setOnAction(ev -> {
                                containerFacture.getChildren().setAll(labelSelectionTableMessage);
                                labelSelectionTableMessage.setText("La Table " + numTable + " n'a pas de demande d'addition en attente.");
                            });
                        }

                        grilleTablesAdditions.add(btnTable, col, row);
                        col++;
                        if (col >= 3) { col = 0; row++; }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                    containerFacture.getChildren().setAll(new Label("Erreur de connexion au serveur.")));
            }
        }).start();
    }
    private void recupererStatutLumiereTable(Long idTable, String numTable, Button btnTable) {
        new Thread(() -> {
            try {
                String jsonCommande = ApiClient.get("/commandes/table/" + idTable);

                if (jsonCommande != null && !jsonCommande.trim().isEmpty()) {
                    com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(jsonCommande);
                    String statutCommande = "";

                    if (rootNode.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode cmdNode : rootNode) {
                            if (cmdNode.has("statut") && "SERVIE".equals(cmdNode.get("statut").asText())) {
                                statutCommande = "SERVIE";
                                break;
                            }
                        }
                    } else if (rootNode.isObject()) {
                        if (rootNode.has("statut")) {
                            statutCommande = rootNode.get("statut").asText();
                        }
                    }

                    if ("SERVIE".equals(statutCommande)) {
                        Platform.runLater(() -> {
                            btnTable.setStyle("-fx-background-color: #f5a623; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
                            btnTable.setOnAction(ev -> afficherFactureTable(idTable, numTable));
                        });
                        return;
                    }
                }
            } catch (Exception e) {
            }

            Platform.runLater(() -> {
                btnTable.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8; -fx-opacity: 0.4; -fx-cursor: hand;");
                btnTable.setOnAction(ev -> {
                    containerFacture.getChildren().setAll(labelSelectionTableMessage);
                    labelSelectionTableMessage.setText("La Table " + numTable + " n'a pas formulé de demande d'addition.");
                });
            });
        }).start();
    }
    private void afficherFactureTable(Long idTable, String numeroTable) {
        new Thread(() -> {
            try {
                String jsonAddition = ApiClient.get("/additions/table/" + idTable);
                if (jsonAddition == null || jsonAddition.isBlank()) {
                    Platform.runLater(() -> containerFacture.getChildren().setAll(
                            new Label("Aucune addition trouvée pour la Table " + numeroTable + " (réponse vide du serveur).")));
                    return;
                }
                Map<String, Object> addition = mapper.readValue(jsonAddition,
                        new com.fasterxml.jackson.core.type.TypeReference<>(){});

                if (addition == null || addition.isEmpty()) {
                    Platform.runLater(() -> containerFacture.getChildren().setAll(
                            new Label("Aucune addition trouvée pour la Table " + numeroTable)));
                    return;
                }

                String jsonCommandes = ApiClient.get("/commandes/table/" + idTable);
                List<Map<String, Object>> commandes = mapper.readValue(jsonCommandes,
                        new com.fasterxml.jackson.core.type.TypeReference<List<Map<String,Object>>>(){});

                List<Map<String, Object>> toutesLignes = new ArrayList<>();
                int nbPersonnes = 0;
                for (Map<String, Object> cmd : commandes) {
                    String statut = cmd.getOrDefault("statut", "").toString();
                    if ("ANNULEE".equals(statut)) continue;
                    List<Map<String, Object>> lignesCmd = (List<Map<String, Object>>) cmd.get("lignes");
                    if (lignesCmd != null) toutesLignes.addAll(lignesCmd);
                    if (nbPersonnes == 0 && cmd.get("table") instanceof Map) {
                        Object nbP = ((Map<?,?>) cmd.get("table")).get("nbPersonnes");
                        if (nbP != null) nbPersonnes = ((Number) nbP).intValue();
                    }
                }

                final Long idAddition = Long.valueOf(addition.get("idAddition").toString());
                final double montantTotal = ((Number) addition.get("montantTotal")).doubleValue();
                final List<Map<String, Object>> lignesFinal = toutesLignes;
                final int nbPFinal = nbPersonnes;
                final String modePaiementServeur = addition.get("modePaiement") != null
                        ? addition.get("modePaiement").toString() : "ESPECES";

                Platform.runLater(() -> {
                    containerFacture.getChildren().clear();

                    HBox headerBox = new HBox();
                    headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    headerBox.setSpacing(15);

                    Text title = new Text("🧾 Clôturer l'addition — Table " + numeroTable);
                    title.setStyle("-fx-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

                    Label badgePersonnes = new Label(nbPFinal + " personnes");
                    badgePersonnes.setStyle("-fx-background-color: #159267; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    headerBox.getChildren().addAll(title, spacer, badgePersonnes);

                    VBox detailBox = new VBox(10);
                    detailBox.setStyle("-fx-background-color: #141a2e; -fx-background-radius: 10; -fx-padding: 15;");

                    Label lblSection = new Label("Détail de l'addition");
                    lblSection.setStyle("-fx-text-fill: #98a2b8; -fx-font-size: 12px;");
                    detailBox.getChildren().add(lblSection);

                    GridPane tableGrid = new GridPane();
                    tableGrid.setHgap(10);
                    tableGrid.setVgap(8);

                    Label hPlat = new Label("Plat"); hPlat.setStyle("-fx-text-fill: #98a2b8; -fx-font-weight: bold;");
                    Label hQte = new Label("Qté"); hQte.setStyle("-fx-text-fill: #98a2b8; -fx-font-weight: bold;");
                    Label hPrix = new Label("Prix unit."); hPrix.setStyle("-fx-text-fill: #98a2b8; -fx-font-weight: bold;");
                    Label hTotal = new Label("Total"); hTotal.setStyle("-fx-text-fill: #98a2b8; -fx-font-weight: bold;");
                    tableGrid.add(hPlat, 0, 0); tableGrid.add(hQte, 1, 0); tableGrid.add(hPrix, 2, 0); tableGrid.add(hTotal, 3, 0);

                    ColumnConstraints col1 = new ColumnConstraints(); col1.setHgrow(Priority.ALWAYS);
                    ColumnConstraints col2 = new ColumnConstraints(40);
                    ColumnConstraints col3 = new ColumnConstraints(90);
                    ColumnConstraints col4 = new ColumnConstraints(90);
                    tableGrid.getColumnConstraints().addAll(col1, col2, col3, col4);

                    double sousTotalCalculé = 0.0;
                    int rowIndex = 1;

                    for (Map<String, Object> ligne : lignesFinal) {
                        Map<String, Object> article = (Map<String, Object>) ligne.get("article");
                        String nomPlat;
                        if (article != null && article.get("nom") != null) {
                            nomPlat = article.get("nom").toString();
                        } else if (ligne.get("nomArticle") != null) {
                            nomPlat = ligne.get("nomArticle").toString();
                        } else {
                            nomPlat = "Article supprimé";
                        }
                        double prixUnit;
                        if (ligne.get("prixUnitaire") instanceof Number)
                            prixUnit = ((Number) ligne.get("prixUnitaire")).doubleValue();
                        else if (article != null)
                            prixUnit = ((Number) article.getOrDefault("prix", 0)).doubleValue();
                        else
                            prixUnit = 0;
                        int qte = ((Number) ligne.getOrDefault("quantite", 1)).intValue();
                        double totalLigne = prixUnit * qte;
                        sousTotalCalculé += totalLigne;

                        Label lblNom = new Label(nomPlat); lblNom.setStyle("-fx-text-fill: white;");
                        Label lblQte = new Label(String.valueOf(qte)); lblQte.setStyle("-fx-text-fill: white;");
                        Label lblPrix = new Label(String.format("%.2f DT", prixUnit)); lblPrix.setStyle("-fx-text-fill: white;");
                        Label lblTotal = new Label(String.format("%.2f DT", totalLigne)); lblTotal.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

                        tableGrid.add(lblNom, 0, rowIndex); tableGrid.add(lblQte, 1, rowIndex);
                        tableGrid.add(lblPrix, 2, rowIndex); tableGrid.add(lblTotal, 3, rowIndex);
                        rowIndex++;
                    }

                    final double sousTotalFinal = sousTotalCalculé;

                    Separator sepTable = new Separator();
                    sepTable.setStyle("-fx-background-color: #232a42;");
                    tableGrid.add(sepTable, 0, rowIndex, 4, 1);
                    rowIndex++;

                    Label lblSousTotalText = new Label("Sous-total");
                    lblSousTotalText.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    Label lblSousTotalVal = new Label(String.format("%.2f DT", sousTotalFinal));
                    lblSousTotalVal.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                    tableGrid.add(lblSousTotalText, 0, rowIndex);
                    tableGrid.add(lblSousTotalVal, 3, rowIndex);

                    detailBox.getChildren().add(tableGrid);

                    GridPane bottomGrid = new GridPane();
                    bottomGrid.setHgap(20);
                    bottomGrid.setVgap(10);

                    Label lblReduc = new Label("Réduction (%)"); lblReduc.setStyle("-fx-text-fill: #98a2b8;");
                    TextField fieldReduc = new TextField("0"); fieldReduc.setPrefWidth(80);

                    Label lblTotalApres = new Label("Total après réduction"); lblTotalApres.setStyle("-fx-text-fill: #98a2b8;");
                    Label valTotalApres = new Label(String.format("%.2f DT", sousTotalFinal));
                    valTotalApres.setStyle("-fx-text-fill: #e94560; -fx-font-size: 18px; -fx-font-weight: bold;");

                    bottomGrid.add(lblReduc, 0, 0);
                    bottomGrid.add(fieldReduc, 0, 1);
                    bottomGrid.add(lblTotalApres, 1, 0);
                    bottomGrid.add(valTotalApres, 1, 1);

                    fieldReduc.textProperty().addListener((obs, oldV, newV) -> {
                        try {
                            double pourcentage = newV.isEmpty() ? 0 : Double.parseDouble(newV);
                            double totalApres = sousTotalFinal * (1 - (pourcentage / 100.0));
                            valTotalApres.setText(String.format("%.2f DT", totalApres));
                        } catch (NumberFormatException e) {
                            valTotalApres.setText(String.format("%.2f DT", sousTotalFinal));
                        }
                    });

                    String modeLabel = "CARTE".equals(modePaiementServeur) ? "💳 Carte bancaire"
                                     : "AUTRE".equals(modePaiementServeur) ? "🔄 Autre"
                                     : "💵 Espèces";
                    String modeBg = "CARTE".equals(modePaiementServeur) ? "#3b82f6" : "#22c55e";

                    Label lblMode = new Label("Mode de paiement");
                    lblMode.setStyle("-fx-text-fill: #98a2b8;");
                    Label badgeMode = new Label(modeLabel);
                    badgeMode.setStyle("-fx-background-color:" + modeBg + ";" +
                            "-fx-text-fill:white;-fx-font-weight:bold;" +
                            "-fx-padding:8 20;-fx-background-radius:8;-fx-font-size:13;");

                    String confirmLabel = "CARTE".equals(modePaiementServeur)
                            ? "✔ Confirmer le paiement par Carte & Générer le reçu"
                            : "✔ Confirmer le paiement en Espèces & Générer le reçu";

                    HBox actionsBox = new HBox(15);
                    actionsBox.setAlignment(javafx.geometry.Pos.CENTER);
                    actionsBox.setStyle("-fx-padding: 15 0 0 0;");

                    Button btnAnnuler = new Button("Annuler");
                    btnAnnuler.getStyleClass().add("btn-danger");
                    btnAnnuler.setPrefWidth(120);
                    btnAnnuler.setOnAction(e -> containerFacture.getChildren().setAll(labelSelectionTableMessage));

                    Button btnConfirmer = new Button(confirmLabel);
                    btnConfirmer.setStyle("-fx-background-color: #223760; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;");
                    HBox.setHgrow(btnConfirmer, Priority.ALWAYS);
                    btnConfirmer.setMaxWidth(Double.MAX_VALUE);
                    btnConfirmer.setOnAction(e -> validerClotureFacture(idAddition, idTable, fieldReduc.getText(), modePaiementServeur));

                    actionsBox.getChildren().addAll(btnAnnuler, btnConfirmer);

                    containerFacture.getChildren().addAll(headerBox, detailBox, lblMode, badgeMode, bottomGrid, actionsBox);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    containerFacture.getChildren().setAll(new Label("Erreur lors de la récupération des détails de la table."));
                });
            }
        }).start();
    }
    private void validerClotureFacture(Long idAddition, Long idTable, String reduction, String modePaiement) {
        new Thread(() -> {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                String modeEnum = modePaiement != null ? modePaiement.toUpperCase() : "ESPECES";
                body.put("modePaiement", modeEnum);
                double reduc = 0;
                try { reduc = Double.parseDouble(reduction.trim()); } catch (Exception ignored) {}
                body.put("reduction", reduc);
                body.put("idAdmin", SessionManager.getInstance().getUserId());

                String resp = ApiClient.put("/additions/" + idAddition + "/payer", body);
                System.out.println("DEBUG payer response: " + resp);

                boolean succes = resp != null && !resp.isBlank()
                        && (resp.contains("\"message\"") || resp.contains("Addition payée"));
                boolean erreur = resp == null || resp.isBlank() || resp.contains("\"error\"")
                        || resp.contains("Forbidden") || resp.contains("Unauthorized");

                if (erreur || !succes) {
                    final String respFinal = resp;
                    Platform.runLater(() -> {
                        labelSelectionTableMessage.setText("⚠ Échec du paiement (réponse serveur: "
                                + (respFinal == null || respFinal.isBlank() ? "vide / accès refusé" : respFinal) + ")");
                        labelSelectionTableMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14;");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    loadTablesPourAdditions();
                    loadPlanSalle();
                    containerFacture.getChildren().setAll(labelSelectionTableMessage);
                    labelSelectionTableMessage.setText("✔ Paiement enregistré — table libérée !");
                    labelSelectionTableMessage.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 15; -fx-font-weight: bold;");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> labelSelectionTableMessage.setText("Erreur lors du paiement : " + e.getMessage()));
            }
        }).start();
    }

    @FXML public void logout() {
        SessionManager.getInstance().logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPlanSalle() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/tables");
                if (json != null && !json.trim().isEmpty()) {
                    List<Map<String,Object>> tables = mapper.readValue(json, new TypeReference<>(){});
                    Platform.runLater(() -> buildPlanSalle(tables));
                } else {
                    System.out.println("[ERREUR 403] Tables non chargees - token invalide ou backend HS");
                }
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
        String statutRaw = t.get("statut").toString();
        boolean libre = "LIBRE".equals(statutRaw) || "PAYEE".equals(statutRaw);
        Long idTable  = Long.valueOf(t.get("idTable").toString());

        VBox card = new VBox(6);
        card.setPrefSize(155, 110);

        boolean selected = idTable.equals(selectedTableId);
        String bgColor = selected ? "#a855f7"
                       : (libre  ? "#159267" : "#c0392b");

        card.setStyle("-fx-background-color:" + bgColor
                + ";-fx-background-radius:12;-fx-padding:12;-fx-cursor:hand;");

        Label num  = new Label("Table " + t.get("numero"));
        num.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14;");

        Label cap  = new Label("Capacité : " + t.get("capacite"));
        cap.setStyle("-fx-text-fill:white;-fx-font-size:11;");

        Object empl = t.get("emplacement");
        if (empl != null && !empl.toString().isEmpty()) {
            Label emplLabel = new Label(empl.toString());
            emplLabel.setStyle("-fx-text-fill:#dfe4ef;-fx-font-size:10;");
            card.getChildren().add(emplLabel);
        }

        String statutTxt = "PAYEE".equals(statutRaw) ? "libre"
                          : libre ? "Libre"
                          : "Occupée (" + t.get("nbPersonnes") + " pers.)";
        Label stat = new Label(statutTxt);
        stat.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:11;");

        card.getChildren().addAll(0, List.of(num, cap));
        card.getChildren().add(stat);

        card.setOnMouseClicked(e -> {
            selectedTableId = idTable;
            tNumeroField.setText(t.get("numero").toString());
            tCapaciteField.setText(t.get("capacite").toString());
            tEmplacementField.setText(empl != null ? empl.toString() : "");
            tableSelectionneeAdminLabel.setText("Table " + t.get("numero") + " sélectionnée — modifiez les champs puis cliquez Modifier, ou cliquez Supprimer.");
            tableSelectionneeAdminLabel.setStyle("-fx-text-fill:#f5a623;-fx-font-size:12;");
            loadPlanSalle();
        });

        return card;
    }


    @FXML void ajouterTable() {
        String numStr  = tNumeroField.getText().trim();
        String capStr  = tCapaciteField.getText().trim();
        String empl    = tEmplacementField.getText().trim();
        if (numStr.isEmpty() || capStr.isEmpty()) {
            setTableMsg("⚠ Numéro et capacité sont obligatoires.", "#e74c3c");
            return;
        }
        Map<String,Object> body = new LinkedHashMap<>();
        try {
            body.put("numero",   Integer.parseInt(numStr));
            body.put("capacite", Integer.parseInt(capStr));
        } catch (NumberFormatException ex) {
            setTableMsg("⚠ Numéro et capacité doivent être des entiers.", "#e74c3c");
            return;
        }
        if (!empl.isEmpty()) body.put("emplacement", empl);
        new Thread(() -> {
            try {
                String resp = ApiClient.post("/tables", body);
                if (resp.contains("error")) {
                    Platform.runLater(() -> setTableMsg("⚠ " + extractError(resp), "#e74c3c"));
                } else {
                    Platform.runLater(() -> {
                        clearTableFields();
                        setTableMsg("✔ Table ajoutée.", "#22c55e");
                        loadPlanSalle();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void modifierTable() {
        if (selectedTableId == null) { setTableMsg("⚠ Sélectionnez une table.", "#e74c3c"); return; }
        String numStr = tNumeroField.getText().trim();
        String capStr = tCapaciteField.getText().trim();
        String empl   = tEmplacementField.getText().trim();
        if (numStr.isEmpty() && capStr.isEmpty() && empl.isEmpty()) {
            setTableMsg("⚠ Renseignez au moins un champ à modifier.", "#e74c3c"); return;
        }
        Map<String,Object> body = new LinkedHashMap<>();
        try {
            if (!numStr.isEmpty()) body.put("numero",   Integer.parseInt(numStr));
            if (!capStr.isEmpty()) body.put("capacite", Integer.parseInt(capStr));
        } catch (NumberFormatException ex) {
            setTableMsg("⚠ Numéro et capacité doivent être des entiers.", "#e74c3c"); return;
        }
        body.put("emplacement", empl);
        Long id = selectedTableId;
        new Thread(() -> {
            try {
                String resp = ApiClient.put("/tables/" + id, body);
                if (resp.contains("error")) {
                    Platform.runLater(() -> setTableMsg("⚠ " + extractError(resp), "#e74c3c"));
                } else {
                    Platform.runLater(() -> {
                        selectedTableId = null;
                        clearTableFields();
                        setTableMsg("✔ Table modifiée.", "#22c55e");
                        tableSelectionneeAdminLabel.setText("Cliquez sur une table pour la sélectionner.");
                        tableSelectionneeAdminLabel.setStyle("-fx-text-fill:#6b7488;-fx-font-size:12;");
                        loadPlanSalle();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void supprimerTable() {
        if (selectedTableId == null) { setTableMsg("⚠ Sélectionnez une table.", "#e74c3c"); return; }
        Long id = selectedTableId;
        ButtonType btnOuiTable = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNonTable = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette table ? Cette action est irréversible.", btnOuiTable, btnNonTable);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnOuiTable) {
                new Thread(() -> {
                    try {
                        ApiClient.delete("/tables/" + id);

                        Platform.runLater(() -> {
                            selectedTableId = null;
                            clearTableFields();
                            setTableMsg("✔ Table supprimée.", "#22c55e");
                            loadPlanSalle();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur de suppression");
                            alert.setHeaderText(null);
                            alert.setContentText("Impossible de supprimer une table occupée.");
                            alert.showAndWait();

                            setTableMsg("⚠ Impossible de supprimer : table occupée.", "#e74c3c");
                        });
                    }
                }).start();
            }
        });
    }

    private void setTableMsg(String msg, String color) {
        tableActionLabel.setText(msg);
        tableActionLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12;");
    }

    private void clearTableFields() {
        tNumeroField.clear(); tCapaciteField.clear(); tEmplacementField.clear();
    }

    private String extractError(String json) {
        try {
            Map<String,Object> m = mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>(){});
            return m.getOrDefault("error", json).toString();
        } catch (Exception e) { return json; }
    }


    private void setupMenuTable() {
        colNom.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrDefault("nom","").toString()));
        colCategorie.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrDefault("categorie","").toString()));
        colPrix.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrDefault("prix","").toString() + " TND"));
        colDispo.setCellValueFactory(c -> new SimpleStringProperty(
                Boolean.parseBoolean(c.getValue().getOrDefault("disponible","false").toString()) ? "Oui" : "Non"));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOrDefault("description","").toString()));
        menuTable.setItems(menuItems);
        categorieBox.setItems(FXCollections.observableArrayList(
                "ENTREE","PLAT","DESSERT","BOISSON","AUTRE"));
    }

    private void loadMenu() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/articles");
                List<Map<String,Object>> articles = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> menuItems.setAll(articles));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void ajouterArticle() {
        if (nomField.getText().isEmpty() || prixField.getText().isEmpty()) {
            showAlert("Nom et prix sont obligatoires.");
            return;
        }
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("nom",         nomField.getText());
        body.put("prix",        Double.parseDouble(prixField.getText()));
        body.put("categorie",   categorieBox.getValue() != null ? categorieBox.getValue() : "PLAT");
        body.put("description", descField.getText());
        body.put("disponible",  true);
        new Thread(() -> {
            try {
                ApiClient.post("/articles", body);
                Platform.runLater(() -> {
                    nomField.clear(); prixField.clear(); descField.clear();
                    loadMenu();
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void supprimerArticle() {
        Map<String,Object> sel = menuTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Sélectionnez un article."); return; }
        Long id = Long.valueOf(sel.get("idArticle").toString());
        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'article \"" + sel.get("nom") + "\" ?", btnOui, btnNon);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnOui) {
                new Thread(() -> {
                    try {
                        String resp = ApiClient.deleteWithBody("/articles/" + id);
                        if (resp != null && resp.contains("\"error\"")) {
                            String msg = extractError(resp);
                            Platform.runLater(() -> showAlert("⚠ " + msg));
                        } else {
                            Platform.runLater(this::loadMenu);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> showAlert("Erreur lors de la suppression."));
                    }
                }).start();
            }
        });
    }

    @FXML void toggleDispo() {
        Map<String,Object> sel = menuTable.getSelectionModel().getSelectedItem();
        if (sel == null) { showAlert("Selectionnez un article."); return; }
        Long id = Long.valueOf(sel.get("idArticle").toString());
        new Thread(() -> {
            try {
                ApiClient.patch("/articles/" + id + "/disponibilite");
                Platform.runLater(this::loadMenu);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
    @FXML
    void modifierArticle() {
        // 1. Vérifier qu'un article est bien sélectionné
        Map<String, Object> sel = menuTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert("Sélectionnez un article à modifier.");
            return;
        }

        // 2. Récupérer l'ID de l'article sélectionné
        Long id = Long.valueOf(sel.get("idArticle").toString());

        // 3. Préparer le corps de la requête
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nom",         nomField.getText());
        body.put("prix",        Double.parseDouble(prixField.getText()));
        body.put("categorie",   categorieBox.getValue() != null ? categorieBox.getValue() : "PLAT");
        body.put("description", descField.getText());
        body.put("disponible",  true); // Ou conserver l'état actuel si besoin

        // 4. Envoyer la requête via un Thread (comme pour ajouter/supprimer)
        new Thread(() -> {
            try {
                String resp = ApiClient.put("/articles/" + id, body);
                if (resp != null && resp.contains("\"error\"")) {
                    String msg = extractError(resp);
                    Platform.runLater(() -> showAlert("⚠ " + msg));
                    return;
                }
                Platform.runLater(() -> {
                    nomField.clear();
                    prixField.clear();
                    descField.clear();
                    loadMenu();
                    showAlert("Article modifié avec succès !");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Erreur lors de la modification."));
            }
        }).start();
    }
    private void setupCommandesTable() {
        colCId.setCellValueFactory(c -> {
            Object id = c.getValue().get("idCommande");
            return new SimpleStringProperty(id != null ? "#" + id : "--");
        });

        colCTable.setCellValueFactory(c -> {
            Object t = c.getValue().get("table");
            if (t instanceof Map) return new SimpleStringProperty("Table " + ((Map<?,?>)t).get("numero"));
            return new SimpleStringProperty("--");
        });

        colCStatut.setCellValueFactory(c -> {
            Object statut = c.getValue().get("statut");
            return new SimpleStringProperty(statut != null ? statut.toString() : "INCONNU");
        });
        colCStatut.setCellFactory(col -> new TableCell<Map<String,Object>, String>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(statut.replace("_", " "));
                badge.getStyleClass().add("badge");
                String cls = switch (statut) {
                    case "EN_ATTENTE" -> "badge-attente";
                    case "EN_PREPARATION" -> "badge-preparation";
                    case "PRETE" -> "badge-prete";
                    case "SERVIE" -> "badge-servie";
                    case "ANNULEE" -> "badge-annulee";
                    default -> "badge-servie";
                };
                badge.getStyleClass().add(cls);
                setGraphic(badge);
                setText(null);
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
        });

        colCServeur.setCellValueFactory(c -> {
            Object s = c.getValue().get("serveur");
            if (s instanceof Map) return new SimpleStringProperty(((Map<?,?>)s).get("nom").toString());
            return new SimpleStringProperty("--");
        });

        colCHeure.setCellValueFactory(c -> {
            Object h = c.getValue().get("heureCreation");
            if (h != null && h.toString().length() >= 16) {
                return new SimpleStringProperty(h.toString().substring(0,16).replace("T"," "));
            }
            return new SimpleStringProperty("--");
        });

        colCTotal.setCellValueFactory(c -> {
            Map<String, Object> commande = c.getValue();
            if (commande.get("totalFinal") instanceof Number) {
                double total = ((Number) commande.get("totalFinal")).doubleValue();
                return new SimpleStringProperty(String.format("%.2f DT", total));
            }
            List<Map<String, Object>> lignes = (List<Map<String, Object>>) commande.get("lignes");
            double total = 0.0;
            if (lignes != null) {
                for (Map<String, Object> ligne : lignes) {
                    double prix = (ligne.get("prixUnitaire") instanceof Number) ? ((Number) ligne.get("prixUnitaire")).doubleValue() : 0.0;
                    int qte = (ligne.get("quantite") instanceof Number) ? ((Number) ligne.get("quantite")).intValue() : 0;
                    total += (prix * qte);
                }
            }
            return new SimpleStringProperty(String.format("%.2f DT", total));
        });

        commandesTable.setItems(commandeItems);

        commandesTable.setRowFactory(tv -> {
            TableRow<Map<String,Object>> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    afficherDetailsCommandeAdmin(row.getItem());
                }
            });
            return row;
        });
    }
    @FXML public void loadCommandes() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/commandes/toutes");
                List<Map<String,Object>> cmds = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> commandeItems.setAll(cmds));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML public void loadStats() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/stats/dashboard");
                Map<String,Object> stats = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> {
                    caJourLabel.setText(stats.getOrDefault("chiffreAffairesJour","0") + " TND");
                    tablesOcclabel.setText(stats.getOrDefault("tablesOccupees","0")
                            + " / " + stats.getOrDefault("totalTables","0"));
                    cmdEnCours.setText(stats.getOrDefault("commandesEnCours","0").toString());
                    caMoisLabel.setText(stats.getOrDefault("chiffreAffairesMois", "0") + " TND");
                    caAnneeLabel.setText(stats.getOrDefault("chiffreAffairesAnnee", "0") + " TND");
                    List<Map<String, Object>> bestList = (List<Map<String, Object>>) stats.get("bestSellers");

                    if (bestList != null && !bestList.isEmpty()) {
                        Map<String, Object> best = bestList.get(0);
                        bestSellerLabel.setText(best.get("nom").toString());
                        if (bestSellerQteLabel != null) {
                            bestSellerQteLabel.setText("");
                        }

                    } else {
                        bestSellerLabel.setText("Aucun");
                        if (bestSellerQteLabel != null) bestSellerQteLabel.setText("");
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }


    private void rafraichirBadgeNotifications() {
        Long idAdmin = SessionManager.getInstance().getUserId();
        if (idAdmin == null) return;
        new Thread(() -> {
            try {
                String json = ApiClient.get("/notifications/user/" + idAdmin + "/count");
                Map<String, Object> m = mapper.readValue(json, new TypeReference<>(){});
                long count = ((Number) m.getOrDefault("count", 0)).longValue();
                Platform.runLater(() -> {
                    if (count > 0) {
                        badgeNotifCount.setText(String.valueOf(count));
                        badgeNotifCount.setVisible(true);
                        badgeNotifCount.setManaged(true);
                    } else {
                        badgeNotifCount.setVisible(false);
                        badgeNotifCount.setManaged(false);
                    }
                });
            } catch (Exception e) { }
        }).start();
    }

    @FXML public void loadNotifications() {
        Long idAdmin = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            try {
                String json = ApiClient.get("/notifications/user/" + idAdmin);
                List<Map<String, Object>> notifs = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> {
                    listeNotifications.getChildren().clear();
                    if (notifs.isEmpty()) {
                        Label vide = new Label("Aucune notification pour le moment.");
                        vide.setStyle("-fx-text-fill:#6b7488;-fx-font-size:13;");
                        listeNotifications.getChildren().add(vide);
                    } else {
                        for (Map<String, Object> n : notifs) {
                            listeNotifications.getChildren().add(creerCarteNotification(n));
                        }
                    }
                });
                rafraichirBadgeNotifications();
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private VBox creerCarteNotification(Map<String, Object> n) {
        boolean lue = Boolean.TRUE.equals(n.get("lue"));
        Long idNotif = Long.valueOf(n.get("idNotif").toString());
        String type = n.get("type") != null ? n.get("type").toString() : "";

        String icone = switch (type) {
            case "ADDITION"          -> "💵";
            case "COMMANDE_PRETE"    -> "✅";
            case "NOUVELLE_COMMANDE" -> "🧾";
            case "COMMANDE_MODIFIEE" -> "✏️";
            case "COMMANDE_ANNULEE"  -> "❌";
            default -> "🔔";
        };

        VBox card = new VBox(4);
        card.setStyle("-fx-background-color:" + (lue ? "#141a2e" : "#243b66")
                + ";-fx-background-radius:10;-fx-padding:14;"
                + (lue ? "" : "-fx-border-color:#60a5fa;-fx-border-width:0 0 0 4;-fx-border-radius:10;"));

        HBox top = new HBox(10);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size:16;");
        Label lblMsg = new Label(n.get("message") != null ? n.get("message").toString() : "");
        lblMsg.setStyle("-fx-text-fill:white;-fx-font-size:13;" + (lue ? "" : "-fx-font-weight:bold;"));
        lblMsg.setWrapText(true);
        HBox.setHgrow(lblMsg, Priority.ALWAYS);
        top.getChildren().addAll(lblIcone, lblMsg);

        String heure = n.get("heureCreation") != null ? n.get("heureCreation").toString() : "";
        if (heure.length() >= 16) heure = heure.substring(0, 16).replace("T", " ");
        Label lblHeure = new Label(heure);
        lblHeure.setStyle("-fx-text-fill:#8b93a8;-fx-font-size:11;");

        card.getChildren().addAll(top, lblHeure);

        if (!lue) {
            card.setStyle(card.getStyle() + "-fx-cursor:hand;");
            card.setOnMouseClicked(e -> {
                new Thread(() -> {
                    try {
                        ApiClient.put("/notifications/" + idNotif + "/lire", Map.of());
                        Platform.runLater(this::loadNotifications);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }).start();
            });
        }
        return card;
    }

    @FXML public void marquerToutesNotifsLues() {
        Long idAdmin = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            try {
                ApiClient.put("/notifications/user/" + idAdmin + "/lire-tout", Map.of());
                Platform.runLater(this::loadNotifications);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }


    private boolean utilisateursTableConfiguree = false;

    private void setupUtilisateursTable() {
        if (utilisateursTableConfiguree) return;
        utilisateursTableConfiguree = true;

        colUNom.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("nom") != null ? c.getValue().get("nom").toString() : ""));
        colUEmail.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("email") != null ? c.getValue().get("email").toString() : ""));
        colURole.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().get("role") != null ? c.getValue().get("role").toString() : ""));
        colUActif.setCellValueFactory(c -> {
            boolean actif = Boolean.TRUE.equals(c.getValue().get("actif"));
            return new SimpleStringProperty(actif ? "Actif" : "Désactivé");
        });



        colUActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button();
            private final Button btnResetMdp = new Button("🔑 Reset MDP");
            private final Button btnResetNom = new Button("✏ Reset Nom");
            private final Button btnResetEmail = new Button("📧 Reset Email");
            private final Button btnDelete = new Button("🗑");
            private final HBox box = new HBox(6, btnToggle, btnResetMdp, btnResetNom, btnResetEmail, btnDelete);

            {
                btnToggle.setStyle("-fx-font-size:11;-fx-padding:4 8;");
                btnResetMdp.setStyle("-fx-font-size:11;-fx-padding:4 8;");
                btnResetNom.setStyle("-fx-font-size:11;-fx-padding:4 8;");
                btnResetEmail.setStyle("-fx-font-size:11;-fx-padding:4 8;");
                btnDelete.setStyle("-fx-font-size:11;-fx-padding:4 8;-fx-background-color:#c0392b;-fx-text-fill:white;");

                btnResetNom.setOnAction(e -> {
                    Map<String, Object> u = getTableView().getItems().get(getIndex());
                    ouvrirDialogModification("Nom", "nom", u.get("id").toString(), u.get("nom").toString());
                });
                btnResetEmail.setOnAction(e -> {
                    Map<String, Object> u = getTableView().getItems().get(getIndex());
                    ouvrirDialogModification("Email", "email", u.get("id").toString(), u.get("email").toString());
                });
                btnToggle.setOnAction(e -> {
                    Map<String, Object> u = getTableView().getItems().get(getIndex());
                    boolean actifActuel = Boolean.TRUE.equals(u.get("actif"));
                    toggleStatutUtilisateur(Long.valueOf(u.get("id").toString()), !actifActuel);
                });
                btnResetMdp.setOnAction(e -> {
                    Map<String, Object> u = getTableView().getItems().get(getIndex());
                    ouvrirDialogResetMotDePasse(Long.valueOf(u.get("id").toString()), u.get("nom").toString());
                });
                btnDelete.setOnAction(e -> {
                    Map<String, Object> u = getTableView().getItems().get(getIndex());
                    confirmerSuppressionUtilisateur(Long.valueOf(u.get("id").toString()), u.get("nom").toString());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Map<String, Object> u = getTableView().getItems().get(getIndex());

                boolean actif = Boolean.TRUE.equals(u.get("actif"));
                btnToggle.setText(actif ? "⏸ Désactiver" : "▶ Activer");

                boolean aDesCommandes = Boolean.TRUE.equals(u.get("aDesCommandes"));
                btnDelete.setVisible(!aDesCommandes);
                btnDelete.setManaged(!aDesCommandes);

                setGraphic(box);
            }
        });

        utilisateursTable.setItems(utilisateursItems);
    }

    private final ObservableList<Map<String,Object>> utilisateursItems = FXCollections.observableArrayList();

    @FXML public void loadUtilisateurs() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/utilisateurs");
                List<Map<String,Object>> users = mapper.readValue(json, new TypeReference<>(){});
                Platform.runLater(() -> utilisateursItems.setAll(users));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML public void creerUtilisateur() {
        String nom   = uNomField.getText().trim();
        String email = uEmailField.getText().trim();
        String mdp   = uMotDePasseField.getText();
        String role  = uRoleBox.getValue();

        if (nom.isEmpty() || email.isEmpty() || mdp.isEmpty() || role == null) {
            setUtilisateurMsg("⚠ Tous les champs sont obligatoires.", "#e74c3c");
            return;
        }
        if (!email.endsWith("@restaurantpro.tn")) {
            setUtilisateurMsg("⚠ L'email doit se terminer par @restaurantpro.tn", "#e74c3c");
            return;
        }
        if (mdp.length() < 6) {
            setUtilisateurMsg("⚠ Le mot de passe doit contenir au moins 6 caractères.", "#e74c3c");
            return;
        }

        Map<String,Object> body = new LinkedHashMap<>();
        body.put("nom", nom);
        body.put("email", email);
        body.put("motDePasse", mdp);
        body.put("role", role);

        new Thread(() -> {
            try {
                String resp = ApiClient.post("/utilisateurs", body);
                if (resp != null && resp.contains("\"error\"")) {
                    Platform.runLater(() -> setUtilisateurMsg("⚠ " + extractError(resp), "#e74c3c"));
                } else {
                    Platform.runLater(() -> {
                        uNomField.clear(); uEmailField.clear(); uMotDePasseField.clear(); uRoleBox.setValue(null);
                        setUtilisateurMsg("✔ Compte créé avec succès.", "#22c55e");
                        loadUtilisateurs();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void toggleStatutUtilisateur(Long id, boolean nouveauStatut) {
        new Thread(() -> {
            try {
                ApiClient.put("/utilisateurs/" + id + "/statut", Map.of("actif", nouveauStatut));
                Platform.runLater(this::loadUtilisateurs);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void ouvrirDialogResetMotDePasse(Long id, String nomUser) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Réinitialiser le mot de passe");
        dialog.setHeaderText("Nouveau mot de passe pour " + nomUser);
        dialog.setContentText("Mot de passe (min. 6 caractères) :");
        dialog.showAndWait().ifPresent(nouveauMdp -> {
            if (nouveauMdp.trim().length() < 6) {
                showAlert("Le mot de passe doit contenir au moins 6 caractères.");
                return;
            }
            new Thread(() -> {
                try {
                    ApiClient.put("/utilisateurs/" + id + "/mot-de-passe", Map.of("motDePasse", nouveauMdp.trim()));
                    Platform.runLater(() -> {
                        showAlert("Mot de passe mis à jour pour " + nomUser + ".");
                        loadUtilisateurs();
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        });
    }

    private void confirmerSuppressionUtilisateur(Long id, String nomUser) {
        ButtonType btnOuiUser = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNonUser = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer définitivement le compte de " + nomUser + " ?", btnOuiUser, btnNonUser);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnOuiUser) {
                new Thread(() -> {
                    try {
                        String resp = ApiClient.deleteWithBody("/utilisateurs/" + id);
                        if (resp != null && resp.contains("\"error\"")) {
                            String msg = extractError(resp);
                            Platform.runLater(() -> showAlert("⚠ " + msg));
                        } else {
                            Platform.runLater(this::loadUtilisateurs);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> showAlert("Erreur lors de la suppression : " + e.getMessage()));
                    }
                }).start();
            }
        });
    }

    private void setUtilisateurMsg(String msg, String color) {
        utilisateurActionLabel.setText(msg);
        utilisateurActionLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12;");
    }

    private void afficherDetailsCommandeAdmin(Map<String,Object> cmd) {
        String idCmd = cmd.get("idCommande") != null ? cmd.get("idCommande").toString() : "?";
        String statut = cmd.get("statut") != null ? cmd.get("statut").toString() : "?";

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de la Commande #" + idCmd);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(480);
        content.setStyle("-fx-padding:20;");

        Label lblStatut = new Label("Statut actuel : " + statut);
        lblStatut.setStyle("-fx-font-size:14;-fx-font-weight:bold;");
        content.getChildren().add(lblStatut);

        Object tableObj = cmd.get("table");
        if (tableObj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String,Object> tMap = (Map<String,Object>) tableObj;
            Label lblTable = new Label("Table : " + tMap.get("numero"));
            lblTable.setStyle("-fx-font-size:13;");
            content.getChildren().add(lblTable);
        }

        Object serveurObj = cmd.get("serveur");
        if (serveurObj instanceof Map) {
            @SuppressWarnings("unchecked") Map<String,Object> sMap = (Map<String,Object>) serveurObj;
            Label lblServeur = new Label("Serveur : " + sMap.get("nom"));
            lblServeur.setStyle("-fx-font-size:13;");
            content.getChildren().add(lblServeur);
        }

        Label lblPlats = new Label("Plats commandés :");
        lblPlats.setStyle("-fx-font-size:12;-fx-text-fill:#8b93a8;");
        content.getChildren().add(lblPlats);

        ObservableList<String> lignesItems = FXCollections.observableArrayList();
        ListView<String> lignesView = new ListView<>(lignesItems);
        lignesView.setPrefHeight(200);

        List<?> lignes = (List<?>) cmd.get("lignes");
        double total = 0;
        if (lignes != null && !lignes.isEmpty()) {
            for (Object l : lignes) {
                @SuppressWarnings("unchecked") Map<String,Object> ligne = (Map<String,Object>) l;
                @SuppressWarnings("unchecked") Map<String,Object> article = (Map<String,Object>) ligne.get("article");
                String nom;
                if (article != null && article.get("nom") != null) {
                    nom = article.get("nom").toString();
                } else if (ligne.get("nomArticle") != null) {
                    nom = ligne.get("nomArticle").toString();
                } else {
                    nom = "Article supprimé";
                }
                int qte = ligne.get("quantite") instanceof Number ? ((Number)ligne.get("quantite")).intValue() : 1;
                double prix = ligne.get("prixUnitaire") instanceof Number
                        ? ((Number)ligne.get("prixUnitaire")).doubleValue()
                        : (article != null && article.get("prix") instanceof Number ? ((Number)article.get("prix")).doubleValue() : 0);
                total += prix * qte;
                lignesItems.add(nom + "  x" + qte + "   →  " + String.format("%.3f TND", prix * qte));
            }
            lignesItems.add("─────────────────────────────");
            lignesItems.add("TOTAL :  " + String.format("%.3f TND", total));
        } else {
            lignesItems.add("Aucun article.");
        }

        Object notes = cmd.get("notes");
        Label lblNotes = new Label("Notes : " + (notes != null && !notes.toString().isEmpty() ? notes : "Aucune note"));
        lblNotes.setStyle("-fx-font-size:11;-fx-text-fill:#8b93a8;-fx-font-style:italic;");

        content.getChildren().addAll(lignesView, lblNotes);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg); a.showAndWait();
    }
    private void masquerTousLesPanneaux() {
        var panneaux = java.util.List.of(paneTables, paneMenu, paneCommandes, paneStats, paneAdditions, paneNotifications, paneUtilisateurs);

        for (VBox pane : panneaux) {
            if (pane != null) {
                pane.setVisible(false);
                pane.setManaged(false);
            }
        }
    }
    @FXML
    private void ouvrirDialogModification(String titre, String champ, String id, String valeurActuelle) {
        TextInputDialog dialog = new TextInputDialog(valeurActuelle);
        dialog.setTitle("Modifier " + titre);
        dialog.setHeaderText("Nouveau " + titre + " :");
        dialog.setContentText(titre + " :");

        dialog.showAndWait().ifPresent(nouvelleValeur -> {
            Map<String, Object> body = new HashMap<>();
            body.put(champ, nouvelleValeur.trim());

            new Thread(() -> {
                try {
                    ApiClient.put("/utilisateurs/" + id + "/profil", body);
                    Platform.runLater(() -> {
                        setUtilisateurMsg("✔ " + titre + " mis à jour !", "#22c55e");
                        loadUtilisateurs();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> setUtilisateurMsg("⚠ Erreur : " + e.getMessage(), "#e74c3c"));
                }
            }).start();
        });
    }
}