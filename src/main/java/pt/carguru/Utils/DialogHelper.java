package pt.carguru.Utils;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pt.carguru.App;

import java.util.Optional;

public class DialogHelper {

    private static String css() {
        return DialogHelper.class
            .getResource("/pt/carguru/css/style.css").toExternalForm();
    }

    /** Aplica CSS dark + remove header branco + remove ícone ao Dialog */
    public static <T> Dialog<T> estilizar(Dialog<T> dialog) {
        dialog.getDialogPane().getStylesheets().add(css());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // Remove o ícone (? ! i) — substitui por nada
        dialog.getDialogPane().setGraphic(null);

        // Esconde o header panel se não tiver conteúdo útil
        // (nos Alert com header, deixamos; nos Dialog normais escondemos)
        if (dialog.getHeaderText() == null) {
            dialog.getDialogPane().setHeader(null);
        }

        // Aplica estilo à janela do dialog (remove decoração nativa branca)
        dialog.initStyle(StageStyle.TRANSPARENT);

        // Fundo externo transparente — a decoração é a do dialog-pane
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #111111; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: rgba(220,38,38,0.3); " +
            "-fx-border-radius: 16; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.9), 40, 0, 0, 16);"
        );

        // Herda ícone da janela principal
        try {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            if (App.getStage() != null && !App.getStage().getIcons().isEmpty()) {
                dialogStage.getIcons().setAll(App.getStage().getIcons());
            }
        } catch (Exception ignored) {}

        return dialog;
    }

    /** Alert estilizado sem header branco */
    private static Alert criarAlert(Alert.AlertType tipo, String titulo, String mensagem, ButtonType... botoes) {
        Alert a = new Alert(tipo, mensagem, botoes);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.getDialogPane().setGraphic(null);
        a.getDialogPane().setHeader(null);
        a.getDialogPane().getStylesheets().add(css());
        a.getDialogPane().getStyleClass().add("dialog-pane");
        a.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        // Título customizado na content area em vez do header nativo
        a.getDialogPane().setStyle(
            "-fx-background-color: #111111; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: rgba(220,38,38,0.3); " +
            "-fx-border-radius: 16; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.9), 40, 0, 0, 16);"
        );

        try {
            a.initStyle(StageStyle.TRANSPARENT);
        } catch (Exception ignored) {}

        return a;
    }

    public static void erro(String mensagem) {
        Alert a = criarAlert(Alert.AlertType.ERROR, "Erro", mensagem, ButtonType.OK);
        // Adiciona label de título vermelho acima da mensagem
        Label titulo = new Label("❌  Erro");
        titulo.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 1.05em;");
        Label msg = new Label(mensagem);
        msg.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.92em;");
        msg.setWrapText(true);
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, titulo, msg);
        box.setPadding(new Insets(4, 0, 4, 0));
        a.getDialogPane().setContent(box);
        a.showAndWait();
    }

    public static void sucesso(String mensagem) {
        Alert a = criarAlert(Alert.AlertType.INFORMATION, "Sucesso", mensagem, ButtonType.OK);
        Label titulo = new Label("✅  Sucesso");
        titulo.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 1.05em;");
        Label msg = new Label(mensagem);
        msg.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.92em;");
        msg.setWrapText(true);
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, titulo, msg);
        box.setPadding(new Insets(4, 0, 4, 0));
        a.getDialogPane().setContent(box);
        a.showAndWait();
    }

    public static Optional<ButtonType> confirmar(String titulo, String mensagem) {
        Alert a = criarAlert(Alert.AlertType.CONFIRMATION, titulo, mensagem, ButtonType.YES, ButtonType.NO);
        Label tituloLbl = new Label("⚠️  " + titulo);
        tituloLbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 1.05em;");
        Label msg = new Label(mensagem);
        msg.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.92em;");
        msg.setWrapText(true);
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, tituloLbl, msg);
        box.setPadding(new Insets(4, 0, 4, 0));
        a.getDialogPane().setContent(box);
        return a.showAndWait();
    }

    public static Optional<String> pedirTexto(String titulo, String header, String labelTxt) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(titulo);
        dlg.setHeaderText(null);
        dlg.setGraphic(null);
        dlg.getDialogPane().setHeader(null);
        dlg.getDialogPane().getStylesheets().add(css());
        dlg.getDialogPane().getStyleClass().add("dialog-pane");
        dlg.getDialogPane().setStyle(
            "-fx-background-color: #111111; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: rgba(220,38,38,0.3); " +
            "-fx-border-radius: 16; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.9), 40, 0, 0, 16);"
        );

        // Conteúdo customizado
        Label tituloLbl = new Label(titulo);
        tituloLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 1.05em;");
        Label lbl = new Label(labelTxt);
        lbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em; -fx-font-weight: bold;");
        TextField tf = dlg.getEditor();
        tf.getStyleClass().add("form-input");
        tf.setStyle(
            "-fx-background-color: #1e1e1e; -fx-border-color: rgba(255,255,255,0.12); " +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 9 12 9 12;"
        );
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, tituloLbl, lbl, tf);
        box.setPadding(new Insets(4, 0, 4, 0));
        dlg.getDialogPane().setContent(box);

        // Campo obrigatório: impede confirmar com o valor vazio.
        javafx.scene.Node btnOk = dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.setDisable(true);
            tf.textProperty().addListener((obs, oldV, newV) -> btnOk.setDisable(newV == null || newV.trim().isEmpty()));
        }

        try { dlg.initStyle(StageStyle.TRANSPARENT); } catch (Exception ignored) {}
        return dlg.showAndWait();
    }
}
