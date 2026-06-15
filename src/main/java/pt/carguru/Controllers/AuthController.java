package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pt.carguru.App;
import pt.carguru.Services.AuthService;

import java.time.LocalDate;

public class AuthController {
    // Login
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginErro;

    // Registo
    @FXML private TextField regNome;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private TextField regNif;
    @FXML private TextField regCarta;
    @FXML private DatePicker regValidadeCarta;
    @FXML private Label regErro;

    // Recuperação
    @FXML private TextField recEmail;
    @FXML private Label recMensagem;
    @FXML private TextField recToken;
    @FXML private PasswordField recNovaPw;
    @FXML private PasswordField recConfirmPw;
    @FXML private Label recErro;
    @FXML private javafx.scene.layout.VBox recPhase2;
    @FXML private javafx.scene.layout.VBox recPhase1;

    private final AuthService authService = new AuthService();
    private String tokenAtual;

    @FXML
    public void initialize() {
        if (recPhase2 != null) { recPhase2.setVisible(false); recPhase2.setManaged(false); }
    }

    @FXML
    public void handleLogin() {
        limparErros();
        try {
            authService.login(loginEmail.getText(), loginPassword.getText());
            App.navigateTo("Home");
        } catch (Exception e) { loginErro.setText(e.getMessage()); }
    }

    @FXML
    public void handleRegistar() {
        limparErros();
        try {
            LocalDate validade = regValidadeCarta.getValue();
            authService.registar(regNome.getText(), regEmail.getText(), regPassword.getText(),
                regConfirmPassword.getText(), regNif.getText(), regCarta.getText(), validade);
            App.navigateTo("Home");
        } catch (Exception e) { regErro.setText(e.getMessage()); }
    }

    @FXML
    public void handleEnviarToken() {
        recMensagem.setText("");
        recErro.setText("");
        try {
            tokenAtual = authService.iniciarRecuperacao(recEmail.getText());
            // Em produção enviaria email; aqui mostra o token diretamente para demo
            recMensagem.setText("Token de recuperação: " + tokenAtual + "\n(Em produção seria enviado por email)");
            if (recPhase2 != null) {
                recPhase2.setVisible(true);
                recPhase2.setManaged(true);
            }
            if (recPhase1 != null) {
                recPhase1.setVisible(false);
                recPhase1.setManaged(false);
            }
        } catch (Exception e) { recErro.setText(e.getMessage()); }
    }

    @FXML
    public void handleRedefinirPassword() {
        recErro.setText("");
        try {
            String token = recToken != null && !recToken.getText().isBlank()
                ? recToken.getText() : tokenAtual;
            authService.redefinirPassword(token, recNovaPw.getText(), recConfirmPw.getText());
            recMensagem.setText("✅ Password redefinida com sucesso! Podes fazer login.");
            if (recPhase2 != null) { recPhase2.setVisible(false); recPhase2.setManaged(false); }
        } catch (Exception e) { recErro.setText(e.getMessage()); }
    }

    @FXML public void irParaHome() { App.navigateTo("Home"); }

    private void limparErros() {
        if (loginErro != null) loginErro.setText("");
        if (regErro != null) regErro.setText("");
        if (recErro != null) recErro.setText("");
    }
}
