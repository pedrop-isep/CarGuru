package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pt.carguru.App;
import pt.carguru.Services.AuthService;
import javafx.scene.layout.VBox;

public class AuthController {
    @FXML private TabPane tabPane;

    // Login
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginErro;

    // Registo
    @FXML private TextField regNome;
    @FXML private TextField regEmail;
    @FXML private TextField regNif;
    @FXML private TextField regCartaNumero;
    @FXML private DatePicker regCartaValidade;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label regErro;

    // Recuperar password
    @FXML private VBox painelEmail;
    @FXML private VBox painelToken;
    @FXML private TextField forgotEmail;
    @FXML private Label forgotMensagem;
    @FXML private TextField forgotToken;
    @FXML private PasswordField forgotNovaPassword;
    @FXML private PasswordField forgotConfirmar;
    @FXML private Label forgotTokenMensagem;

    private final AuthService authService = new AuthService();

    @FXML
    public void handleLogin() {
        loginErro.setText("");
        try {
            authService.login(loginEmail.getText(), loginPassword.getText());
            App.navigateTo("Dashboard");
        } catch (Exception e) {
            loginErro.setText(e.getMessage());
        }
    }

    @FXML
    public void handleRegisto() {
        regErro.setText("");
        try {
            String validadeStr = regCartaValidade.getValue() != null
                    ? regCartaValidade.getValue().toString()
                    : "";

            authService.registar(
                    regNome.getText(),
                    regEmail.getText(),
                    regNif.getText(),
                    regCartaNumero.getText(),
                    validadeStr,
                    regPassword.getText(),
                    regConfirm.getText()
            );
            mostrarAlerta(Alert.AlertType.INFORMATION, "Conta criada!", "Conta criada com sucesso! Faz login para continuar.");
            tabPane.getSelectionModel().selectFirst();
        } catch (Exception e) {
            regErro.setText(e.getMessage());
        }
    }

    @FXML
    public void handleEnviarToken() {
        forgotMensagem.setText("");
        try {
            authService.iniciarRecuperacaoPassword(forgotEmail.getText());
            forgotMensagem.setStyle("-fx-text-fill: green;");
            forgotMensagem.setText("Código enviado! Verifica o teu email.");
            // Mostra o passo 2
            painelEmail.setVisible(false);
            painelEmail.setManaged(false);
            painelToken.setVisible(true);
            painelToken.setManaged(true);
        } catch (Exception e) {
            forgotMensagem.setStyle("-fx-text-fill: red;");
            forgotMensagem.setText(e.getMessage());
        }
    }

    @FXML
    public void handleRedefinirPassword() {
        forgotTokenMensagem.setText("");
        try {
            authService.concluirRecuperacaoPassword(
                    forgotToken.getText(),
                    forgotNovaPassword.getText(),
                    forgotConfirmar.getText()
            );
            forgotTokenMensagem.setStyle("-fx-text-fill: green;");
            forgotTokenMensagem.setText("Password alterada com sucesso! Faz login.");
            // Volta ao passo 1
            painelToken.setVisible(false);
            painelToken.setManaged(false);
            painelEmail.setVisible(true);
            painelEmail.setManaged(true);
            tabPane.getSelectionModel().selectFirst();
        } catch (Exception e) {
            forgotTokenMensagem.setStyle("-fx-text-fill: red;");
            forgotTokenMensagem.setText(e.getMessage());
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensagem);
        a.showAndWait();
    }
}
