package com.restaurantpro.fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("RestaurantPro");
        showLogin();
        stage.show();
    }

    public static void showLogin() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/fxml/LoginView.fxml"));
        Scene scene = new Scene(loader.load(), 500, 400);
        scene.getStylesheets().add(
                MainApp.class.getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
    }

    public static void showDashboard(String role) throws Exception {
        String fxml = switch (role) {
            case "ADMIN"     -> "/fxml/AdminDashboard.fxml";
            case "SERVEUR"   -> "/fxml/ServeurDashboard.fxml";
            case "CUISINIER" -> "/fxml/CuisinierDashboard.fxml";
            default          -> throw new IllegalArgumentException("Rôle inconnu: " + role);
        };
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(fxml));
        Scene scene = new Scene(loader.load(), 1200, 750);
        scene.getStylesheets().add(
                MainApp.class.getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
    }

    public static void main(String[] args) { launch(args); }
}
