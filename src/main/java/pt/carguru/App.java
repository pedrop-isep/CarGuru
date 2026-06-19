package pt.carguru;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pt.carguru.Utils.CombustivelScheduler;
import pt.carguru.Utils.ScrollSpeedUtil;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("CarGuru — Aluguer Peer-to-Peer");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setWidth(1200);
        stage.setHeight(760);
        stage.setMaximized(true);
        CombustivelScheduler.getInstance().iniciar();
        navigateTo("Home");
        stage.show();
    }

    @Override
    public void stop() {
        CombustivelScheduler.getInstance().parar();
    }

    /** Navigate preserving window size and maximized state */
    public static void navigateTo(String viewName) {
        try {
            double w = primaryStage.getWidth();
            double h = primaryStage.getHeight();
            boolean max = primaryStage.isMaximized();

            FXMLLoader loader = new FXMLLoader(
                App.class.getResource("/pt/carguru/Views/" + viewName + "View.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                App.class.getResource("/pt/carguru/css/style.css").toExternalForm());
            ScrollSpeedUtil.aplicar(scene.getRoot());
            primaryStage.setScene(scene);

            if (!max) {
                primaryStage.setWidth(w);
                primaryStage.setHeight(h);
            }
            primaryStage.setMaximized(max);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[CarGuru] Erro a carregar vista: " + viewName);
        }
    }

    public static Stage getStage() { return primaryStage; }
    public static void main(String[] args) { launch(args); }
}
