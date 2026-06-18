package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import pt.carguru.App;
import pt.carguru.Models.Reserva;
import pt.carguru.Services.DisputaService;
import pt.carguru.Services.ReservaService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.NavbarHelper;
import pt.carguru.Utils.Session;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReservasController {
    @FXML private VBox reservasLocatarioList;
    @FXML private VBox reservasProprietarioList;
    @FXML private Button btnAdmin;

    // Filtros do tab Locatário
    private DatePicker filtroLocDe;
    private DatePicker filtroLocAte;
    private List<Reserva> cacheLocatario = new ArrayList<>();

    // Filtros do tab Proprietário
    private DatePicker filtroPropDe;
    private DatePicker filtroPropAte;
    private List<Reserva> cacheProprietario = new ArrayList<>();

    private final ReservaService reservaService = new ReservaService();
    private final DisputaService disputaService = new DisputaService();

    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        if (Session.getUser() == null) { App.navigateTo("Login"); return; }
        NavbarHelper.configurar(btnAdmin);
        injetarBarraLocatario();
        injetarBarraProprietario();
        carregarLocatario(null, null);
        carregarProprietario(null, null);
    }

    // ── Locatário ────────────────────────────────────────────────────────────

    private void injetarBarraLocatario() {
        javafx.scene.Parent parent = reservasLocatarioList.getParent();
        if (!(parent instanceof VBox card)) return;

        filtroLocDe  = datePicker("Data início");
        filtroLocAte = datePicker("Data fim");

        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.getStyleClass().add("btn-primary");
        btnFiltrar.setOnAction(e -> aplicarFiltrosLocatario());

        Button btnLimpar = new Button("✕ Limpar");
        estilizarBtnLimpar(btnLimpar);
        btnLimpar.setOnAction(e -> {
            filtroLocDe.setValue(null);
            filtroLocAte.setValue(null);
            carregarLocatario(null, null);
        });

        Button btnCsv = new Button("📥 Exportar CSV");
        btnCsv.getStyleClass().add("btn-outline-sm");
        btnCsv.setOnAction(e -> exportarCsv(cacheLocatario, "locatario"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox barra = new HBox(8,
                labelFiltro("De:"), filtroLocDe,
                labelFiltro("Até:"), filtroLocAte,
                btnFiltrar, btnLimpar,
                spacer, btnCsv);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(4, 4, 4, 4));

        int idx = card.getChildren().indexOf(reservasLocatarioList);
        if (idx >= 0) card.getChildren().add(idx, barra);
        else card.getChildren().add(0, barra);
    }

    private void aplicarFiltrosLocatario() {
        LocalDate de  = filtroLocDe  != null ? filtroLocDe.getValue()  : null;
        LocalDate ate = filtroLocAte != null ? filtroLocAte.getValue() : null;
        if (ate != null && de != null && ate.isBefore(de)) {
            DialogHelper.erro("A data de fim não pode ser anterior à data de início.");
            return;
        }
        carregarLocatario(de, ate);
    }

    private void carregarLocatario(LocalDate de, LocalDate ate) {
        try {
            cacheLocatario = reservaService.minhasReservasComoLocatarioFiltrado(de, ate);
            reservasLocatarioList.getChildren().clear();
            if (cacheLocatario.isEmpty()) {
                reservasLocatarioList.getChildren().add(vazio("Não foram encontradas reservas com os filtros selecionados."));
            } else {
                for (Reserva r : cacheLocatario) reservasLocatarioList.getChildren().add(cardLocatario(r));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    // ── Proprietário ─────────────────────────────────────────────────────────

    private void injetarBarraProprietario() {
        javafx.scene.Parent parent = reservasProprietarioList.getParent();
        if (!(parent instanceof VBox card)) return;

        filtroPropDe  = datePicker("Data início");
        filtroPropAte = datePicker("Data fim");

        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.getStyleClass().add("btn-primary");
        btnFiltrar.setOnAction(e -> aplicarFiltrosProprietario());

        Button btnLimpar = new Button("✕ Limpar");
        estilizarBtnLimpar(btnLimpar);
        btnLimpar.setOnAction(e -> {
            filtroPropDe.setValue(null);
            filtroPropAte.setValue(null);
            carregarProprietario(null, null);
        });

        Button btnCsv = new Button("📥 Exportar CSV");
        btnCsv.getStyleClass().add("btn-outline-sm");
        btnCsv.setOnAction(e -> exportarCsv(cacheProprietario, "proprietario"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox barra = new HBox(8,
                labelFiltro("De:"), filtroPropDe,
                labelFiltro("Até:"), filtroPropAte,
                btnFiltrar, btnLimpar,
                spacer, btnCsv);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(4, 4, 4, 4));

        int idx = card.getChildren().indexOf(reservasProprietarioList);
        if (idx >= 0) card.getChildren().add(idx, barra);
        else card.getChildren().add(0, barra);
    }

    private void aplicarFiltrosProprietario() {
        LocalDate de  = filtroPropDe  != null ? filtroPropDe.getValue()  : null;
        LocalDate ate = filtroPropAte != null ? filtroPropAte.getValue() : null;
        if (ate != null && de != null && ate.isBefore(de)) {
            DialogHelper.erro("A data de fim não pode ser anterior à data de início.");
            return;
        }
        carregarProprietario(de, ate);
    }

    private void carregarProprietario(LocalDate de, LocalDate ate) {
        try {
            cacheProprietario = reservaService.minhasReservasComoProprietarioFiltrado(de, ate);
            reservasProprietarioList.getChildren().clear();
            if (cacheProprietario.isEmpty()) {
                reservasProprietarioList.getChildren().add(vazio("Não foram encontradas reservas com os filtros selecionados."));
            } else {
                for (Reserva r : cacheProprietario) reservasProprietarioList.getChildren().add(cardProprietario(r));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    // ── Exportação CSV ────────────────────────────────────────────────────────

    /**
     * Exporta a lista de reservas atualmente visível (já filtrada) para um ficheiro CSV.
     * Inclui: ID, Veículo, Data Início, Data Fim, Nº Dias, Estado, Valor Renda (€),
     *         Caução (€), Km Inicial, Km Final, Contraparte (Proprietário ou Locatário).
     *
     * @param lista      reservas a exportar (as que estão no cache — podem estar filtradas)
     * @param perspetiva "locatario" ou "proprietario" — define o campo Contraparte
     */
    private void exportarCsv(List<Reserva> lista, String perspetiva) {
        if (lista == null || lista.isEmpty()) {
            DialogHelper.erro("Não há reservas para exportar. Ajusta os filtros e tenta de novo.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar histórico de alugueres CSV");
        fc.setInitialFileName("historico_alugueres_" + perspetiva + "_" + LocalDate.now() + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File dest = fc.showSaveDialog(App.getStage());
        if (dest == null) return;

        boolean isLocatario = "locatario".equals(perspetiva);

        try (PrintWriter pw = new PrintWriter(new FileWriter(dest, StandardCharsets.UTF_8))) {
            // BOM para o Excel reconhecer UTF-8 automaticamente
            pw.print('\uFEFF');

            // Cabeçalho
            if (isLocatario) {
                pw.println("ID Reserva,Veículo,Data Início,Data Fim,Nº Dias,Estado," +
                           "Valor Renda (€),Caução (€),Km Inicial,Km Final,Proprietário");
            } else {
                pw.println("ID Reserva,Veículo,Data Início,Data Fim,Nº Dias,Estado," +
                           "Valor Renda (€),Caução (€),Km Inicial,Km Final,Locatário");
            }

            for (Reserva r : lista) {
                String contraparte = isLocatario
                        ? csv(r.getProprietarioNome())
                        : csv(r.getLocatarioNome());

                String kmI = r.getKmInicial() != null ? String.valueOf(r.getKmInicial()) : "";
                String kmF = r.getKmFinal()   != null ? String.valueOf(r.getKmFinal())   : "";

                pw.printf("\"%d\",\"%s\",\"%s\",\"%s\",\"%d\",\"%s\",\"%.2f\",\"%.2f\",\"%s\",\"%s\",\"%s\"%n",
                        r.getId(),
                        csv(r.getVeiculoNome()),
                        r.getDataInicio().format(DATA_FMT),
                        r.getDataFim().format(DATA_FMT),
                        r.getNumeroDias(),
                        r.getEstado(),
                        r.getTotal(),
                        r.getCaucao(),
                        kmI,
                        kmF,
                        contraparte);
            }

            DialogHelper.sucesso("CSV exportado com sucesso para:\n" + dest.getAbsolutePath());
        } catch (Exception ex) {
            DialogHelper.erro("Erro ao exportar CSV: " + ex.getMessage());
        }
    }

    /** Escapa aspas duplas dentro de um valor CSV. */
    private String csv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    // ── Cards UI ──────────────────────────────────────────────────────────────

    private VBox cardLocatario(Reserva r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label veiculo = new Label("🚗 " + r.getVeiculoNome());
        veiculo.getStyleClass().add("reserva-veiculo");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label estado = new Label(estadoEmoji(r.getEstado()) + " " + r.getEstado().toUpperCase());
        estado.getStyleClass().add("reserva-estado-" + r.getEstado());
        top.getChildren().addAll(veiculo, sp, estado);

        Label datas = new Label("📅 " + r.getDataInicio() + " → " + r.getDataFim());
        datas.getStyleClass().add("reserva-datas");
        Label total = new Label(String.format("💶 Renda: %.2f€", r.getTotal()));
        total.getStyleClass().add("reserva-total");
        Label prop = new Label("👤 Proprietário: " + (r.getProprietarioNome() != null ? r.getProprietarioNome() : "-"));
        prop.getStyleClass().add("reserva-datas");

        if (r.getCaucao() > 0) {
            String caucaoStatus = "concluida".equals(r.getEstado()) ? "🔓 Caução: %.2f€ (liquidada)" : "🔒 Caução retida: %.2f€";
            Label caucaoLbl = new Label(String.format(caucaoStatus, r.getCaucao()));
            caucaoLbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 0.82em;");
            card.getChildren().addAll(top, datas, total, prop, caucaoLbl);
        } else {
            card.getChildren().addAll(top, datas, total, prop);
        }

        if (("confirmada".equals(r.getEstado()) || "concluida".equals(r.getEstado())) && r.getKmInicial() != null) {
            String txt = "🔢 Km inicial: " + r.getKmInicial();
            if (r.getKmFinal() != null) txt += "   →   Km final: " + r.getKmFinal() + " (atualizado no carro)";
            Label km = new Label(txt);
            km.getStyleClass().add("reserva-datas");
            card.getChildren().add(km);
        }

        HBox btns = new HBox(8);
        if ("pendente".equals(r.getEstado()) || "confirmada".equals(r.getEstado())) {
            Button btnC = new Button("❌ Cancelar");
            btnC.getStyleClass().add("btn-danger");
            btnC.setOnAction(e -> {
                if (confirmar("Cancelar reserva?")) {
                    try { reservaService.cancelarReserva(r.getId()); carregarLocatario(null, null); }
                    catch (Exception ex) { mostrarErro(ex.getMessage()); }
                }
            });
            btns.getChildren().add(btnC);
        }
        if ("confirmada".equals(r.getEstado()) && r.getKmFinal() == null) {
            Button btnF = new Button("🏁 Km Final + Liquidar");
            btnF.getStyleClass().add("btn-success");
            btnF.setOnAction(e -> pedirKmFinal(r));
            btns.getChildren().add(btnF);
        }
        if ("concluida".equals(r.getEstado()) && r.getAvaliacao() == null) {
            Button btnA = new Button("⭐ Avaliar");
            btnA.getStyleClass().add("btn-primary");
            btnA.setOnAction(e -> abrirAvaliacao(r));
            btns.getChildren().add(btnA);
        }

        if ("concluida".equals(r.getEstado()) && r.getCaucao() > 0) {
            try {
                boolean jaTemDisputa = disputaService.existeDisputa(r.getId());
                if (!jaTemDisputa) {
                    Button btnD = new Button("⚖️ Abrir Disputa");
                    btnD.setStyle("-fx-background-color: #7c3aed; -fx-text-fill: white; " +
                            "-fx-background-radius: 8; -fx-border-radius: 8; -fx-font-size: 0.82em;");
                    btnD.setOnAction(e -> abrirDialogoDisputa(r));
                    btns.getChildren().add(btnD);
                } else {
                    Label disputaAberta = new Label("⚖️ Disputa submetida");
                    disputaAberta.setStyle("-fx-text-fill: #7c3aed; -fx-font-size: 0.82em;");
                    btns.getChildren().add(disputaAberta);
                }
            } catch (Exception ignored) {}
        }

        if (!btns.getChildren().isEmpty()) card.getChildren().add(btns);
        return card;
    }

    private VBox cardProprietario(Reserva r) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reserva-card");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label veiculo = new Label("🚗 " + r.getVeiculoNome());
        veiculo.getStyleClass().add("reserva-veiculo");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label estado = new Label(estadoEmoji(r.getEstado()) + " " + r.getEstado().toUpperCase());
        estado.getStyleClass().add("reserva-estado-" + r.getEstado());
        top.getChildren().addAll(veiculo, sp, estado);

        Label loc = new Label("👤 Locatário: " + (r.getLocatarioNome() != null ? r.getLocatarioNome() : "-"));
        loc.getStyleClass().add("reserva-datas");
        Label datas = new Label("📅 " + r.getDataInicio() + " → " + r.getDataFim());
        datas.getStyleClass().add("reserva-datas");
        Label total = new Label(String.format("💶 %.2f€", r.getTotal()));
        total.getStyleClass().add("reserva-total");

        HBox btns = new HBox(8);
        if ("pendente".equals(r.getEstado())) {
            Button btnAp = new Button("✅ Aprovar");
            btnAp.getStyleClass().add("btn-success");
            btnAp.setOnAction(e -> {
                try { reservaService.aprovarReserva(r.getId()); carregarProprietario(null, null); mostrarSucesso("Reserva aprovada!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });
            Button btnRe = new Button("❌ Recusar");
            btnRe.getStyleClass().add("btn-danger");
            btnRe.setOnAction(e -> {
                if (confirmar("Recusar reserva?")) {
                    try { reservaService.cancelarReserva(r.getId()); carregarProprietario(null, null); }
                    catch (Exception ex) { mostrarErro(ex.getMessage()); }
                }
            });
            btns.getChildren().addAll(btnAp, btnRe);
        }

        if ("concluida".equals(r.getEstado()) && r.getAvaliacaoProprietario() == null) {
            Button btnAP = new Button("⭐ Avaliar Locatário");
            btnAP.getStyleClass().add("btn-primary");
            btnAP.setOnAction(e -> abrirAvaliacaoProprietario(r));
            btns.getChildren().add(btnAP);
        } else if ("concluida".equals(r.getEstado()) && r.getAvaliacaoProprietario() != null) {
            Label jaAvaliou = new Label("⭐ Avaliaste: " + "★".repeat(r.getAvaliacaoProprietario()));
            jaAvaliou.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 0.82em;");
            btns.getChildren().add(jaAvaliou);
        }

        card.getChildren().addAll(top, loc, datas, total);
        if (!btns.getChildren().isEmpty()) card.getChildren().add(btns);
        return card;
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────

    private void abrirAvaliacaoProprietario(Reserva r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Avaliar Locatário");
        ComboBox<Integer> estrelas = new ComboBox<>();
        estrelas.getItems().addAll(1, 2, 3, 4, 5);
        estrelas.setValue(5);
        estrelas.getStyleClass().add("filter-combo");
        estrelas.setMaxWidth(Double.MAX_VALUE);
        TextArea comentario = new TextArea();
        comentario.setPromptText("Comentário (opcional)");
        comentario.setPrefRowCount(3);
        VBox c = new VBox(10,
                new Label("Avaliação ao locatário " + r.getLocatarioNome() + ":"),
                new Label("Estrelas:"), estrelas,
                new Label("Comentário:"), comentario);
        c.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(c);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dlg);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    reservaService.avaliarLocatario(r.getId(), estrelas.getValue(), comentario.getText());
                    carregarProprietario(null, null);
                    mostrarSucesso("Avaliação ao locatário submetida!");
                } catch (Exception e) { mostrarErro(e.getMessage()); }
            }
        });
    }

    private void pedirKmFinal(Reserva r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Km Final");

        Label tituloLbl = new Label("Km Final");
        tituloLbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 1.05em;");
        Label lbl = new Label("Quilómetros finais:" + (r.getKmInicial() != null ? "  (km inicial: " + r.getKmInicial() + ")" : ""));
        lbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em; -fx-font-weight: bold;");
        TextField tf = new TextField();
        tf.setPromptText("Obrigatório");
        tf.getStyleClass().add("form-input");
        tf.setStyle(
            "-fx-background-color: #1e1e1e; -fx-border-color: rgba(255,255,255,0.12); " +
            "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; " +
            "-fx-text-fill: white; -fx-prompt-text-fill: #555; -fx-padding: 9 12 9 12;"
        );

        CheckBox cbIncidente = new CheckBox("⚠️ Reportar incidente/dano com o veículo (a caução não será devolvida)");
        cbIncidente.setWrapText(true);
        cbIncidente.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 0.85em;");

        VBox box = new VBox(10, tituloLbl, lbl, tf, cbIncidente);
        box.setPadding(new Insets(4, 0, 4, 0));
        dlg.getDialogPane().setContent(box);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dlg);

        javafx.scene.Node btnOk = dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.setDisable(true);
            tf.textProperty().addListener((obs, oldV, newV) -> {
                boolean valido;
                try {
                    int km = Integer.parseInt(newV.trim());
                    valido = km > 0 && (r.getKmInicial() == null || km > r.getKmInicial());
                } catch (Exception ex) { valido = false; }
                btnOk.setDisable(!valido);
            });
        }

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int km = Integer.parseInt(tf.getText().trim());
                    boolean incidente = cbIncidente.isSelected();
                    reservaService.registarKmFinalELiquidar(r.getId(), km, incidente);
                    carregarLocatario(null, null);
                    if (incidente) mostrarSucesso("Reserva concluída. Incidente reportado — a caução não foi devolvida.");
                    else mostrarSucesso("Reserva concluída e liquidada! Caução devolvida.");
                } catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }
        });
    }

    private void abrirDialogoDisputa(Reserva r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Abrir Disputa");

        Label infoLbl = new Label(String.format(
                "Reserva #%d  |  🚗 %s\nCaução retida: %.2f€\n\nSe acreditas que a caução foi retida indevidamente, " +
                "descreve o sucedido. O administrador irá analisar e decidir.",
                r.getId(), r.getVeiculoNome(), r.getCaucao()));
        infoLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em;");
        infoLbl.setWrapText(true);

        Label descLbl = new Label("Descrição do problema:");
        descLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.85em; -fx-font-weight: bold;");
        javafx.scene.control.TextArea tfDesc = new javafx.scene.control.TextArea();
        tfDesc.setPromptText("Descreve o motivo da disputa...");
        tfDesc.setPrefRowCount(4);
        tfDesc.setWrapText(true);
        tfDesc.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: white; " +
                "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox conteudo = new VBox(10, infoLbl, descLbl, tfDesc);
        conteudo.setPadding(new javafx.geometry.Insets(4, 0, 4, 0));
        dlg.getDialogPane().setContent(conteudo);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dlg);

        javafx.scene.Node btnOk = dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.setDisable(true);
            tfDesc.textProperty().addListener((obs, o, n) -> btnOk.setDisable(n.trim().isBlank()));
        }

        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    disputaService.abrirDisputa(r.getId(), tfDesc.getText().trim());
                    carregarLocatario(null, null);
                    mostrarSucesso("Disputa submetida com sucesso. O administrador irá analisar o caso.");
                } catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }
        });
    }

    private void abrirAvaliacao(Reserva r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Avaliar reserva");
        ComboBox<Integer> estrelas = new ComboBox<>();
        estrelas.getItems().addAll(1,2,3,4,5); estrelas.setValue(5);
        estrelas.getStyleClass().add("filter-combo"); estrelas.setMaxWidth(Double.MAX_VALUE);
        TextArea comentario = new TextArea(); comentario.setPromptText("Comentário (opcional)"); comentario.setPrefRowCount(3);
        VBox c = new VBox(10, new Label("Avaliação (estrelas):"), estrelas, new Label("Comentário:"), comentario);
        c.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(c);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dlg);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try { reservaService.avaliarReserva(r.getId(), estrelas.getValue(), comentario.getText()); carregarLocatario(null, null); mostrarSucesso("Avaliação submetida!"); }
                catch (Exception e) { mostrarErro(e.getMessage()); }
            }
        });
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private DatePicker datePicker(String prompt) {
        DatePicker dp = new DatePicker();
        dp.setPromptText(prompt);
        dp.getStyleClass().add("form-input");
        dp.setPrefWidth(145);
        return dp;
    }

    private Label labelFiltro(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em;");
        return l;
    }

    private void estilizarBtnLimpar(Button btn) {
        btn.setStyle("-fx-background-color: #374151; -fx-text-fill: #d1d5db; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
    }

    private String estadoEmoji(String estado) {
        return switch (estado) {
            case "pendente"   -> "🕐";
            case "confirmada" -> "✅";
            case "cancelada"  -> "❌";
            case "concluida"  -> "🏁";
            default           -> "•";
        };
    }

    private Label vazio(String msg) {
        Label l = new Label(msg);
        l.getStyleClass().add("conta-email");
        l.setPadding(new Insets(16));
        return l;
    }

    private boolean confirmar(String msg) {
        return DialogHelper.confirmar("Confirmação", msg)
                .filter(b -> b == ButtonType.YES)
                .isPresent();
    }

    // ── Navegação ─────────────────────────────────────────────────────────────

    @FXML public void irParaHome()      { App.navigateTo("Home"); }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos()  { App.navigateTo("Vehicles"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void irParaAdmin()     { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout()          { NavbarHelper.logout(); }

    private void mostrarErro(String msg)    { DialogHelper.erro(msg); }
    private void mostrarSucesso(String msg) { DialogHelper.sucesso(msg); }
}
