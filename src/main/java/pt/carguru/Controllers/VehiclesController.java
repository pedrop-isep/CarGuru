package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Reserva;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.Session;

import java.time.LocalDate;
import java.util.List;

public class VehiclesController {
    @FXML private TextField fMarca;
    @FXML private ComboBox<String> fCombustivel;
    @FXML private ComboBox<String> fTransmissao;
    @FXML private TextField fLocalizacao;
    @FXML private TextField fPrecoMax;
    @FXML private FlowPane vehiclesGrid;

    private final VeiculoService veiculoService = new VeiculoService();
    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        fCombustivel.getItems().addAll("", "Gasolina", "Gasóleo", "Elétrico", "GPL", "Híbrido");
        fTransmissao.getItems().addAll("", "Manual", "Automática");
        carregarVeiculos();
    }

    @FXML
    public void carregarVeiculos() {
        try {
            String marca = fMarca.getText();
            String comb = fCombustivel.getValue();
            String trans = fTransmissao.getValue();
            String loc = fLocalizacao.getText();
            double precoMax = 0;
            try { precoMax = Double.parseDouble(fPrecoMax.getText()); } catch (Exception ignored) {}

            List<Veiculo> veiculos = veiculoService.pesquisarVeiculos(marca, comb, trans, loc, precoMax);
            vehiclesGrid.getChildren().clear();
            if (veiculos.isEmpty()) {
                vehiclesGrid.getChildren().add(new Label("Nenhum veículo encontrado."));
            } else {
                for (Veiculo v : veiculos) vehiclesGrid.getChildren().add(criarCardVeiculo(v));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox criarCardVeiculo(Veiculo v) {
        VBox card = new VBox(6);
        card.getStyleClass().add("vehicle-card");
        card.setPrefWidth(260);
        card.setPadding(new Insets(14));

        Label titulo = new Label("🚗 " + v.getNomeCompleto());
        titulo.getStyleClass().add("card-title");
        Label loc = new Label("📍 " + v.getLocalizacao());
        Label preco = new Label("💶 " + String.format("%.2f€/dia", v.getPrecoPorDia()));
        Label comb = new Label("⛽ " + v.getCombustivel() + "  •  " + v.getTransmissao());
        Label prop = new Label("👤 " + v.getProprietarioNome());

        Button btnDetalhes = new Button("Ver detalhes / Reservar");
        btnDetalhes.getStyleClass().add("btn-primary");
        btnDetalhes.setMaxWidth(Double.MAX_VALUE);
        btnDetalhes.setOnAction(e -> abrirModalReserva(v));

        card.getChildren().addAll(titulo, loc, preco, comb, prop, btnDetalhes);
        return card;
    }

    private void abrirModalReserva(Veiculo v) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reservar " + v.getNomeCompleto());
        dialog.setHeaderText("Período de reserva");

        DatePicker dpInicio = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker dpFim = new DatePicker(LocalDate.now().plusDays(3));
        Label totalLabel = new Label();

        Runnable calcTotal = () -> {
            if (dpInicio.getValue() != null && dpFim.getValue() != null && dpFim.getValue().isAfter(dpInicio.getValue())) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(dpInicio.getValue(), dpFim.getValue());
                totalLabel.setText(String.format("Total: %.2f€ (%d dias)", dias * v.getPrecoPorDia(), dias));
            }
        };
        dpInicio.setOnAction(e -> calcTotal.run());
        dpFim.setOnAction(e -> calcTotal.run());
        calcTotal.run();

        VBox content = new VBox(10,
            new Label("Início:"), dpInicio,
            new Label("Fim:"), dpFim,
            totalLabel);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    reservaService.criarReserva(v.getId(), dpInicio.getValue(), dpFim.getValue());
                    mostrarSucesso("Reserva submetida! Aguarda aprovação do proprietário.");
                } catch (Exception e) { mostrarErro(e.getMessage()); }
            }
        });
    }

    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaConta() { App.navigateTo("Conta"); }
    @FXML public void irParaReservas() { App.navigateTo("Reservas"); }
    @FXML public void irParaAdmin() { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout() { Session.clear(); App.navigateTo("Login"); }

    private void mostrarErro(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
    private void mostrarSucesso(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }
}
