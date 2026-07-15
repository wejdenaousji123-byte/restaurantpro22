package com.restaurantpro.fx.controller;

import com.restaurantpro.fx.MainApp;
import com.restaurantpro.fx.util.ApiClient;
import com.restaurantpro.fx.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Map;

public class LoginController {

    @FXML private TextField    emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label        errorLabel;
    @FXML private Button       loginBtn;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir votre email et mot de passe.");
            return;
        }

        loginBtn.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                Map<String, Object> body = Map.of("email", email, "password", password);
                String response = ApiClient.post("/auth/login", body);
                Map<String, Object> data = ApiClient.parseMap(response);

                if (data.containsKey("error")) {
                    Platform.runLater(() -> {
                        showError(data.get("error").toString());
                        loginBtn.setDisable(false);
                    });
                    return;
                }

                String token = data.get("token").toString();
                String role  = data.get("role").toString();
                String nom   = data.get("nom").toString();
                Long   id    = Long.valueOf(data.get("id").toString());

                SessionManager.getInstance().login(id, nom, role, token);

                Platform.runLater(() -> {
                    try {
                        MainApp.showDashboard(role);
                    } catch (Exception ex) {
                        showError("Erreur lors du chargement de l'interface.");
                        loginBtn.setDisable(false);
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("Impossible de contacter le serveur.");
                    loginBtn.setDisable(false);
                });
            }
        }).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
