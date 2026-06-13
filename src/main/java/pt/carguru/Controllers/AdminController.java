package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Reserva;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.UserService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.Session;

import java.util.List;

public class AdminController {
    @FXML private VBox veiculosPendentesList;
    @FXML private VBox utilizadoresList;
    @FXML private VBox historicoList;

    private final VeiculoService veiculoService = new VeiculoService();
    private final UserService userService = new UserService();
    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        if (!Session.isAdmin()) { App.navigateTo("Dashboard"); return; }
        carregarVeiculosPendentes();
        carregarUtilizadores();
        carregarHistorico();
    }

    private void carregarVeiculosPendentes() {
        try {
            List<Veiculo> pendentes = veiculoService.listarPendentes();
            veiculosPendentesList.getChildren().clear();
            if (pendentes.isEmpty()) {
                veiculosPendentesList.getChildren().add(new Label("Não há veículos pendentes."));
            } else {
                for (Veiculo v : pendentes) {
                    HBox row = new HBox(12);
                    row.setPadding(new Insets(8));
                    row.getStyleClass().add("admin-row");
                    Label info = new Label(String.format("🚗 %s | 👤 %s | 📍 %s | %.2f€/dia",
                            v.getNomeCompleto(), v.getProprietarioNome(), v.getLocalizacao(), v.getPrecoPorDia()));
                    info.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Button btnAprovar = new Button("✅ Aprovar");
                    Button btnRejeitar = new Button("❌ Rejeitar");
                    btnAprovar.setOnAction(e -> {
                        try { veiculoService.aprovarVeiculo(v.getId()); carregarVeiculosPendentes(); mostrarSucesso("Veículo aprovado!"); }
                        catch (Exception ex) { mostrarErro(ex.getMessage()); }
                    });
                    btnRejeitar.setOnAction(e -> {
                        try { veiculoService.rejeitarVeiculo(v.getId()); carregarVeiculosPendentes(); }
                        catch (Exception ex) { mostrarErro(ex.getMessage()); }
                    });
                    row.getChildren().addAll(info, btnAprovar, btnRejeitar);
                    veiculosPendentesList.getChildren().add(row);
                }
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private void carregarUtilizadores() {
        try {
            List<User> users = userService.listarTodos();
            utilizadoresList.getChildren().clear();
            for (User u : users) {
                HBox row = new HBox(12);
                row.setPadding(new Insets(8));
                row.getStyleClass().add("admin-row");
                Label info = new Label(String.format("👤 %s | %s | NIF: %s | Saldo: %.2f€ | %s",
                        u.getNome(), u.getEmail(), u.getNif() != null ? u.getNif() : "-",
                        u.getSaldo(), u.isAtivo() ? "Ativo" : "Suspenso"));
                info.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(info, Priority.ALWAYS);
                Button btnToggle = new Button(u.isAtivo() ? "🚫 Suspender" : "✅ Reativar");
                btnToggle.setOnAction(e -> {
                    try { userService.toggleAtivo(u.getId()); carregarUtilizadores(); }
                    catch (Exception ex) { mostrarErro(ex.getMessage()); }
                });
                row.getChildren().addAll(info, btnToggle);
                utilizadoresList.getChildren().add(row);
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private void carregarHistorico() {
        try {
            List<Reserva> reservas = reservaService.listarTodas();
            historicoList.getChildren().clear();
            if (reservas.isEmpty()) {
                historicoList.getChildren().add(new Label("Sem reservas no histórico."));
            } else {
                for (Reserva r : reservas) {
                    Label l = new Label(String.format("#%d | %s | %s → %s | %.2f€ | %s → %s",
                            r.getId(), r.getVeiculoNome(), r.getDataInicio(), r.getDataFim(),
                            r.getTotal(), r.getLocatarioNome(), r.getEstado().toUpperCase()));
                    l.getStyleClass().add("admin-row");
                    l.setPadding(new Insets(6));
                    l.setWrapText(true);
                    historicoList.getChildren().add(l);
                }
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos() { App.navigateTo("Vehicles"); }
    @FXML public void irParaConta() { App.navigateTo("Conta"); }
    @FXML public void logout() { Session.clear(); App.navigateTo("Login"); }

    private void mostrarErro(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void mostrarSucesso(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
