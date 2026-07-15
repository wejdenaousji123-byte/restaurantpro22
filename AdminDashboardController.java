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

public class AdminDashboardController {
    @FXML private Button btnAdditions;
    @FXML private GridPane grilleTablesAdditions;
    @FXML private VBox containerFacture;
    @FXML private Label labelSelectionTableMessage;
    @FXML private VBox paneAdditions;
    @FXML private Label nomAdminLabel;

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
    @FXML private TableColumn<Map<String,Object>,String> colCTable, colCStatut, colCServeur, colCHeure, colCNotes;

    @FXML private Label caJourLabel, tablesOcclabel, cmdEnCours;

    private final ObjectMapper mapper = new ObjectMapper();
    private ObservableList<Map<String,Object>> menuItems = FXCollections.observableArrayList();
    private ObservableList<Map<String,Object>> commandeItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        nomAdminLabel.setText(SessionManager.getInstance().getNom());
        setupMenuTable();
        setupCommandesTable();
        loadPlanSalle();    }

    private void showPane(Node pane) {
        for (Node p : List.of(paneTables, paneMenu, paneCommandes, paneStats, paneAdditions)) {
            if (p != null) {
                p.setVisible(false);
                p.setManaged(false);
            }
        }

        pane.setVisible(true);
        pane.setManaged(true);


        for (Button b : List.of(btnTables, btnMenu, btnCommandes, btnStats, btnAdditions)) {
            if (b != null) {
                b.setStyle("-fx-background-color:transparent;-fx-text-fill:#ccc;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
            }
        }
    }

    @FXML public void showTables() {
        masquerTousLesPanneaux();
        paneTables.setVisible(true);
        paneTables.setManaged(true);
        showPane(paneTables);
        btnTables.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadPlanSalle();
    }

    @FXML public void showMenu() {
        masquerTousLesPanneaux();
        paneMenu.setVisible(true);
        paneMenu.setManaged(true);
        showPane(paneMenu);
        btnMenu.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadMenu();
    }

    @FXML public void showCommandes() {
        masquerTousLesPanneaux();
        paneCommandes.setVisible(true);
        paneCommandes.setManaged(true);
        showPane(paneCommandes);
        btnCommandes.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadCommandes();
    }

    @FXML public void showStats() {
        masquerTousLesPanneaux();
        paneStats.setVisible(true);
        paneStats.setManaged(true);
        showPane(paneStats);
        btnStats.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadStats();
    }
    @FXML
    public void showGestionAdditions() {
        showPane(paneAdditions);

        if (btnAdditions != null) {
            btnAdditions.setStyle("-fx-background-color:#0f3460;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        }

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
                                "-fx-background-color: #2980b9;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-size: 13;" +
                                "-fx-background-radius: 10;" +
                                "-fx-cursor: hand;" +
                                "-fx-effect: dropshadow(gaussian,#2980b9,8,0.5,0,0);"
                            );
                            btnTable.setOnAction(ev -> afficherFactureTable(idTable, numTable));
                        } else {
                            btnTable.setStyle(
                                "-fx-background-color: #27ae60;" +
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
                            btnTable.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
                            btnTable.setOnAction(ev -> afficherFactureTable(idTable, numTable));
                        });
                        return;
                    }
                }
            } catch (Exception e) {
            }

            Platform.runLater(() -> {
                btnTable.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 8; -fx-opacity: 0.4; -fx-cursor: hand;");
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

                Platform.runLater(() -> {
                    containerFacture.getChildren().clear();

                    HBox headerBox = new HBox();
                    headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    headerBox.setSpacing(15);

                    Text title = new Text("🧾 Clôturer l'addition — Table " + numeroTable);
                    title.setStyle("-fx-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

                    Label badgePersonnes = new Label(nbPFinal + " personnes");
                    badgePersonnes.setStyle("-fx-background-color: #1e8449; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    headerBox.getChildren().addAll(title, spacer, badgePersonnes);

                    VBox detailBox = new VBox(10);
                    detailBox.setStyle("-fx-background-color: #16213e; -fx-background-radius: 10; -fx-padding: 15;");

                    Label lblSection = new Label("Détail de l'addition");
                    lblSection.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
                    detailBox.getChildren().add(lblSection);

                    GridPane tableGrid = new GridPane();
                    tableGrid.setHgap(10);
                    tableGrid.setVgap(8);

                    Label hPlat = new Label("Plat"); hPlat.setStyle("-fx-text-fill: #aaa; -fx-font-weight: bold;");
                    Label hQte = new Label("Qté"); hQte.setStyle("-fx-text-fill: #aaa; -fx-font-weight: bold;");
                    Label hPrix = new Label("Prix unit."); hPrix.setStyle("-fx-text-fill: #aaa; -fx-font-weight: bold;");
                    Label hTotal = new Label("Total"); hTotal.setStyle("-fx-text-fill: #aaa; -fx-font-weight: bold;");
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
                        if (article == null) continue;
                        String nomPlat = article.getOrDefault("nom", "?").toString();
                        double prixUnit = ((Number) article.getOrDefault("prix", 0)).doubleValue();
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
                    sepTable.setStyle("-fx-background-color: #333;");
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

                    Label lblReduc = new Label("Réduction (%)"); lblReduc.setStyle("-fx-text-fill: #aaa;");
                    TextField fieldReduc = new TextField("0"); fieldReduc.setPrefWidth(80);

                    Label lblTotalApres = new Label("Total après réduction"); lblTotalApres.setStyle("-fx-text-fill: #aaa;");
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

                    Label lblMode = new Label("Mode de paiement"); lblMode.setStyle("-fx-text-fill: #aaa;");
                    HBox modesBox = new HBox(10);

                    Button btnEspeces = new Button("Espèces");
                    Button btnCB = new Button("Carte bancaire");
                    Button btnAutre = new Button("Autre");

                    String styleSelect = "-fx-background-color: #0f3460; -fx-text-fill: white; -fx-border-color: #e94560; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16;";
                    String styleUnselect = "-fx-background-color: #16213e; -fx-text-fill: #aaa; -fx-border-color: #333; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 16;";

                    btnEspeces.setStyle(styleSelect);
                    btnCB.setStyle(styleUnselect);
                    btnAutre.setStyle(styleUnselect);

                    final String[] modeChoisi = {"Espèces"};

                    btnEspeces.setOnAction(e -> { modeChoisi[0] = "Espèces"; btnEspeces.setStyle(styleSelect); btnCB.setStyle(styleUnselect); btnAutre.setStyle(styleUnselect); });
                    btnCB.setOnAction(e -> { modeChoisi[0] = "Carte bancaire"; btnEspeces.setStyle(styleUnselect); btnCB.setStyle(styleSelect); btnAutre.setStyle(styleUnselect); });
                    btnAutre.setOnAction(e -> { modeChoisi[0] = "Autre"; btnEspeces.setStyle(styleUnselect); btnCB.setStyle(styleUnselect); btnAutre.setStyle(styleSelect); });

                    modesBox.getChildren().addAll(btnEspeces, btnCB, btnAutre);

                    HBox actionsBox = new HBox(15);
                    actionsBox.setAlignment(javafx.geometry.Pos.CENTER);
                    actionsBox.setStyle("-fx-padding: 15 0 0 0;");

                    Button btnAnnuler = new Button("Annuler");
                    btnAnnuler.getStyleClass().add("btn-danger");                    btnAnnuler.setPrefWidth(120);
                    btnAnnuler.setOnAction(e -> containerFacture.getChildren().setAll(labelSelectionTableMessage));

                    Button btnConfirmer = new Button("Confirmer le paiement & Générer le reçu");
                    btnConfirmer.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20;");
                    HBox.setHgrow(btnConfirmer, Priority.ALWAYS);
                    btnConfirmer.setMaxWidth(Double.MAX_VALUE);

                    btnConfirmer.setOnAction(e -> validerClotureFacture(idAddition, idTable, fieldReduc.getText(), modeChoisi[0]));

                    actionsBox.getChildren().addAll(btnAnnuler, btnConfirmer);

                    containerFacture.getChildren().addAll(headerBox, detailBox, lblMode, modesBox, bottomGrid, actionsBox);
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
                body.put("modePaiement", modePaiement);
                double reduc = 0;
                try { reduc = Double.parseDouble(reduction.trim()); } catch (Exception ignored) {}
                body.put("reduction", reduc);
                body.put("idAdmin", SessionManager.getInstance().getUserId());

                String resp = ApiClient.put("/additions/" + idAddition + "/payer", body);
                Platform.runLater(() -> {
                    loadTablesPourAdditions();
                    containerFacture.getChildren().setAll(labelSelectionTableMessage);
                    labelSelectionTableMessage.setText("✔ Paiement enregistré — table libérée !");
                    labelSelectionTableMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 15; -fx-font-weight: bold;");
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
        boolean libre = "LIBRE".equals(t.get("statut").toString());
        Long idTable  = Long.valueOf(t.get("idTable").toString());

        VBox card = new VBox(6);
        card.setPrefSize(155, 110);

        boolean selected = idTable.equals(selectedTableId);
        String bgColor = selected ? "#8e44ad"
                       : (libre  ? "#1e8449" : "#c0392b");

        card.setStyle("-fx-background-color:" + bgColor
                + ";-fx-background-radius:12;-fx-padding:12;-fx-cursor:hand;");

        Label num  = new Label("Table " + t.get("numero"));
        num.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14;");

        Label cap  = new Label("Capacité : " + t.get("capacite"));
        cap.setStyle("-fx-text-fill:white;-fx-font-size:11;");

        Object empl = t.get("emplacement");
        if (empl != null && !empl.toString().isEmpty()) {
            Label emplLabel = new Label(empl.toString());
            emplLabel.setStyle("-fx-text-fill:#ddd;-fx-font-size:10;");
            card.getChildren().add(emplLabel);
        }

        String statutTxt = libre ? "Libre" : "Occupée (" + t.get("nbPersonnes") + " pers.)";
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
            tableSelectionneeAdminLabel.setStyle("-fx-text-fill:#f39c12;-fx-font-size:12;");
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
                        setTableMsg("✔ Table ajoutée.", "#27ae60");
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
                        setTableMsg("✔ Table modifiée.", "#27ae60");
                        tableSelectionneeAdminLabel.setText("Cliquez sur une table pour la sélectionner.");
                        tableSelectionneeAdminLabel.setStyle("-fx-text-fill:#666;-fx-font-size:12;");
                        loadPlanSalle();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void supprimerTable() {
        if (selectedTableId == null) { setTableMsg("⚠ Sélectionnez une table.", "#e74c3c"); return; }
        Long id = selectedTableId;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette table ? Cette action est irréversible.", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        ApiClient.delete("/tables/" + id);
                        Platform.runLater(() -> {
                            selectedTableId = null;
                            clearTableFields();
                            setTableMsg("✔ Table supprimée.", "#27ae60");
                            tableSelectionneeAdminLabel.setText("Cliquez sur une table pour la sélectionner.");
                            tableSelectionneeAdminLabel.setStyle("-fx-text-fill:#666;-fx-font-size:12;");
                            loadPlanSalle();
                        });
                    } catch (Exception e) { e.printStackTrace(); }
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
        if (sel == null) { showAlert("Selectionnez un article."); return; }
        Long id = Long.valueOf(sel.get("idArticle").toString());
        new Thread(() -> {
            try {
                ApiClient.delete("/articles/" + id);
                Platform.runLater(this::loadMenu);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
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

    private void setupCommandesTable() {
        colCTable.setCellValueFactory(c -> {
            Object t = c.getValue().get("table");
            if (t instanceof Map) return new SimpleStringProperty("Table " + ((Map<?,?>)t).get("numero"));
            return new SimpleStringProperty("--");
        });

        colCStatut.setCellValueFactory(c -> {
            Object statut = c.getValue().get("statut");
            return new SimpleStringProperty(statut != null ? statut.toString() : "INCONNU");
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

        colCNotes.setCellValueFactory(c -> {
            Object notes = c.getValue().get("notes");
            return new SimpleStringProperty(notes != null ? notes.toString() : "");
        });

        commandesTable.setItems(commandeItems);
    }
    @FXML public void loadCommandes() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/commandes/actives");
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
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg); a.showAndWait();
    }
    private void masquerTousLesPanneaux() {
        var panneaux = java.util.List.of(paneTables, paneMenu, paneCommandes, paneStats, paneAdditions);

        for (VBox pane : panneaux) {
            if (pane != null) {
                pane.setVisible(false);
                pane.setManaged(false);
            }
        }
    }
}