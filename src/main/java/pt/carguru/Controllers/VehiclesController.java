package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Indisponibilidade;
import pt.carguru.Models.Veiculo;
import pt.carguru.Repositories.VeiculoRepository;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.Session;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VehiclesController {
    @FXML private TextField fMarca;
    @FXML private ComboBox<String> fCombustivel;
    @FXML private ComboBox<String> fTransmissao;
    @FXML private TextField fLocalizacao;
    @FXML private TextField fPrecoMax;
    @FXML private TextField fLotacaoMin;
    @FXML private ComboBox<String> fOrdenacao;
    @FXML private DatePicker fDataInicio;
    @FXML private DatePicker fDataFim;
    @FXML private FlowPane vehiclesGrid;
    @FXML private Button btnAdmin;

    private final VeiculoService veiculoService = new VeiculoService();
    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        NavbarHelper.configurar(btnAdmin);
        fCombustivel.getItems().addAll("", "GASOLINA", "GASOLEO", "ELETRICO", "GPL", "HIBRIDO");
        fTransmissao.getItems().addAll("", "MANUAL", "AUTOMATICA");
        fOrdenacao.getItems().addAll("Avaliação ↓", "Preço ↑", "Preço ↓");
        fOrdenacao.setValue("Avaliação ↓");
        // Aplicar a ordenação automaticamente assim que o utilizador escolhe uma opção,
        // sem precisar de clicar em "Pesquisar".
        fOrdenacao.valueProperty().addListener((obs, oldVal, newVal) -> carregarVeiculos());
        carregarVeiculos();
    }

    @FXML
    public void carregarVeiculos() {
        try {
            String marca    = fMarca.getText();
            String comb     = fCombustivel.getValue();
            String trans    = fTransmissao.getValue();
            String loc      = fLocalizacao.getText();
            double precoMax = 0;
            try { precoMax = Double.parseDouble(fPrecoMax.getText()); } catch (Exception ignored) {}

            LocalDate dInicio = fDataInicio != null ? fDataInicio.getValue() : null;
            LocalDate dFim    = fDataFim    != null ? fDataFim.getValue()    : null;

            int lotMin = 0;
            try { lotMin = Integer.parseInt(fLotacaoMin != null ? fLotacaoMin.getText() : ""); } catch (Exception ignored) {}
            final int lotacaoMin = lotMin;

            List<Veiculo> veiculos;
            if (dInicio != null && dFim != null && dFim.isAfter(dInicio)) {
                veiculos = veiculoService.pesquisarDisponiveisPorDatas(marca, comb, trans, loc, precoMax, dInicio, dFim);
            } else {
                veiculos = veiculoService.pesquisarVeiculos(marca, comb, trans, loc, precoMax);
            }

            // Filtro lotação (client-side)
            if (lotacaoMin > 0) {
                veiculos = veiculos.stream().filter(v -> v.getLotacao() >= lotacaoMin).collect(java.util.stream.Collectors.toList());
            }

            // Ordenação
            String ord = fOrdenacao != null ? fOrdenacao.getValue() : "Avaliação ↓";
            if ("Preço ↑".equals(ord)) {
                veiculos.sort(java.util.Comparator.comparingDouble(Veiculo::getPrecoPorDia));
            } else if ("Preço ↓".equals(ord)) {
                veiculos.sort(java.util.Comparator.comparingDouble(Veiculo::getPrecoPorDia).reversed());
            } else {
                veiculos.sort(java.util.Comparator.comparingDouble(Veiculo::getAvaliacaoMedia).reversed());
            }

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

    @FXML
    public void limparFiltros() {
        fMarca.clear();
        fCombustivel.setValue("");
        fTransmissao.setValue("");
        fLocalizacao.clear();
        fPrecoMax.clear();
        if (fLotacaoMin != null) fLotacaoMin.clear();
        if (fDataInicio != null) fDataInicio.setValue(null);
        if (fDataFim    != null) fDataFim.setValue(null);
        // Não usamos setValue aqui sem verificar primeiro, para não disparar o
        // listener de ordenação (que já chama carregarVeiculos) e pesquisar duas vezes.
        if (fOrdenacao != null && !"Avaliação ↓".equals(fOrdenacao.getValue())) {
            fOrdenacao.setValue("Avaliação ↓");
            return; // o listener já trata de chamar carregarVeiculos()
        }
        carregarVeiculos();
    }

    private VBox criarCardVeiculo(Veiculo v) {
        VBox card = new VBox(0);
        card.getStyleClass().add("vehicle-card");
        card.setPrefWidth(248);

        // Imagem ou placeholder emoji
        File imgFile = new File(VeiculoRepository.resolverPathImagem(v.getId()));
        if (imgFile.exists()) {
            try {
                ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
                iv.setFitWidth(248);
                iv.setFitHeight(148);
                iv.setPreserveRatio(false);
                iv.setStyle("-fx-background-radius: 12 12 0 0;");
                card.getChildren().add(iv);
            } catch (Exception ex) {
                card.getChildren().add(buildEmojiPlaceholder(v));
            }
        } else {
            card.getChildren().add(buildEmojiPlaceholder(v));
        }

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
        Label badgeFuel  = new Label(v.getCombustivel() != null ? v.getCombustivel() : "-");
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

        card.getChildren().addAll(body, btnDetalhes);
        return card;
    }

    private Label buildEmojiPlaceholder(Veiculo v) {
        String emojiFuel = switch (v.getCombustivel() != null ? v.getCombustivel().toUpperCase() : "") {
            case "ELETRICO" -> "⚡";
            case "GPL"      -> "🔵";
            default         -> "🚗";
        };
        Label photo = new Label(emojiFuel);
        photo.getStyleClass().add("vehicle-photo-placeholder");
        photo.setMaxWidth(Double.MAX_VALUE);
        photo.setAlignment(Pos.CENTER);
        return photo;
    }

    private void abrirModalDetalhes(Veiculo v) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(v.getMarca() + " " + v.getModelo() + " (" + v.getAno() + ")");

        // Detalhes lado esquerdo
        VBox detalhes = new VBox(10);
        detalhes.setPrefWidth(280);

        // Imagem no topo do modal
        File imgFile = new File(VeiculoRepository.resolverPathImagem(v.getId()));
        if (imgFile.exists()) {
            try {
                ImageView iv = new ImageView(new Image(imgFile.toURI().toString()));
                iv.setFitWidth(270);
                iv.setFitHeight(160);
                iv.setPreserveRatio(false);
                iv.setStyle("-fx-background-radius: 10;");
                detalhes.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

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

        Label lblInicio = new Label("Data de início:");
        lblInicio.getStyleClass().add("form-label");
        LocalDate defaultInicio = (fDataInicio != null && fDataInicio.getValue() != null)
            ? fDataInicio.getValue() : LocalDate.now().plusDays(1);
        DatePicker dpInicio = new DatePicker(defaultInicio);

        Label lblFim = new Label("Data de fim:");
        lblFim.getStyleClass().add("form-label");
        LocalDate defaultFim = (fDataFim != null && fDataFim.getValue() != null)
            ? fDataFim.getValue() : defaultInicio.plusDays(2);
        DatePicker dpFim = new DatePicker(defaultFim);

        Label totalLabel = new Label();
        totalLabel.getStyleClass().add("reserva-total");
        Label decompLabel = new Label();
        decompLabel.setWrapText(true);
        decompLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.82em;");

        Runnable calcTotal = () -> {
            if (dpInicio.getValue() != null && dpFim.getValue() != null
                    && dpFim.getValue().isAfter(dpInicio.getValue())) {
                long dias = ChronoUnit.DAYS.between(dpInicio.getValue(), dpFim.getValue());
                double precoDin = calcularPrecoDinamicoLocal(v.getPrecoPorDia(), dpInicio.getValue(), dpFim.getValue(), dias);
                double rendaTotal = precoDin * dias;
                double caucao = rendaTotal * 0.20;
                // Estimativa combustível com 100 km por dia
                double kmsPrev = dias * 100.0;
                double custoCombEst = (kmsPrev / 100.0) * v.getConsumo() * 1.70;

                totalLabel.setText(String.format("Total estimado: %.2f€  (%d dias × %.2f€/dia)", rendaTotal, dias, precoDin));
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("  - Renda: %.2f€\n", rendaTotal));
                sb.append(String.format("  - Caucao (20%%): %.2f€ (devolvida no fim)\n", caucao));
                sb.append(String.format("  - Combustivel est. (%.0f km): %.2f€\n", kmsPrev, custoCombEst));
                // Mostrar fatores ativos
                if (temEpocaAlta(dpInicio.getValue(), dpFim.getValue())) sb.append("  +30% epoca alta\n");
                if (temFimSemana(dpInicio.getValue(), dpFim.getValue()))  sb.append("  +20% fim de semana\n");
                if (dias >= 7) sb.append("  -10% desconto >=7 dias\n");
                decompLabel.setText(sb.toString().trim());
            } else {
                totalLabel.setText("Datas inválidas");
                decompLabel.setText("");
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

        reservaBox.getChildren().addAll(lblReserva, indispBox, new Separator(),
            lblInicio, dpInicio, lblFim, dpFim, totalLabel, decompLabel, btnReservar);

        // Secção de avaliações/comentários anteriores
        VBox avaliacoesBox = new VBox(8);
        avaliacoesBox.setPrefWidth(280);
        Label lblAvals = new Label("⭐ Avaliações de locatários anteriores");
        lblAvals.getStyleClass().add("conta-card-title");
        avaliacoesBox.getChildren().add(lblAvals);
        try {
            java.sql.Connection connAv = pt.carguru.Utils.DatabaseConnection.getConnection();
            java.sql.PreparedStatement psAv = connAv.prepareStatement(
                "SELECT av.estrelas, av.comentario, u.nome AS avaliador, av.data " +
                "FROM avaliacoes av " +
                "JOIN alugueres al ON av.aluguer_id = al.id " +
                "JOIN reservas rv ON al.reserva_id = rv.id " +
                "JOIN utilizadores u ON av.avaliador_id = u.id " +
                "WHERE rv.veiculo_id = ? AND av.tipo = 'LOCATARIO' ORDER BY av.data DESC LIMIT 10");
            psAv.setInt(1, v.getId());
            java.sql.ResultSet rsAv = psAv.executeQuery();
            boolean temAvals = false;
            while (rsAv.next()) {
                temAvals = true;
                int estrelas = rsAv.getInt("estrelas");
                String coment = rsAv.getString("comentario");
                String avaliador = rsAv.getString("avaliador");
                VBox avCard = new VBox(2);
                avCard.setStyle("-fx-background-color: #1e1e1e; -fx-padding: 8; -fx-background-radius: 6;");
                Label starsLbl = new Label("★".repeat(estrelas) + "☆".repeat(5 - estrelas) + "  " + avaliador);
                starsLbl.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 0.9em;");
                avCard.getChildren().add(starsLbl);
                if (coment != null && !coment.isBlank()) {
                    Label commentLbl = new Label("\"" + coment + "\"");
                    commentLbl.setWrapText(true);
                    commentLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.82em; -fx-font-style: italic;");
                    avCard.getChildren().add(commentLbl);
                }
                avaliacoesBox.getChildren().add(avCard);
            }
            if (!temAvals) {
                Label sem = new Label("Ainda sem avaliações.");
                sem.setStyle("-fx-text-fill: #666; -fx-font-size: 0.85em;");
                avaliacoesBox.getChildren().add(sem);
            }
            rsAv.close(); psAv.close(); connAv.close();
        } catch (Exception exAv) {
            Label errAv = new Label("Não foi possível carregar avaliações.");
            errAv.setStyle("-fx-text-fill: #666; -fx-font-size: 0.82em;");
            avaliacoesBox.getChildren().add(errAv);
        }

        HBox layout = new HBox(24, detalhes, new Separator(Orientation.VERTICAL), avaliacoesBox, new Separator(Orientation.VERTICAL), reservaBox);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #141414;");

        ScrollPane sp = new ScrollPane(layout);
        sp.setFitToWidth(true);
        sp.setPrefHeight(560);
        sp.setStyle("-fx-background-color: #141414; -fx-background: #141414;");

        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().setStyle("-fx-background-color: #141414;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
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

    // ---- Helpers preço dinâmico (local, sem depender do service) ----
    private double calcularPrecoDinamicoLocal(double base, java.time.LocalDate inicio, java.time.LocalDate fim, long dias) {
        double f = 1.0;
        if (temEpocaAlta(inicio, fim)) f += 0.30;
        if (temFimSemana(inicio, fim)) f += 0.20;
        if (dias >= 7) f -= 0.10;
        return base * f;
    }

    private boolean temEpocaAlta(java.time.LocalDate inicio, java.time.LocalDate fim) {
        java.time.LocalDate d = inicio;
        while (!d.isAfter(fim)) {
            int m = d.getMonthValue();
            if (m == 6 || m == 7 || m == 8 || m == 12) return true;
            d = d.plusDays(1);
        }
        return false;
    }

    private boolean temFimSemana(java.time.LocalDate inicio, java.time.LocalDate fim) {
        java.time.LocalDate d = inicio;
        while (!d.isAfter(fim)) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) return true;
            d = d.plusDays(1);
        }
        return false;
    }

    @FXML public void irParaHome()      { App.navigateTo("Home"); }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void irParaReservas()  { App.navigateTo("Reservas"); }
    @FXML public void irParaAdmin()     { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout()          { Session.clear(); App.navigateTo("Home"); }
}
