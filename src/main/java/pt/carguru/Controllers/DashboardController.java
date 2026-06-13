package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import pt.carguru.App;
import pt.carguru.Models.Reserva;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.Session;

import java.util.List;

public class DashboardController {
    @FXML private Label nomeLabel;
    @FXML private Label saldoLabel;
    @FXML private Label totalVeiculosLabel;
    @FXML private Label reservasAtivasLabel;
    @FXML private VBox veiculosBox;
    @FXML private VBox reservasBox;

    private final VeiculoService veiculoService = new VeiculoService();
    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        User user = Session.getUser();
        if (user == null) { App.navigateTo("Login"); return; }

        nomeLabel.setText("BEM-VINDO, " + user.getNome().toUpperCase() + "!");
        saldoLabel.setText(String.format("%.2f€", user.getSaldo()));

        try {
            List<Veiculo> veiculos = veiculoService.listarMeusVeiculos();
            totalVeiculosLabel.setText(String.valueOf(veiculos.size()));
            veiculosBox.getChildren().clear();
            if (veiculos.isEmpty()) {
                veiculosBox.getChildren().add(new Label("Ainda não anunciaste nenhum veículo."));
            } else {
                veiculos.stream().limit(3).forEach(v -> {
                    Label l = new Label("🚗 " + v.getNomeCompleto() + " — " + v.getEstado());
                    l.getStyleClass().add("dash-item");
                    veiculosBox.getChildren().add(l);
                });
            }

            List<Reserva> reservas = reservaService.minhasReservasComoLocatario();
            long ativas = reservas.stream().filter(r -> "confirmada".equals(r.getEstado()) || "pendente".equals(r.getEstado())).count();
            reservasAtivasLabel.setText(String.valueOf(ativas));
            reservasBox.getChildren().clear();
            if (reservas.isEmpty()) {
                reservasBox.getChildren().add(new Label("Ainda não tens reservas."));
            } else {
                reservas.stream().limit(3).forEach(r -> {
                    Label l = new Label("📅 " + r.getVeiculoNome() + " | " + r.getDataInicio() + " → " + r.getDataFim() + " | " + r.getEstado());
                    l.getStyleClass().add("dash-item");
                    reservasBox.getChildren().add(l);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML public void irParaVeiculos() { App.navigateTo("Vehicles"); }
    @FXML public void irParaReservas() { App.navigateTo("Reservas"); }
    @FXML public void irParaConta() { App.navigateTo("Conta"); }
    @FXML public void irParaAdmin() { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout() { Session.clear(); App.navigateTo("Login"); }
}
