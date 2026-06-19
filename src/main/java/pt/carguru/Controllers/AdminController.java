package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Disputa;
import pt.carguru.Models.PrecoCombustivel;
import pt.carguru.Models.Reserva;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.CombustivelService;
import pt.carguru.Services.DisputaService;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.UserService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.NavbarHelper;
import pt.carguru.Utils.Session;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminController {
    @FXML private VBox veiculosPendentesList;
    @FXML private VBox todosVeiculosList;
    @FXML private VBox utilizadoresList;
    @FXML private VBox historicoList;
    @FXML private VBox disputasList;
    @FXML private VBox combustivelList;

    private final VeiculoService veiculoService = new VeiculoService();
    private final UserService userService = new UserService();
    private final ReservaService reservaService = new ReservaService();
    private final DisputaService disputaService = new DisputaService();
    private final CombustivelService combustivelService = new CombustivelService();

    @FXML
    public void initialize() {
        if (!Session.isAdmin()) { App.navigateTo("Dashboard"); return; }
        carregarVeiculosPendentes();
        carregarTodosVeiculos();
        carregarUtilizadores();
        carregarHistorico();
        carregarDisputas();
        carregarCombustivel();
    }

    private void carregarVeiculosPendentes() {
        try {
            List<Veiculo> pendentes = veiculoService.listarPendentes();
            veiculosPendentesList.getChildren().clear();
            if (pendentes.isEmpty()) {
                veiculosPendentesList.getChildren().add(labelInfo("✅ Não há veículos pendentes de validação."));
            } else {
                for (Veiculo v : pendentes)
                    veiculosPendentesList.getChildren().add(rowVeiculoPendente(v));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox rowVeiculoPendente(Veiculo v) {
        VBox box = new VBox(10);
        box.getStyleClass().add("admin-row");

        // ── Cabeçalho: nome + badge estado ───────────────────────────────────
        HBox topRow = new HBox(10);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titulo = new Label("🚗 " + v.getNomeCompleto());
        titulo.getStyleClass().add("admin-card-title");
        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
        Label badge = new Label("⏳ PENDENTE");
        badge.getStyleClass().add("admin-badge-warn");
        topRow.getChildren().addAll(titulo, sp, badge);

        // ── Matrícula ────────────────────────────────────────────────────────
        String matriculaStr = (v.getMatricula() != null && !v.getMatricula().isBlank())
                ? v.getMatricula() : "⚠️ Não definida";
        Label matriculaLbl = new Label("🔖 Matrícula: " + matriculaStr);
        matriculaLbl.setStyle(v.getMatricula() != null && !v.getMatricula().isBlank()
                ? "-fx-text-fill: #f1f5f9; -fx-font-weight: bold;"
                : "-fx-text-fill: #f87171; -fx-font-weight: bold;");

        // ── Dados técnicos ────────────────────────────────────────────────────
        Label dadosTecnicos = new Label(String.format(
                "⛽ %s   🔧 %s   👥 %d lugares   🛣️ %d km   💶 %.2f€/dia",
                v.getCombustivel() != null ? v.getCombustivel() : "-",
                v.getTransmissao() != null ? v.getTransmissao() : "-",
                v.getLotacao(),
                v.getQuilometragem(),
                v.getPrecoPorDia()));
        dadosTecnicos.getStyleClass().add("admin-card-meta");
        dadosTecnicos.setWrapText(true);

        // ── Proprietário e localização ────────────────────────────────────────
        String emailStr = v.getProprietarioEmail() != null ? "  ✉️ " + v.getProprietarioEmail() : "";
        Label proprietarioLbl = new Label(String.format("👤 %s%s   📍 %s",
                v.getProprietarioNome(), emailStr, v.getLocalizacao()));
        proprietarioLbl.getStyleClass().add("admin-card-meta");
        proprietarioLbl.setWrapText(true);

        // ── Descrição (se existir) ─────────────────────────────────────────────
        VBox infoBox = new VBox(6, topRow, matriculaLbl, dadosTecnicos, proprietarioLbl);
        if (v.getDescricao() != null && !v.getDescricao().isBlank()) {
            Label descLbl = new Label("📝 " + v.getDescricao());
            descLbl.getStyleClass().add("admin-card-desc");
            descLbl.setWrapText(true);
            infoBox.getChildren().add(descLbl);
        }

        // ── Foto do veículo (se existir localmente) ─────────────────────────
        HBox contentRow = new HBox(16);
        contentRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        File imgFile = new File(pt.carguru.Repositories.VeiculoRepository.resolverPathImagem(v.getId()));
        if (imgFile.exists()) {
            try {
                ImageView imgView = new ImageView(new Image(imgFile.toURI().toString()));
                imgView.setFitWidth(160);
                imgView.setFitHeight(110);
                imgView.setPreserveRatio(true);
                imgView.setStyle("-fx-border-radius: 8; -fx-background-radius: 8;");
                contentRow.getChildren().add(imgView);
            } catch (Exception ignored) {}
        } else {
            Label semFoto = new Label("📷 Sem foto");
            semFoto.setStyle("-fx-text-fill: #f87171; -fx-font-size: 0.82em; -fx-padding: 8;");
            contentRow.getChildren().add(semFoto);
        }
        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);
        contentRow.getChildren().add(infoBox);

        // ── Botões de ação ────────────────────────────────────────────────────
        Button btnAprovar = new Button("✅ Aprovar");
        btnAprovar.getStyleClass().add("btn-admin-ok");
        btnAprovar.setOnAction(e -> {
            Optional<ButtonType> res = confirmar("Aprovar veículo",
                    "Confirmas a aprovação de " + v.getNomeCompleto() + "?\n" +
                    "O veículo ficará disponível na plataforma e o proprietário será notificado.");
            if (res.isPresent() && res.get() == ButtonType.YES) {
                try {
                    veiculoService.aprovarVeiculo(v.getId());
                    carregarVeiculosPendentes();
                    mostrarSucesso("Veículo aprovado! Notificação enviada ao proprietário.");
                } catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }
        });

        Button btnRejeitar = new Button("❌ Rejeitar");
        btnRejeitar.getStyleClass().add("btn-admin-danger");
        btnRejeitar.setOnAction(e -> abrirDialogoRejeicaoVeiculo(v));

        HBox btns = new HBox(8, btnAprovar, btnRejeitar);
        btns.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        box.getChildren().addAll(contentRow, btns);
        return box;
    }

    /**
     * Abre um diálogo para o administrador introduzir a justificação de rejeição.
     * O motivo é obrigatório, guardado na BD e enviado por email ao proprietário.
     */
    private void abrirDialogoRejeicaoVeiculo(Veiculo v) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Rejeitar Anúncio");

        Label infoLbl = new Label(String.format(
                "Veículo: %s\nProprietário: %s",
                v.getNomeCompleto(),
                v.getProprietarioNome() != null ? v.getProprietarioNome() : "-"));
        infoLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em;");
        infoLbl.setWrapText(true);

        Label motivoLbl = new Label("Motivo da rejeição (obrigatório):");
        motivoLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.85em; -fx-font-weight: bold;");

        javafx.scene.control.TextArea tfMotivo = new javafx.scene.control.TextArea();
        tfMotivo.setPromptText("Ex: Matrícula ilegível, dados técnicos incorretos, foto em falta...");
        tfMotivo.setPrefRowCount(4);
        tfMotivo.setWrapText(true);
        tfMotivo.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: white; " +
                "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8; -fx-background-radius: 8;");

        Label avisoLbl = new Label("📧 O proprietário será notificado por email com este motivo.");
        avisoLbl.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 0.8em;");

        VBox conteudo = new VBox(10, infoLbl, motivoLbl, tfMotivo, avisoLbl);
        conteudo.setPadding(new Insets(4, 0, 4, 0));
        dlg.getDialogPane().setContent(conteudo);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dlg);

        // OK só ativo quando há texto
        javafx.scene.Node btnOk = dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.setDisable(true);
            tfMotivo.textProperty().addListener((obs, o, n) -> btnOk.setDisable(n.trim().isBlank()));
        }

        dlg.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String motivo = tfMotivo.getText().trim();
            try {
                veiculoService.rejeitarVeiculoComMotivo(v.getId(), motivo);
                carregarVeiculosPendentes();
                mostrarSucesso("Anúncio rejeitado. Proprietário notificado por email.");
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });
    }

    private void carregarTodosVeiculos() {
        try {
            List<Veiculo> todos = veiculoService.listarTodos();
            todosVeiculosList.getChildren().clear();
            if (todos.isEmpty()) {
                todosVeiculosList.getChildren().add(labelInfo("Sem veículos."));
            } else {
                for (Veiculo v : todos)
                    todosVeiculosList.getChildren().add(rowVeiculoAdmin(v));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox rowVeiculoAdmin(Veiculo v) {
        VBox box = new VBox(8);
        box.getStyleClass().add("admin-row");

        HBox topRow = new HBox(10);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titulo = new Label("🚗 " + v.getNomeCompleto());
        titulo.getStyleClass().add("admin-card-title");
        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
        String estadoStr = v.getEstado() != null ? v.getEstado().toUpperCase() : "-";
        Label estadoBadge = new Label(estadoStr);
        estadoBadge.getStyleClass().add(estadoStr.contains("APROVADO") || estadoStr.contains("DISPONIVEL")
                ? "admin-badge-ok" : estadoStr.contains("PENDENTE") ? "admin-badge-warn" : "admin-badge-red");
        topRow.getChildren().addAll(titulo, sp, estadoBadge);

        Label meta = new Label(String.format("👤 %s   📍 %s   💶 %.2f€/dia",
                v.getProprietarioNome(), v.getLocalizacao(), v.getPrecoPorDia()));
        meta.getStyleClass().add("admin-card-meta");
        meta.setWrapText(true);

        Button btnRemover = new Button("🗑️ Remover");
        btnRemover.getStyleClass().add("btn-admin-danger");
        btnRemover.setOnAction(e -> {
            Optional<ButtonType> res = confirmar("Remover veículo?",
                    "Tens a certeza que queres remover " + v.getNomeCompleto() + "?\nEsta ação é reversível apenas por base de dados.");
            if (res.isPresent() && res.get() == ButtonType.YES) {
                try { veiculoService.removerVeiculoAdmin(v.getId()); carregarTodosVeiculos(); mostrarSucesso("Veículo removido."); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }
        });

        HBox btns = new HBox(8, btnRemover);
        btns.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getChildren().addAll(topRow, meta, btns);
        return box;
    }

    private void carregarUtilizadores() {
        try {
            List<User> users = userService.listarTodos();
            utilizadoresList.getChildren().clear();
            int adminId = Session.getUser().getId();

            for (User u : users) {
                VBox box = new VBox(8);
                box.getStyleClass().add("admin-row");

                // Cabeçalho: nome + badge de estado
                HBox topRow = new HBox(10);
                topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label nomeLabel = new Label("👤 " + u.getNome());
                nomeLabel.getStyleClass().add("admin-card-title");
                javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
                HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
                Label badge = new Label(u.isBloqueado() ? "🔴 Suspenso" : "🟢 Ativo");
                badge.getStyleClass().add(u.isBloqueado() ? "admin-badge-red" : "admin-badge-ok");
                Label roleBadge = new Label("ADMINISTRADOR".equals(u.getRole()) ? "🛡️ Admin" : "Utilizador");
                roleBadge.getStyleClass().add("ADMINISTRADOR".equals(u.getRole()) ? "admin-badge-warn" : "admin-badge-muted");
                topRow.getChildren().addAll(nomeLabel, sp, roleBadge, badge);

                // Metadados
                Label meta = new Label(String.format("✉️ %s   🪪 NIF %s   💶 Saldo: %.2f€",
                        u.getEmail(),
                        u.getNif() != null ? u.getNif() : "-",
                        u.getSaldo()));
                meta.getStyleClass().add("admin-card-meta");
                meta.setWrapText(true);

                HBox btns = new HBox(8);
                btns.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                if (u.getId() != adminId) {
                    Button btnToggle = new Button(u.isBloqueado() ? "✅ Reativar" : "🚫 Suspender");
                    btnToggle.getStyleClass().add(u.isBloqueado() ? "btn-admin-ok" : "btn-admin-danger");
                    btnToggle.setOnAction(e -> {
                        String acao = u.isBloqueado() ? "reativar" : "suspender";
                        Optional<ButtonType> res = confirmar("Confirmar ação",
                                "Tens a certeza que queres " + acao + " o utilizador " + u.getNome() + "?");
                        if (res.isPresent() && res.get() == ButtonType.YES) {
                            try { userService.toggleAtivo(u.getId()); carregarUtilizadores(); }
                            catch (Exception ex) { mostrarErro(ex.getMessage()); }
                        }
                    });
                    btns.getChildren().add(btnToggle);
                } else {
                    Label youLabel = new Label("(Tu próprio — admin não pode suspender-se)");
                    youLabel.getStyleClass().add("conta-email");
                    btns.getChildren().add(youLabel);
                }

                box.getChildren().addAll(topRow, meta, btns);
                utilizadoresList.getChildren().add(box);
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }


    // ── Campos de filtro do histórico ────────────────────────────────────────
    // Guardados como campos para evitar IllegalArgumentException "duplicate children"
    // em JavaFX (um nó não pode ter dois pais ao mesmo tempo).
    private DatePicker filtroHistoricoDataInicio;
    private DatePicker filtroHistoricoDataFim;
    private TextField  filtroHistoricoLocatario;
    private TextField  filtroHistoricoVeiculo;
    private ComboBox<String> filtroHistoricoEstado;
    private HBox filtroLinha1;
    private HBox filtroLinha2;
    private javafx.scene.control.Separator filtroSep;

    // Lista corrente de reservas (para exportar CSV sem nova query)
    private List<Reserva> historicoAtual = new java.util.ArrayList<>();

    private void carregarHistorico() {
        try {
            historicoList.getChildren().clear();

            // ── Construir a barra de filtros UMA única vez ────────────────────
            // (JavaFX não permite um nó ter dois pais — guardamos as HBox como campos)
            if (filtroLinha1 == null) {
                filtroHistoricoDataInicio = new DatePicker();
                filtroHistoricoDataInicio.setPromptText("Data início");
                filtroHistoricoDataInicio.setPrefWidth(148);

                filtroHistoricoDataFim = new DatePicker();
                filtroHistoricoDataFim.setPromptText("Data fim");
                filtroHistoricoDataFim.setPrefWidth(148);

                filtroHistoricoLocatario = new TextField();
                filtroHistoricoLocatario.setPromptText("Nome locatário…");
                filtroHistoricoLocatario.setPrefWidth(160);

                filtroHistoricoVeiculo = new TextField();
                filtroHistoricoVeiculo.setPromptText("Veículo (marca/modelo)…");
                filtroHistoricoVeiculo.setPrefWidth(180);

                filtroHistoricoEstado = new ComboBox<>();
                filtroHistoricoEstado.getItems().addAll("(todos)", "pendente", "confirmada", "cancelada", "concluida");
                filtroHistoricoEstado.setValue("(todos)");
                filtroHistoricoEstado.setPrefWidth(130);

                Button btnFiltrar = new Button("🔍 Filtrar");
                btnFiltrar.getStyleClass().add("btn-admin-primary");
                btnFiltrar.setOnAction(e -> aplicarFiltrosHistorico());

                Button btnLimpar = new Button("✖ Limpar");
                btnLimpar.getStyleClass().add("btn-admin-neutral");
                btnLimpar.setOnAction(e -> {
                    filtroHistoricoDataInicio.setValue(null);
                    filtroHistoricoDataFim.setValue(null);
                    filtroHistoricoLocatario.clear();
                    filtroHistoricoVeiculo.clear();
                    filtroHistoricoEstado.setValue("(todos)");
                    aplicarFiltrosHistorico();
                });

                Button btnExportarCsv = new Button("📥 Exportar CSV");
                btnExportarCsv.getStyleClass().add("btn-admin-ok");
                btnExportarCsv.setOnAction(e -> exportarHistoricoCSV());

                Label lblDe     = new Label("De:");
                Label lblAte    = new Label("Até:");
                Label lblEstado = new Label("Estado:");
                for (Label l : new Label[]{lblDe, lblAte, lblEstado})
                    l.setStyle("-fx-text-fill:#ccc; -fx-font-size:0.85em;");

                filtroLinha1 = new HBox(8, lblDe, filtroHistoricoDataInicio,
                        lblAte, filtroHistoricoDataFim,
                        lblEstado, filtroHistoricoEstado,
                        btnFiltrar, btnLimpar, btnExportarCsv);
                filtroLinha1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                filtroLinha1.setStyle("-fx-padding: 0 0 4 0;");

                Label lblLoc = new Label("Locatário:");
                Label lblVei = new Label("Veículo:");
                lblLoc.setStyle("-fx-text-fill:#ccc; -fx-font-size:0.85em;");
                lblVei.setStyle("-fx-text-fill:#ccc; -fx-font-size:0.85em;");

                filtroLinha2 = new HBox(8, lblLoc, filtroHistoricoLocatario,
                        lblVei, filtroHistoricoVeiculo);
                filtroLinha2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                filtroSep = new javafx.scene.control.Separator();
                filtroSep.setStyle("-fx-background-color: rgba(255,255,255,0.08);");
            }

            // Adicionar a barra de filtros ao VBox (os mesmos nós, sem recriar)
            historicoList.getChildren().addAll(filtroLinha1, filtroLinha2, filtroSep);

            // ── Carregar resultados (sem filtros activos inicialmente) ─────────
            aplicarFiltrosHistorico();

        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    /** Aplica os filtros ativos e repopula o VBox de resultados. */
    private void aplicarFiltrosHistorico() {
        // Remover apenas as linhas de resultado (após o índice 3: linha1, linha2, sep)
        if (historicoList.getChildren().size() > 3)
            historicoList.getChildren().remove(3, historicoList.getChildren().size());

        try {
            java.time.LocalDate de  = filtroHistoricoDataInicio.getValue();
            java.time.LocalDate ate = filtroHistoricoDataFim.getValue();
            String estadoFiltro = filtroHistoricoEstado.getValue();
            String estadoParam  = "(todos)".equals(estadoFiltro) ? null : estadoFiltro;

            // Carregar todas as reservas com filtros de período e estado
            List<Reserva> reservas = reservaService.listarTodasFiltrado(de, ate, null, null, estadoParam);

            // Filtro textual em memória (locatário / veículo) — evita query extra
            String textoLoc = filtroHistoricoLocatario.getText().trim().toLowerCase();
            String textoVei = filtroHistoricoVeiculo.getText().trim().toLowerCase();
            if (!textoLoc.isBlank())
                reservas = reservas.stream()
                        .filter(r -> r.getLocatarioNome() != null &&
                                     r.getLocatarioNome().toLowerCase().contains(textoLoc))
                        .collect(java.util.stream.Collectors.toList());
            if (!textoVei.isBlank())
                reservas = reservas.stream()
                        .filter(r -> r.getVeiculoNome() != null &&
                                     r.getVeiculoNome().toLowerCase().contains(textoVei))
                        .collect(java.util.stream.Collectors.toList());

            historicoAtual = reservas;

            // Contador de resultados
            Label contadorLbl = new Label(String.format("📊 %d reserva(s) encontrada(s)", reservas.size()));
            contadorLbl.setStyle("-fx-text-fill:#888; -fx-font-size:0.82em; -fx-padding: 4 0 4 0;");
            historicoList.getChildren().add(contadorLbl);

            if (reservas.isEmpty()) {
                historicoList.getChildren().add(labelInfo("Nenhuma reserva corresponde aos filtros selecionados."));
                return;
            }

            for (Reserva r : reservas) {
                VBox box = new VBox(6);
                box.getStyleClass().add("admin-row");

                HBox topRow = new HBox(10);
                topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label titulo = new Label(String.format("#%d  🚗 %s", r.getId(), r.getVeiculoNome()));
                titulo.getStyleClass().add("admin-card-title");
                javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
                HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
                String est = r.getEstado().toUpperCase();
                Label estadoBadge = new Label(est);
                String badgeCls = switch (est) {
                    case "CONCLUIDA" -> "admin-badge-blue";
                    case "CONFIRMADA" -> "admin-badge-ok";
                    case "CANCELADA"  -> "admin-badge-red";
                    default           -> "admin-badge-warn";
                };
                estadoBadge.getStyleClass().add(badgeCls);
                topRow.getChildren().addAll(titulo, sp, estadoBadge);

                Label meta = new Label(String.format(
                        "👤 %s   🏠 %s   📅 %s → %s   💶 %.2f€",
                        r.getLocatarioNome(),
                        r.getProprietarioNome(),
                        r.getDataInicio(), r.getDataFim(),
                        r.getTotal()));
                meta.getStyleClass().add("admin-card-meta");
                meta.setWrapText(true);

                box.getChildren().addAll(topRow, meta);
                historicoList.getChildren().add(box);
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    /** Exporta o histórico atualmente visível para um ficheiro CSV no ambiente do utilizador. */
    private void exportarHistoricoCSV() {
        if (historicoAtual == null || historicoAtual.isEmpty()) {
            mostrarErro("Não há reservas para exportar. Aplica primeiro os filtros desejados.");
            return;
        }

        // Diálogo para escolher o destino
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Guardar CSV");
        fc.setInitialFileName("historico_reservas.csv");
        fc.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        java.io.File destino = fc.showSaveDialog(historicoList.getScene().getWindow());
        if (destino == null) return; // utilizador cancelou

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(destino), java.nio.charset.StandardCharsets.UTF_8))) {

            // BOM UTF-8 para compatibilidade com Excel
            pw.print('\uFEFF');

            // Cabeçalho
            pw.println("ID;Veículo;Locatário;Proprietário;Data Início;Data Fim;Estado;Total (€);Caução (€)");

            // Linhas
            for (Reserva r : historicoAtual) {
                pw.printf("%d;%s;%s;%s;%s;%s;%s;%.2f;%.2f%n",
                        r.getId(),
                        escapeCsv(r.getVeiculoNome()),
                        escapeCsv(r.getLocatarioNome()),
                        escapeCsv(r.getProprietarioNome()),
                        r.getDataInicio(),
                        r.getDataFim(),
                        r.getEstado(),
                        r.getTotal(),
                        r.getCaucao());
            }
            mostrarSucesso("CSV exportado com sucesso:\n" + destino.getAbsolutePath());

        } catch (Exception e) {
            mostrarErro("Erro ao exportar CSV: " + e.getMessage());
        }
    }

    /** Escapa um valor para CSV (envolve em aspas se contiver ponto-e-vírgula ou aspas). */
    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(";") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private void carregarDisputas() {
        try {
            List<Disputa> todas = disputaService.listarTodas();
            disputasList.getChildren().clear();

            List<Disputa> abertas    = todas.stream().filter(d -> !d.isResolvida()).collect(Collectors.toList());
            List<Disputa> resolvidas = todas.stream().filter(Disputa::isResolvida).collect(Collectors.toList());

            // ── Secção: Disputas Abertas ──────────────────────────────────────
            Label titAberta = new Label(String.format("🔴 Disputas Abertas / Em Análise  (%d)", abertas.size()));
            titAberta.setStyle("-fx-text-fill: #f87171; -fx-font-size: 1em; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");
            disputasList.getChildren().add(titAberta);

            if (abertas.isEmpty()) {
                disputasList.getChildren().add(labelInfo("✅ Não há disputas abertas."));
            } else {
                for (Disputa d : abertas)
                    disputasList.getChildren().add(rowDisputa(d));
            }

            // ── Separador ─────────────────────────────────────────────────────
            javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
            sep.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-padding: 6 0;");
            disputasList.getChildren().add(sep);

            // ── Secção: Disputas Resolvidas ───────────────────────────────────
            Label titResolvida = new Label(String.format("🟢 Disputas Resolvidas / Encerradas  (%d)", resolvidas.size()));
            titResolvida.setStyle("-fx-text-fill: #86efac; -fx-font-size: 1em; -fx-font-weight: bold; -fx-padding: 6 0 2 0;");
            disputasList.getChildren().add(titResolvida);

            if (resolvidas.isEmpty()) {
                disputasList.getChildren().add(labelInfo("Sem disputas resolvidas ainda."));
            } else {
                for (Disputa d : resolvidas)
                    disputasList.getChildren().add(rowDisputa(d));
            }

        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox rowDisputa(Disputa d) {
        VBox box = new VBox(8);
        box.getStyleClass().add("admin-row");

        // ── Cabeçalho: ID + veículo + badge de estado ─────────────────────────
        HBox topRow = new HBox(10);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label titulo = new Label(String.format("⚖️ Disputa #%d  •  Reserva #%d  •  🚗 %s",
                d.getId(), d.getReservaId(), d.getVeiculoNome()));
        titulo.getStyleClass().add("admin-card-title");
        titulo.setWrapText(true);
        javafx.scene.layout.Region sp = new javafx.scene.layout.Region();
        HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
        Label estadoBadge = new Label(d.getEstadoLabel());
        String badgeCls = switch (d.getEstado() == null ? "" : d.getEstado().toUpperCase()) {
            case "ABERTA"                 -> "admin-badge-red";
            case "EM_ANALISE"             -> "admin-badge-warn";
            case "RESOLVIDA_PROPRIETARIO",
                 "RESOLVIDA_LOCATARIO"    -> "admin-badge-ok";
            default                       -> "admin-badge-muted";
        };
        estadoBadge.getStyleClass().add(badgeCls);
        topRow.getChildren().addAll(titulo, sp, estadoBadge);
        box.getChildren().add(topRow);

        // ── Partes envolvidas + caução ─────────────────────────────────────────
        Label partes = new Label(String.format(
                "👤 Locatário: %s   🏠 Proprietário: %s   💰 Caução: %.2f€",
                d.getLocatarioNome(), d.getProprietarioNome(), d.getCaucao()));
        partes.getStyleClass().add("admin-card-meta");
        partes.setWrapText(true);
        box.getChildren().add(partes);

        // ── Metadados: iniciador + datas + admin atribuído ─────────────────────
        StringBuilder metaStr = new StringBuilder();
        metaStr.append(String.format("📅 Aberta: %s",
                d.getDataCriacao() != null
                        ? d.getDataCriacao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "-"));
        metaStr.append(String.format("   🙋 Iniciada por: %s",
                d.getIniciadorNome() != null ? d.getIniciadorNome() : "-"));
        if (d.getAdminNome() != null)
            metaStr.append(String.format("   🛡️ Admin: %s", d.getAdminNome()));
        if (d.getDataResolucao() != null)
            metaStr.append(String.format("   ✅ Resolvida: %s",
                    d.getDataResolucao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
        Label metaLbl = new Label(metaStr.toString());
        metaLbl.setStyle("-fx-text-fill: #888; -fx-font-size: 0.78em;");
        metaLbl.setWrapText(true);
        box.getChildren().add(metaLbl);

        // ── Descrição do problema ──────────────────────────────────────────────
        Label descricaoLbl = new Label("📋 Descrição: " + d.getDescricao());
        descricaoLbl.getStyleClass().add("admin-card-desc");
        descricaoLbl.setWrapText(true);
        box.getChildren().add(descricaoLbl);

        // ── Resolução + valores financeiros (se resolvida) ─────────────────────
        if (d.getResolucao() != null && !d.getResolucao().isBlank()) {
            Label resLbl = new Label("✏️ Decisão: " + d.getResolucao());
            resLbl.getStyleClass().add("admin-card-resolve");
            resLbl.setWrapText(true);
            box.getChildren().add(resLbl);

            // Mostrar valores financeiros da resolução
            StringBuilder finStr = new StringBuilder();
            if (d.getReembolsoForcado() != null && d.getReembolsoForcado() > 0.01)
                finStr.append(String.format("💶 Devolvido ao locatário: %.2f€", d.getReembolsoForcado()));
            if (d.getPenalizacao() != null && d.getPenalizacao() > 0.01) {
                if (finStr.length() > 0) finStr.append("   ");
                finStr.append(String.format("💸 Penalização aplicada: %.2f€", d.getPenalizacao()));
            }
            if (finStr.length() > 0) {
                Label finLbl = new Label(finStr.toString());
                finLbl.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 0.85em; -fx-font-weight: bold;");
                box.getChildren().add(finLbl);
            }
        }

        // ── Botões de ação (apenas para disputas não resolvidas) ─────────────
        if (!d.isResolvida()) {
            HBox btns = new HBox(8);
            btns.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Button btnAnalise = new Button("🔍 Em Análise");
            btnAnalise.getStyleClass().add("btn-admin-primary");
            btnAnalise.setDisable("EM_ANALISE".equals(d.getEstado()));
            btnAnalise.setOnAction(e -> {
                try {
                    disputaService.iniciarAnalise(d.getId());
                    carregarDisputas();
                    mostrarSucesso("Disputa marcada como Em Análise.");
                } catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });

            Button btnFavProp = new Button("🏠 Favor Proprietário");
            btnFavProp.getStyleClass().add("btn-admin-danger");
            btnFavProp.setOnAction(e -> abrirDialogoResolucao(d, "proprietario"));

            Button btnFavLoc = new Button("👤 Favor Locatário");
            btnFavLoc.getStyleClass().add("btn-admin-ok");
            btnFavLoc.setOnAction(e -> abrirDialogoResolucao(d, "locatario"));

            Button btnEncerrar = new Button("⚫ Encerrar");
            btnEncerrar.getStyleClass().add("btn-admin-neutral");
            btnEncerrar.setOnAction(e -> abrirDialogoResolucao(d, "encerrar"));

            btns.getChildren().addAll(btnAnalise, btnFavProp, btnFavLoc, btnEncerrar);
            box.getChildren().add(btns);
        }

        return box;
    }

    private void abrirDialogoResolucao(Disputa d, String modo) {
        Dialog<ButtonType> dlg = new Dialog<>();
        String tituloModo = switch (modo) {
            case "proprietario" -> "Resolver a favor do Proprietário";
            case "locatario"    -> "Resolver a favor do Locatário";
            default             -> "Encerrar Disputa";
        };
        dlg.setTitle(tituloModo);

        Label infoLbl = new Label(String.format(
                "Disputa #%d  |  Caução total: %.2f€\n%s vs %s",
                d.getId(), d.getCaucao(), d.getLocatarioNome(), d.getProprietarioNome()));
        infoLbl.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em;");
        infoLbl.setWrapText(true);

        Label resolucaoLbl = new Label("Decisão / Justificação:");
        resolucaoLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.85em; -fx-font-weight: bold;");
        javafx.scene.control.TextArea tfResolucao = new javafx.scene.control.TextArea();
        tfResolucao.setPromptText("Descreve a decisão tomada...");
        tfResolucao.setPrefRowCount(3);
        tfResolucao.setWrapText(true);
        tfResolucao.setStyle("-fx-control-inner-background: #1e1e1e; -fx-text-fill: white; " +
                "-fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 8; -fx-background-radius: 8;");

        // Aviso de notificação por email
        Label avisoEmail = new Label("📧 Ambas as partes serão notificadas por email com esta decisão.");
        avisoEmail.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 0.8em;");

        VBox conteudo = new VBox(10, infoLbl, resolucaoLbl, tfResolucao, avisoEmail);

        // Campo de valor apenas quando há transferência financeira
        javafx.scene.control.TextField tfValor = null;
        if (!modo.equals("encerrar")) {
            String labelValor = modo.equals("proprietario")
                    ? String.format("Penalização ao locatário (máx. %.2f€):", d.getCaucao())
                    : String.format("Valor a devolver ao locatário (máx. %.2f€):", d.getCaucao());
            Label valorLbl = new Label(labelValor);
            valorLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.85em; -fx-font-weight: bold;");
            tfValor = new javafx.scene.control.TextField(String.format("%.2f", d.getCaucao()));
            tfValor.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: rgba(255,255,255,0.12); " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-padding: 8 12;");
            conteudo.getChildren().addAll(valorLbl, tfValor);
        }

        conteudo.setPadding(new javafx.geometry.Insets(4, 0, 4, 0));
        dlg.getDialogPane().setContent(conteudo);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dlg);

        // OK só ativo quando há texto de decisão
        javafx.scene.Node btnOk = dlg.getDialogPane().lookupButton(ButtonType.OK);
        if (btnOk != null) {
            btnOk.setDisable(true);
            tfResolucao.textProperty().addListener((obs, o, n) -> btnOk.setDisable(n.trim().isBlank()));
        }

        final javafx.scene.control.TextField tfValorFinal = tfValor;
        dlg.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            String resolucao = tfResolucao.getText().trim();
            if (resolucao.isBlank()) { mostrarErro("A justificação não pode estar vazia."); return; }
            try {
                switch (modo) {
                    case "proprietario" -> {
                        double pen = parseValor(tfValorFinal, d.getCaucao());
                        disputaService.resolverFavorProprietario(d.getId(), resolucao, pen);
                        mostrarSucesso(String.format(
                                "Disputa resolvida a favor do proprietário. Penalização: %.2f€\nAmbas as partes foram notificadas por email.", pen));
                    }
                    case "locatario" -> {
                        double ree = parseValor(tfValorFinal, d.getCaucao());
                        disputaService.resolverFavorLocatario(d.getId(), resolucao, ree);
                        mostrarSucesso(String.format(
                                "Disputa resolvida a favor do locatário. Devolvido: %.2f€\nAmbas as partes foram notificadas por email.", ree));
                    }
                    default -> {
                        disputaService.encerrar(d.getId(), resolucao);
                        mostrarSucesso("Disputa encerrada sem penalização.\nAmbas as partes foram notificadas por email.");
                    }
                }
                carregarDisputas();
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });
    }

    /** Lê e valida um valor monetário de um TextField, com fallback para o máximo. */
    private double parseValor(javafx.scene.control.TextField tf, double max) {
        if (tf == null) return 0.0;
        try {
            double v = Double.parseDouble(tf.getText().trim().replace(",", "."));
            return Math.max(0.0, Math.min(v, max));
        } catch (NumberFormatException e) {
            return max;
        }
    }

    private void carregarCombustivel() {
        try {
            List<PrecoCombustivel> precos = combustivelService.listarPrecos();
            combustivelList.getChildren().clear();

            if (precos.isEmpty()) {
                combustivelList.getChildren().add(labelInfo(
                    "⚠️ Nenhum preço configurado ainda. Define os preços base abaixo."));
            }

            // Cards de preço corrente por tipo
            HBox cardsRow = new HBox(12);
            for (PrecoCombustivel p : precos) {
                cardsRow.getChildren().add(cardPreco(p));
            }
            combustivelList.getChildren().add(cardsRow);

            // Secção de edição
            Label editLabel = new Label("✏️ Definir preço base");
            editLabel.setStyle("-fx-text-fill: #ccc; -fx-font-weight: bold; -fx-font-size: 1em;");
            combustivelList.getChildren().add(editLabel);

            // Uma linha de edição por tipo
            for (String tipo : CombustivelService.TIPOS) {
                combustivelList.getChildren().add(rowEditarPreco(tipo, precos));
            }

        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox cardPreco(PrecoCombustivel p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("admin-row");
        card.setPadding(new javafx.geometry.Insets(14));
        card.setPrefWidth(200);

        Label tipoLbl = new Label(p.getTipoLabel());
        tipoLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-font-size: 1em;");

        Label corrLbl = new Label(String.format("%.4f %s", p.getPrecoCorrente(), p.getUnidade()));
        corrLbl.setStyle("-fx-font-size: 1.35em; -fx-font-weight: bold; -fx-text-fill: #4ade80;");

        Label baseLbl = new Label(String.format("Base: %.4f %s", p.getPrecoBase(), p.getUnidade()));
        baseLbl.setStyle("-fx-font-size: 0.8em; -fx-text-fill: #aaa;");

        String atualizado = p.getUltimaAtualizacao() != null
            ? "⏱ " + p.getUltimaAtualizacao().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
            : "";
        Label tsLbl = new Label(atualizado);
        tsLbl.setStyle("-fx-font-size: 0.75em; -fx-text-fill: #666;");

        card.getChildren().addAll(tipoLbl, corrLbl, baseLbl, tsLbl);
        return card;
    }

    private HBox rowEditarPreco(String tipo, List<PrecoCombustivel> precos) {
        // Encontrar preço atual se existir
        double precoAtual = precos.stream()
            .filter(p -> tipo.equals(p.getTipoCombustivel()))
            .mapToDouble(PrecoCombustivel::getPrecoBase)
            .findFirst().orElse(0.0);

        String unidade = "ELETRICO".equals(tipo) ? "€/kWh" : "€/L";
        String emoji   = switch (tipo) {
            case "GASOLINA" -> "⛽";
            case "GASOLEO"  -> "🛢️";
            case "GPL"      -> "🔵";
            default         -> "⚡";
        };

        Label lbl = new Label(String.format("%s %-10s (%s):", emoji, tipo, unidade));
        lbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.88em;");
        lbl.setPrefWidth(220);

        TextField tf = new TextField(precoAtual > 0 ? String.format("%.4f", precoAtual) : "");
        tf.setPromptText("ex: 1.7350");
        tf.setPrefWidth(120);
        tf.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: rgba(255,255,255,0.12); " +
                "-fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: white; -fx-padding: 6 10;");

        Button btn = new Button("💾 Guardar");
        btn.getStyleClass().add("btn-admin-primary");
        btn.setOnAction(e -> {
            try {
                double valor = Double.parseDouble(tf.getText().trim().replace(",", "."));
                combustivelService.definirPrecoBase(tipo, valor);
                carregarCombustivel();
                mostrarSucesso(String.format("Preço base de %s atualizado para %.4f %s", tipo, valor, unidade));
            } catch (NumberFormatException ex) {
                mostrarErro("Valor inválido. Usa o formato: 1.7350");
            } catch (Exception ex) {
                mostrarErro(ex.getMessage());
            }
        });

        HBox row = new HBox(12, lbl, tf, btn);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return row;
    }

    private Label labelInfo(String txt) {
        Label l = new Label(txt);
        l.getStyleClass().add("conta-email");
        l.setPadding(new Insets(10));
        return l;
    }

    private Optional<ButtonType> confirmar(String titulo, String msg) {
        return DialogHelper.confirmar(titulo, msg);
    }

    @FXML public void irParaHome()     { App.navigateTo("Home"); }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos()  { App.navigateTo("Vehicles"); }
    @FXML public void irParaReservas()   { App.navigateTo("Reservas"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void logout() { NavbarHelper.logout(); }

    private void mostrarErro(String msg)   { DialogHelper.erro(msg); }
    private void mostrarSucesso(String msg){ DialogHelper.sucesso(msg); }
}
