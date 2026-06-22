package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import pt.carguru.App;
import pt.carguru.Services.AuthService;
import pt.carguru.Utils.FieldValidator;

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
    @FXML private Label regNomeErro;
    @FXML private Label regEmailErro;
    @FXML private Label regPasswordErro;
    @FXML private Label regConfirmPasswordErro;
    @FXML private Label regNifErro;
    @FXML private Label regCartaErro;
    @FXML private Label regValidadeCartaErro;

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
    private final FieldValidator regValidator = new FieldValidator();

    @FXML
    public void initialize() {
        if (recPhase2 != null) { recPhase2.setVisible(false); recPhase2.setManaged(false); }
        configurarValidacaoRegisto();
    }

    /** Liga a validação em tempo real a cada campo do formulário de registo. */
    private void configurarValidacaoRegisto() {
        if (regNome == null) return; // FXML sem estes campos (não deveria acontecer)

        regValidator.validarTexto(regNome, regNomeErro, v ->
            v.isBlank() ? "Nome obrigatório." : null);

        regValidator.validarTexto(regEmail, regEmailErro, v -> {
            if (v.isBlank()) return "Email obrigatório.";
            if (!v.contains("@") || !v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return "Email inválido.";
            return null;
        });

        regValidator.validarTexto(regPassword, regPasswordErro, v -> {
            if (v.isBlank()) return "Password obrigatória.";
            if (v.length() < 6) return "A password deve ter pelo menos 6 caracteres.";
            return null;
        });

        regValidator.validarTexto(regConfirmPassword, regConfirmPasswordErro, v -> {
            if (v.isBlank()) return "Confirmação obrigatória.";
            String pw = regPassword.getText() != null ? regPassword.getText() : "";
            if (!v.equals(pw)) return "As passwords não coincidem.";
            return null;
        });
        // Reavaliar a confirmação sempre que a password principal mudar.
        regPassword.textProperty().addListener((obs, oldV, newV) -> {
            String confirm = regConfirmPassword.getText();
            if (confirm != null && !confirm.isEmpty()) regValidator.isValid();
        });

        regValidator.validarTexto(regNif, regNifErro, v -> {
            if (v.isBlank()) return "NIF obrigatório.";
            if (!v.trim().matches("\\d{9}")) return "O NIF deve ter exatamente 9 dígitos.";
            return null;
        });

        regValidator.validarTexto(regCarta, regCartaErro, v ->
            v.isBlank() ? "Número de carta obrigatório." : null);

        regValidator.validarData(regValidadeCarta, regValidadeCartaErro, data -> {
            if (data == null) return "Data de validade obrigatória.";
            if (data.isBefore(LocalDate.now())) return "A carta está expirada — indica uma data futura.";
            return null;
        });
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
        regErro.setText("");
        if (!regValidator.isValid()) {
            regErro.setText("Corrige os campos assinalados a vermelho antes de continuar.");
            return;
        }
        try {
            LocalDate validade = regValidadeCarta.getValue();
            authService.registar(regNome.getText(), regEmail.getText(), regPassword.getText(),
                regConfirmPassword.getText(), regNif.getText(), regCarta.getText(), validade);
            App.navigateTo("Home");
        } catch (Exception e) {
            // Erros vindos do servidor/serviço (ex: "Já existe uma conta com este email.")
            // — os valores introduzidos pelo utilizador permanecem nos campos.
            regErro.setText(e.getMessage());
        }
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
