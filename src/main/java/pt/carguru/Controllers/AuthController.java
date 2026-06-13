package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pt.carguru.App;
import pt.carguru.Services.AuthService;

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
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirm;
    @FXML private Label regErro;

    // Recuperar password
    @FXML private TextField forgotEmail;
    @FXML private Label forgotMensagem;

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
            authService.registar(regNome.getText(), regEmail.getText(),
                    regNif.getText(), regPassword.getText(), regConfirm.getText());
            mostrarAlerta(Alert.AlertType.INFORMATION, "Conta criada!", "Conta criada com sucesso! Faz login para continuar.");
            tabPane.getSelectionModel().selectFirst();
        } catch (Exception e) {
            regErro.setText(e.getMessage());
        }
    }

    @FXML
    public void handleRecuperarPassword() {
        forgotMensagem.setText("");
        try {
            String msg = authService.iniciarRecuperacaoPassword(forgotEmail.getText());
            forgotMensagem.setStyle("-fx-text-fill: green;");
            forgotMensagem.setText(msg);
        } catch (Exception e) {
            forgotMensagem.setStyle("-fx-text-fill: red;");
            forgotMensagem.setText(e.getMessage());
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
