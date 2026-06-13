package pt.carguru;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {

        primaryStage = stage;
        stage.setTitle("CarGuru");
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        navigateTo("Login");
        stage.show();
    }

    public static void navigateTo(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/pt/carguru/Views/" + fxmlName + "View.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                App.class.getResource("/pt/carguru/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro a carregar vista: " + fxmlName);
        }
    }

    public static Stage getStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
