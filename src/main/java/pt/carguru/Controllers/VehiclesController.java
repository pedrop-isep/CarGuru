package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Indisponibilidade;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.Session;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VehiclesController {
    @FXML private TextField fMarca;
    @FXML private ComboBox<String> fCombustivel;
    @FXML private ComboBox<String> fTransmissao;
    @FXML private TextField fLocalizacao;
    @FXML private TextField fPrecoMax;
    @FXML private FlowPane vehiclesGrid;
    @FXML private Button btnAdmin;

    private final VeiculoService veiculoService = new VeiculoService();
    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        NavbarHelper.configurar(btnAdmin);
        fCombustivel.getItems().addAll("", "GASOLINA", "GASOLEO", "ELETRICO", "GPL", "HIBRIDO");
        fTransmissao.getItems().addAll("", "MANUAL", "AUTOMATICA");
        carregarVeiculos();
    }

    @FXML
    public void carregarVeiculos() {
        try {
            String marca = fMarca.getText();
            String comb  = fCombustivel.getValue();
            String trans = fTransmissao.getValue();
            String loc   = fLocalizacao.getText();
            double precoMax = 0;
            try { precoMax = Double.parseDouble(fPrecoMax.getText()); } catch (Exception ignored) {}

            List<Veiculo> veiculos = veiculoService.pesquisarVeiculos(marca, comb, trans, loc, precoMax);
            vehiclesGrid.getChildren().clear();

            if (veiculos.isEmpty()) {
                Label empty = new Label("🔍 Nenhum veículo encontrado com os filtros selecionados.");
                empty.getStyleClass().add("conta-email");
                empty.setPadding(new Insets(32));
                vehiclesGrid.getChildren().add(empty);
            } else {
                for (Veiculo v : veiculos)
                    vehiclesGrid.getChildren().add(criarCardVeiculo(v));
            }
        } catch (Exception e) { DialogHelper.erro(e.getMessage()); }
    }

    private VBox criarCardVeiculo(Veiculo v) {
        VBox card = new VBox(0);
        card.getStyleClass().add("vehicle-card");
        card.setPrefWidth(248);

        String emojiFuel = switch (v.getCombustivel() != null ? v.getCombustivel().toUpperCase() : "") {
            case "ELETRICO" -> "⚡";
            case "GPL"      -> "🔵";
            default         -> "🚗";
        };
        Label photo = new Label(emojiFuel);
        photo.getStyleClass().add("vehicle-photo-placeholder");
        photo.setMaxWidth(Double.MAX_VALUE);
        photo.setAlignment(Pos.CENTER);

        VBox body = new VBox(6);
        body.setPadding(new Insets(12, 12, 4, 12));

        Label titulo = new Label(v.getMarca() + " " + v.getModelo());
        titulo.getStyleClass().add("card-title");
        titulo.setWrapText(true);

        Label ano = new Label(v.getAno() + "  •  " + v.getLocalizacao());
        ano.getStyleClass().add("card-detail");

        Label preco = new Label(String.format("%.2f€/dia", v.getPrecoPorDia()));
        preco.getStyleClass().add("card-price");

        HBox badges = new HBox(6);
        Label badgeFuel = new Label(v.getCombustivel() != null ? v.getCombustivel() : "-");
        badgeFuel.getStyleClass().addAll("badge", "badge-fuel");
        Label badgeTrans = new Label(v.getTransmissao() != null ? v.getTransmissao() : "-");
        badgeTrans.getStyleClass().addAll("badge", "badge-trans");
        badges.getChildren().addAll(badgeFuel, badgeTrans);

        Label rating = new Label(v.getAvaliacaoStr());
        rating.getStyleClass().add("card-rating");

        body.getChildren().addAll(titulo, ano, preco, badges, rating);

        Button btnDetalhes = new Button("Ver detalhes / Reservar");
        btnDetalhes.getStyleClass().add("btn-primary");
        btnDetalhes.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(btnDetalhes, new Insets(8, 12, 12, 12));
        btnDetalhes.setOnAction(e -> abrirModalDetalhes(v));

        card.getChildren().addAll(photo, body, btnDetalhes);
        return card;
    }

    private void abrirModalDetalhes(Veiculo v) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(v.getMarca() + " " + v.getModelo() + " (" + v.getAno() + ")");

        // Detalhes lado esquerdo
        VBox detalhes = new VBox(10);
        detalhes.setPrefWidth(280);
        detalhes.getChildren().addAll(
            detalhe("🚗 Veículo",       v.getMarca() + " " + v.getModelo() + " " + v.getAno()),
            detalhe("⛽ Combustível",   v.getCombustivel()),
            detalhe("⚙️ Transmissão",  v.getTransmissao()),
            detalhe("📍 Localização",  v.getLocalizacao()),
            detalhe("💺 Lotação",       v.getLotacao() > 0 ? v.getLotacao() + " lugares" : "-"),
            detalhe("📏 Quilómetros",  v.getQuilometragem() + " km"),
            detalhe("💧 Consumo",       String.format("%.1f L/100km", v.getConsumo())),
            detalhe("⭐ Avaliação",    v.getAvaliacaoStr()),
            detalhe("👤 Proprietário", v.getProprietarioNome()),
            detalhe("💶 Preço base",   String.format("%.2f€/dia", v.getPrecoPorDia()))
        );
        if (v.getDescricao() != null && !v.getDescricao().isBlank())
            detalhes.getChildren().add(detalhe("📝 Descrição", v.getDescricao()));

        // Reserva lado direito
        VBox reservaBox = new VBox(10);
        reservaBox.setPrefWidth(260);

        Label lblReserva = new Label("📅 Fazer Reserva");
        lblReserva.getStyleClass().add("conta-card-title");

        Label lblInicio = new Label("Data de início:");
        lblInicio.getStyleClass().add("form-label");
        DatePicker dpInicio = new DatePicker(LocalDate.now().plusDays(1));

        Label lblFim = new Label("Data de fim:");
        lblFim.getStyleClass().add("form-label");
        DatePicker dpFim = new DatePicker(LocalDate.now().plusDays(3));

        Label totalLabel = new Label();
        totalLabel.getStyleClass().add("reserva-total");

        Runnable calcTotal = () -> {
            if (dpInicio.getValue() != null && dpFim.getValue() != null
                    && dpFim.getValue().isAfter(dpInicio.getValue())) {
                long dias = ChronoUnit.DAYS.between(dpInicio.getValue(), dpFim.getValue());
                totalLabel.setText(String.format("Total: %.2f€  (%d dias)", dias * v.getPrecoPorDia(), dias));
            } else {
                totalLabel.setText("Datas inválidas");
            }
        };
        dpInicio.setOnAction(e -> calcTotal.run());
        dpFim.setOnAction(e -> calcTotal.run());
        calcTotal.run();

        Button btnReservar = new Button("Solicitar Reserva →");
        btnReservar.getStyleClass().add("btn-primary");
        btnReservar.setMaxWidth(Double.MAX_VALUE);
        btnReservar.setOnAction(e -> {
            try {
                reservaService.criarReserva(v.getId(), dpInicio.getValue(), dpFim.getValue());
                dialog.close();
                DialogHelper.sucesso("Reserva submetida!\nAguarda aprovação do proprietário.");
            } catch (Exception ex) { DialogHelper.erro(ex.getMessage()); }
        });

        // Secção de indisponibilidades
        VBox indispBox = new VBox(6);
        Label lblIndisp = new Label("🚫 Dias indisponíveis:");
        lblIndisp.getStyleClass().add("form-label");
        indispBox.getChildren().add(lblIndisp);
        try {
            List<Indisponibilidade> periodos = veiculoService.listarIndisponibilidades(v.getId());
            if (periodos.isEmpty()) {
                Label semIndisp = new Label("Nenhum período bloqueado.");
                semIndisp.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 0.85em;");
                indispBox.getChildren().add(semIndisp);
            } else {
                for (Indisponibilidade ind : periodos) {
                    Label periodo = new Label("  📅 " + ind.getInicio() + " → " + ind.getFim());
                    periodo.setStyle("-fx-text-fill: #f87171; -fx-font-size: 0.85em;");
                    indispBox.getChildren().add(periodo);
                }
                Label aviso = new Label("⚠️ Não é possível reservar nestes períodos.");
                aviso.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 0.8em;");
                aviso.setWrapText(true);
                indispBox.getChildren().add(aviso);
            }
        } catch (Exception ex) {
            Label errLabel = new Label("Não foi possível carregar indisponibilidades.");
            errLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 0.8em;");
            indispBox.getChildren().add(errLabel);
        }

        reservaBox.getChildren().addAll(lblReserva, indispBox, new Separator(), lblInicio, dpInicio, lblFim, dpFim, totalLabel, btnReservar);

        HBox layout = new HBox(24, detalhes, new Separator(Orientation.VERTICAL), reservaBox);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #141414;");

        dialog.getDialogPane().setContent(layout);
        dialog.getDialogPane().setStyle("-fx-background-color: #141414;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Aplicar CSS dark theme
        DialogHelper.estilizar(dialog);
        dialog.showAndWait();
    }

    private VBox detalhe(String label, String valor) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        Label val = new Label(valor != null ? valor : "-");
        val.setWrapText(true);
        val.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.9em;");
        return new VBox(2, lbl, val);
    }

    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void irParaReservas()  { App.navigateTo("Reservas"); }
    @FXML public void irParaAdmin()     { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout()          { Session.clear(); App.navigateTo("Home"); }
}
