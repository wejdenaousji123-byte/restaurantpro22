package com.restaurantpro.fx.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantpro.fx.MainApp;
import com.restaurantpro.fx.util.ApiClient;
import com.restaurantpro.fx.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.concurrent.*;

public class CuisinierDashboardController {

    @FXML private Label nomCuisinierLabel;
    @FXML private ListView<String> commandesListView;
    @FXML private TextArea detailArea;
    @FXML private Button btnEnPreparation, btnPrete;
    @FXML private Label statusBar;
    @FXML private HBox paneCommandes;
    @FXML private VBox paneNotifications, listeNotifications;
    @FXML private Button btnCommandesTab, btnNotifications;
    @FXML private Label badgeNotifCount;

    private final ObjectMapper mapper = new ObjectMapper();
    private ObservableList<String> commandeItems = FXCollections.observableArrayList();
    private List<Map<String,Object>> commandesRaw = new ArrayList<>();
    private Long selectedCommandeId = null;
    private ScheduledExecutorService scheduler;

    @FXML
    public void initialize() {
        nomCuisinierLabel.setText(SessionManager.getInstance().getNom());
        commandesListView.setItems(commandeItems);
        detailArea.getStyleClass().add("ticket-cuisine");
        commandesListView.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
            int idx = n.intValue();
            if (idx >= 0 && idx < commandesRaw.size()) afficherDetail(commandesRaw.get(idx));
        });
        btnEnPreparation.setDisable(true);
        btnPrete.setDisable(true);
        showCommandesTab();
        loadCommandes();
        rafraichirBadgeNotifications();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            loadCommandes();
            rafraichirBadgeNotifications();
        }), 20, 20, TimeUnit.SECONDS);
    }

    private void showPane(javafx.scene.Node pane) {
        paneCommandes.setVisible(false);
        if (paneNotifications != null) paneNotifications.setVisible(false);
        pane.setVisible(true);
    }

    @FXML public void showCommandesTab() {
        showPane(paneCommandes);
        btnCommandesTab.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        if (btnNotifications != null) btnNotifications.setStyle("-fx-background-color:transparent;-fx-text-fill:#c3cadb;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
    }

    @FXML public void showNotifications() {
        showPane(paneNotifications);
        btnNotifications.setStyle("-fx-background-color:#223760;-fx-text-fill:white;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        btnCommandesTab.setStyle("-fx-background-color:transparent;-fx-text-fill:#c3cadb;-fx-alignment:CENTER_LEFT;-fx-padding:10 16;-fx-background-radius:8;");
        loadNotifications();
    }

    private void loadCommandes() {
        new Thread(() -> {
            try {
                String json = ApiClient.get("/commandes/actives");
                List<Map<String,Object>> cmds = mapper.readValue(json, new TypeReference<>(){});
                cmds.removeIf(c -> "PRETE".equals(c.get("statut")) || "SERVIE".equals(c.get("statut")));

                List<String> labels = new ArrayList<>();
                for (Map<String,Object> c : cmds) {
                    Map<?,?> t = (Map<?,?>) c.get("table");
                    String statut = c.get("statut").toString();
                    String couleur = "EN_ATTENTE".equals(statut) ? "[ATTENTE]" : "[PREP.]";
                    Object heureObj = c.get("heureCreation");
                    String heure = (heureObj != null) ? heureObj.toString() : "";                    if (heure.length() >= 16) heure = heure.substring(11,16);
                    labels.add(couleur + " #" + c.get("idCommande") + "  Table " + t.get("numero") + "  -  " + heure);
                }
                Platform.runLater(() -> {
                    commandesRaw = cmds;
                    commandeItems.setAll(labels);
                    System.out.println("DEBUG loadCommandes: " + cmds.size() + " commandes chargées");
                    for (Map<String,Object> c : cmds) {
                        List<?> lg = (List<?>) c.get("lignes");
                        System.out.println("  -> Commande #" + c.get("idCommande") + " lignes=" + (lg == null ? "NULL" : lg.size()));
                    }
                    statusBar.setText("Actualise  -  " + cmds.size() + " commande(s) active(s)");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusBar.setText("Erreur de connexion au serveur"));
            }
        }).start();
    }

    private void afficherDetail(Map<String,Object> cmd) {
        if (cmd == null) return;

        System.out.println("DEBUG afficherDetail: commande #" + cmd.get("idCommande"));
        List<?> lignes = (List<?>) cmd.get("lignes");
        System.out.println("DEBUG lignes = " + (lignes == null ? "NULL" : lignes.size()));

        selectedCommandeId = Long.valueOf(cmd.get("idCommande").toString());
        StringBuilder sb = new StringBuilder();

        sb.append("Commande   : #").append(cmd.get("idCommande")).append("\n");

        Map<?,?> table = (Map<?,?>) cmd.get("table");
        if (table != null) {
            sb.append("Table      : ").append(table.get("numero")).append("\n");
        } else {
            sb.append("Table      : Inconnue\n");
        }

        sb.append("Statut     : ").append(cmd.getOrDefault("statut", "ATTENTE")).append("\n");

        Object heureObj = cmd.get("heureCreation");
        if (heureObj != null) {
            String heure = heureObj.toString();
            if (heure.length() >= 16) heure = heure.substring(0,16).replace("T"," ");
            sb.append("Heure      : ").append(heure).append("\n");
        }

        Object notesObj = cmd.get("notes");
        if (notesObj != null && !notesObj.toString().trim().isEmpty() && !notesObj.toString().equals("null")) {
            sb.append("⚠️ NOTE : ").append(notesObj.toString()).append("\n");
        }

        sb.append("\n--- Articles ---\n");

        if (lignes != null && !lignes.isEmpty()) {
            for (Object l : lignes) {
                Map<?,?> ligne   = (Map<?,?>) l;
                Map<?,?> article = (Map<?,?>) ligne.get("article");
                String nomPlat;
                if (article != null && article.get("nom") != null) {
                    nomPlat = article.get("nom").toString();
                } else if (ligne.get("nomArticle") != null) {
                    nomPlat = ligne.get("nomArticle").toString();
                } else {
                    nomPlat = "Article supprimé";
                }

                sb.append("  • [ x").append(ligne.get("quantite")).append(" ]  ")
                        .append(nomPlat);

                Object ns = ligne.get("noteSpeciale");
                if (ns != null && !ns.toString().trim().isEmpty() && !ns.toString().equals("null"))
                    sb.append("  (").append(ns).append(")");
                sb.append("\n");
            }
        } else {
            sb.append("  (aucun article)\n");
        }

        detailArea.setText(sb.toString());

        String statut = cmd.getOrDefault("statut", "").toString();
        btnEnPreparation.setDisable(!"EN_ATTENTE".equals(statut));
        btnPrete.setDisable(!"EN_PREPARATION".equals(statut));
    }

    @FXML void marquerEnPreparation() { changerStatut("EN_PREPARATION"); }
    @FXML void marquerPrete()         { changerStatut("PRETE"); }

    private void changerStatut(String statut) {
        if (selectedCommandeId == null) return;
        new Thread(() -> {
            try {
                ApiClient.put("/commandes/" + selectedCommandeId + "/statut",
                        Map.of("statut", statut));
                Platform.runLater(() -> {
                    loadCommandes();
                    detailArea.setText("Statut mis a jour : " + statut);
                    btnEnPreparation.setDisable(true);
                    btnPrete.setDisable(true);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }


    private void rafraichirBadgeNotifications() {
        Long idCuisinier = SessionManager.getInstance().getUserId();
        if (idCuisinier == null) return;
        new Thread(() -> {
            try {
                String json = ApiClient.get("/notifications/user/" + idCuisinier + "/count");
                Map<String, Object> m = mapper.readValue(json, new TypeReference<>(){});
                long count = ((Number) m.getOrDefault("count", 0)).longValue();
                Platform.runLater(() -> {
                    if (badgeNotifCount == null) return;
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
        Long idCuisinier = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            try {
                String json = ApiClient.get("/notifications/user/" + idCuisinier);
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
            case "ALERTE_ATTENTE"    -> "⏰";
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
        Long idCuisinier = SessionManager.getInstance().getUserId();
        new Thread(() -> {
            try {
                ApiClient.put("/notifications/user/" + idCuisinier + "/lire-tout", Map.of());
                Platform.runLater(this::loadNotifications);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML void logout() {
        if (scheduler != null) scheduler.shutdownNow();
        SessionManager.getInstance().logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }
}
