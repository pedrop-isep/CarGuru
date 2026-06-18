package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import pt.carguru.App;
import pt.carguru.Models.Indisponibilidade;
import pt.carguru.Models.Transacao;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.UserService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.NavbarHelper;
import pt.carguru.Utils.Session;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ContaController {
    @FXML private StackPane avatarWrap;
    @FXML private Label avatarLabel;
    @FXML private ImageView avatarImage;
    @FXML private Label nomeLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private TextField perfilNome;
    @FXML private TextField perfilEmail;
    @FXML private TextField perfilNif;
    @FXML private Label perfilErro;
    @FXML private TextField perfilNCarta;
    @FXML private DatePicker perfilValidadeCarta;
    @FXML private Label cartaErro;
    @FXML private Label saldoLabel;
    @FXML private Label caucaoLabel;
    @FXML private Label disponivelLabel;
    @FXML private VBox veiculosList;
    @FXML private VBox transacoesList;
    @FXML private Button btnAdmin;

    // Filtros do histórico de transações (criados programaticamente)
    private ComboBox<String> filtroTipo;
    private DatePicker filtroDataInicio;
    private DatePicker filtroDataFim;
    private List<Transacao> transacoesCache = new java.util.ArrayList<>();

    private final UserService userService = new UserService();
    private final VeiculoService veiculoService = new VeiculoService();
    private static final DateTimeFormatter DATA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        User user = Session.getUser();
        if (user == null) { App.navigateTo("Login"); return; }
        NavbarHelper.configurar(btnAdmin);
        carregarPerfil(user);
        carregarVeiculos();
        injetarBarraFiltros();
        carregarHistorico();
        tentarCarregarFoto(user);
    }

    private void carregarPerfil(User user) {
        avatarLabel.setText(user.getInitials());
        nomeLabel.setText(user.getNome());
        emailLabel.setText(user.getEmail());
        roleLabel.setText("ADMINISTRADOR".equals(user.getRole()) ? "🛡️ Administrador" : "🚗 Proprietário & Locatário");
        perfilNome.setText(user.getNome());
        perfilEmail.setText(user.getEmail());
        perfilNif.setText(user.getNif() != null ? user.getNif() : "");
        perfilNCarta.setText(user.getNCartaConducao() != null && !user.getNCartaConducao().equals("N/A") ? user.getNCartaConducao() : "");
        perfilValidadeCarta.setValue(user.getValidadeCarta());
        atualizarSaldoUI(user);
    }

    /** Atualiza as labels de saldo total, caução ativa e saldo disponível para reservar/levantar. */
    private void atualizarSaldoUI(User user) {
        saldoLabel.setText(String.format("%.2f€", user.getSaldo()));
        try {
            double caucoesAtivas = userService.getCaucoesAtivas(user);
            double disponivel = user.getSaldo() - caucoesAtivas;
            caucaoLabel.setText(String.format("🔒 Em caução (reservas ativas): %.2f€", caucoesAtivas));
            disponivelLabel.setText(String.format("✅ Saldo disponível: %.2f€", disponivel));
        } catch (Exception e) {
            caucaoLabel.setText("🔒 Em caução: —");
            disponivelLabel.setText("✅ Saldo disponível: —");
        }
    }

    private void carregarHistorico() {
        carregarHistoricoFiltrado(null, null, null);
    }

    private void carregarHistoricoFiltrado(String tipo, LocalDate dataInicio, LocalDate dataFim) {
        try {
            String tipoFiltro = (tipo != null && !tipo.equals("Todos")) ? tipo : null;
            transacoesCache = userService.listarHistoricoFiltrado(tipoFiltro, dataInicio, dataFim);
            transacoesList.getChildren().clear();
            if (transacoesCache.isEmpty()) {
                Label vazio = new Label("Não foram encontradas transações com os filtros selecionados.");
                vazio.getStyleClass().add("conta-email");
                vazio.setPadding(new Insets(8));
                transacoesList.getChildren().add(vazio);
            } else {
                for (Transacao t : transacoesCache) transacoesList.getChildren().add(criarRowTransacao(t));
            }
        } catch (Exception e) { DialogHelper.erro(e.getMessage()); }
    }

    private HBox criarRowTransacao(Transacao t) {
        HBox row = new HBox(12);
        row.getStyleClass().add("veiculo-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        Label tipoLbl = new Label(t.getTipoEmoji() + " " + t.getTipoLabel());
        tipoLbl.getStyleClass().add("conta-name");

        String dataStr = t.getData() != null ? t.getData().format(DATA_FMT) : "-";
        String contraparteStr = (t.getContraparte() != null && !t.getContraparte().isBlank())
                ? "  •  👤 " + t.getContraparte() : "";
        Label descLbl = new Label(t.getDescricao() + "  •  📅 " + dataStr + contraparteStr);
        descLbl.getStyleClass().add("conta-email");
        descLbl.setWrapText(true);

        Label saldoAposLbl = new Label("Saldo após: " + String.format("%.2f€", t.getSaldoApos()));
        saldoAposLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 0.78em;");

        info.getChildren().addAll(tipoLbl, descLbl, saldoAposLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label valorLbl = new Label((t.isEntrada() ? "+ " : "− ") + String.format("%.2f€", t.getValor()));
        valorLbl.setStyle(t.isEntrada()
            ? "-fx-text-fill: #4ade80; -fx-font-weight: bold; -fx-font-size: 1.05em;"
            : "-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 1.05em;");

        row.getChildren().addAll(info, valorLbl);
        return row;
    }

    /** Constrói e injeta a barra de filtros acima do transacoesList. Chamado uma única vez no initialize. */
    private void injetarBarraFiltros() {
        // Encontrar o VBox pai (conta-card que contém o histórico)
        javafx.scene.Parent parent = transacoesList.getParent();
        if (!(parent instanceof VBox cardVBox)) return;

        // ComboBox tipo
        filtroTipo = new ComboBox<>();
        filtroTipo.getItems().addAll("Todos", "DEPOSITO", "LEVANTAMENTO",
                "PAGAMENTO_ALUGUER", "RECEITA_ALUGUER",
                "CAUCAO_RETIDA", "CAUCAO_DEVOLVIDA",
                "REEMBOLSO", "PENALIZACAO");
        filtroTipo.setValue("Todos");
        filtroTipo.getStyleClass().add("filter-combo");
        filtroTipo.setPrefWidth(200);

        // DatePickers
        filtroDataInicio = new DatePicker();
        filtroDataInicio.setPromptText("Data início");
        filtroDataInicio.getStyleClass().add("form-input");
        filtroDataInicio.setPrefWidth(145);

        filtroDataFim = new DatePicker();
        filtroDataFim.setPromptText("Data fim");
        filtroDataFim.getStyleClass().add("form-input");
        filtroDataFim.setPrefWidth(145);

        // Botão aplicar
        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.getStyleClass().add("btn-primary");
        btnFiltrar.setOnAction(e -> aplicarFiltros());

        // Botão limpar
        Button btnLimpar = new Button("✕ Limpar");
        btnLimpar.setStyle("-fx-background-color: #374151; -fx-text-fill: #d1d5db; " +
                "-fx-background-radius: 8; -fx-border-radius: 8;");
        btnLimpar.setOnAction(e -> {
            filtroTipo.setValue("Todos");
            filtroDataInicio.setValue(null);
            filtroDataFim.setValue(null);
            carregarHistorico();
        });

        // Botão exportar CSV
        Button btnCsv = new Button("📥 Exportar CSV");
        btnCsv.getStyleClass().add("btn-outline-sm");
        btnCsv.setOnAction(e -> exportarCsv());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox barraFiltros = new HBox(8,
                new Label("Tipo:"), filtroTipo,
                new Label("De:"), filtroDataInicio,
                new Label("Até:"), filtroDataFim,
                btnFiltrar, btnLimpar,
                spacer, btnCsv);
        barraFiltros.setAlignment(Pos.CENTER_LEFT);
        barraFiltros.setPadding(new Insets(4, 0, 4, 0));
        // Estilo subtil nas labels de filtro
        for (javafx.scene.Node n : barraFiltros.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em;");
        }

        // Inserir a barra logo antes do transacoesList (que é o último filho do card)
        int idx = cardVBox.getChildren().indexOf(transacoesList);
        if (idx >= 0) cardVBox.getChildren().add(idx, barraFiltros);
        else cardVBox.getChildren().add(1, barraFiltros);
    }

    private void aplicarFiltros() {
        String tipo = filtroTipo != null ? filtroTipo.getValue() : null;
        LocalDate di = filtroDataInicio != null ? filtroDataInicio.getValue() : null;
        LocalDate df = filtroDataFim   != null ? filtroDataFim.getValue()   : null;
        if (df != null && di != null && df.isBefore(di)) {
            DialogHelper.erro("A data de fim não pode ser anterior à data de início.");
            return;
        }
        carregarHistoricoFiltrado(tipo, di, df);
    }

    private void exportarCsv() {
        if (transacoesCache == null || transacoesCache.isEmpty()) {
            DialogHelper.erro("Não há transações para exportar.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar histórico CSV");
        fc.setInitialFileName("historico_transacoes_" + LocalDate.now() + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File dest = fc.showSaveDialog(App.getStage());
        if (dest == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(dest, StandardCharsets.UTF_8))) {
            // BOM para Excel reconhecer UTF-8
            pw.print('\uFEFF');
            pw.println("ID,Data,Tipo,Descrição,Contraparte,Valor (€),Saldo Após (€),Referência");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Transacao t : transacoesCache) {
                String sinal = t.isEntrada() ? "+" : "-";
                pw.printf("\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s%.2f\",\"%.2f\",\"%s\"%n",
                        t.getId(),
                        t.getData() != null ? t.getData().format(fmt) : "",
                        t.getTipoLabel(),
                        escapeCsv(t.getDescricao()),
                        escapeCsv(t.getContraparte()),
                        sinal, t.getValor(),
                        t.getSaldoApos(),
                        t.getReferenciaId() != null ? t.getReferenciaTipo() + " #" + t.getReferenciaId() : "");
            }
            DialogHelper.sucesso("CSV exportado com sucesso para:\n" + dest.getAbsolutePath());
        } catch (Exception ex) {
            DialogHelper.erro("Erro ao exportar CSV: " + ex.getMessage());
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }

    private void tentarCarregarFoto(User user) {
        try {
            File fotoFile = new File(System.getProperty("user.home") + "/.carguru/pfp_" + user.getId() + ".png");
            if (fotoFile.exists()) {
                Image img = new Image(fotoFile.toURI().toString());
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                avatarImage.setManaged(true);
                avatarLabel.setVisible(false);
                avatarLabel.setManaged(false);
                Circle clip = new Circle(36, 36, 36);
                avatarImage.setClip(clip);
                avatarImage.setFitWidth(72);
                avatarImage.setFitHeight(72);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void handleAlterarFoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar foto de perfil");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fc.showOpenDialog(App.getStage());
        if (file != null) {
            try {
                File destDir = new File(System.getProperty("user.home") + "/.carguru");
                destDir.mkdirs();
                File dest = new File(destDir, "pfp_" + Session.getUser().getId() + ".png");
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tentarCarregarFoto(Session.getUser());
                DialogHelper.sucesso("Foto de perfil atualizada!");
            } catch (Exception e) {
                DialogHelper.erro("Erro ao guardar foto: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleGuardarPerfil() {
        perfilErro.setText("");
        DialogHelper.confirmar("Confirmar alterações",
                "Tens a certeza que queres guardar as alterações ao teu perfil?")
            .filter(b -> b == ButtonType.YES)
            .ifPresent(b -> {
                try {
                    userService.atualizarPerfil(perfilNome.getText(), perfilNif.getText());
                    carregarPerfil(Session.getUser());
                    DialogHelper.sucesso("Perfil atualizado com sucesso!");
                } catch (Exception e) { perfilErro.setText(e.getMessage()); }
            });
    }

    @FXML
    public void handleGuardarCarta() {
        cartaErro.setText("");
        DialogHelper.confirmar("Confirmar carta de condução",
                "Confirmas a atualização dos dados da carta de condução?")
            .filter(b -> b == ButtonType.YES)
            .ifPresent(b -> {
                try {
                    userService.atualizarCarta(perfilNCarta.getText(), perfilValidadeCarta.getValue());
                    carregarPerfil(Session.getUser());
                    DialogHelper.sucesso("Carta de condução atualizada com sucesso!");
                } catch (Exception e) { cartaErro.setText(e.getMessage()); }
            });
    }

    @FXML
    public void handleDepositar() {
        DialogHelper.pedirTexto("Depositar Saldo", "Quanto queres depositar?", "Valor (€):")
            .ifPresent(val -> {
                try {
                    double valor = Double.parseDouble(val.replace(",", "."));
                    if (valor <= 0) { DialogHelper.erro("O valor tem de ser positivo."); return; }
                    DialogHelper.confirmar("Confirmar Depósito",
                        String.format("Confirmas o depósito de %.2f€ na tua conta?", valor))
                        .filter(b -> b == ButtonType.YES)
                        .ifPresent(b -> {
                            try {
                                userService.depositar(valor);
                                atualizarSaldoUI(Session.getUser());
                                carregarHistorico();
                                DialogHelper.sucesso(String.format("%.2f€ depositados com sucesso!", valor));
                            } catch (Exception e) { DialogHelper.erro(e.getMessage()); }
                        });
                } catch (NumberFormatException ex) { DialogHelper.erro("Valor inválido."); }
            });
    }

    @FXML
    public void handleLevantar() {
        DialogHelper.pedirTexto("Levantar Saldo", "Quanto queres levantar?", "Valor (€):")
            .ifPresent(val -> {
                try {
                    double valor = Double.parseDouble(val.replace(",", "."));
                    if (valor <= 0) { DialogHelper.erro("O valor tem de ser positivo."); return; }
                    DialogHelper.confirmar("Confirmar Levantamento",
                        String.format("Confirmas o levantamento de %.2f€?", valor))
                        .filter(b -> b == ButtonType.YES)
                        .ifPresent(b -> {
                            try {
                                userService.levantar(valor);
                                atualizarSaldoUI(Session.getUser());
                                carregarHistorico();
                                DialogHelper.sucesso(String.format("%.2f€ levantados com sucesso!", valor));
                            } catch (Exception e) { DialogHelper.erro(e.getMessage()); }
                        });
                } catch (NumberFormatException ex) { DialogHelper.erro("Valor inválido."); }
            });
    }

    private void carregarVeiculos() {
        try {
            List<Veiculo> veiculos = veiculoService.listarMeusVeiculos();
            veiculosList.getChildren().clear();
            if (veiculos.isEmpty()) {
                Label l = new Label("Ainda não tens veículos. Clica em + Adicionar.");
                l.getStyleClass().add("conta-email");
                l.setPadding(new Insets(10));
                veiculosList.getChildren().add(l);
            } else {
                for (Veiculo v : veiculos)
                    veiculosList.getChildren().add(criarRowVeiculo(v));
            }
        } catch (Exception e) { DialogHelper.erro(e.getMessage()); }
    }

    private VBox criarRowVeiculo(Veiculo v) {
        VBox row = new VBox(6);
        row.getStyleClass().add("veiculo-row");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(3);
        Label nome = new Label("🚗 " + v.getNomeCompleto());
        nome.getStyleClass().add("veiculo-name");
        Label meta = new Label("📍 " + v.getLocalizacao() + "  •  " +
            String.format("%.2f€/dia", v.getPrecoPorDia()) + "  •  " + v.getAvaliacaoStr());
        meta.getStyleClass().add("veiculo-meta");
        info.getChildren().addAll(nome, meta);
        HBox.setHgrow(info, Priority.ALWAYS);

        String estadoClass = switch (v.getEstado() != null ? v.getEstado().toUpperCase() : "") {
            case "DISPONIVEL" -> "estado-disponivel";
            case "PENDENTE_VALIDACAO" -> "estado-pendente";
            default -> "estado-removido";
        };
        Label estadoBadge = new Label(v.getEstado() != null ? v.getEstado().replace("_", " ") : "?");
        estadoBadge.getStyleClass().addAll("estado-badge", estadoClass);

        top.getChildren().addAll(info, estadoBadge);

        HBox btns = new HBox(8);
        Button btnEditar = new Button("✏️ Editar");
        btnEditar.getStyleClass().add("btn-outline-sm");
        btnEditar.setOnAction(e -> abrirModalVeiculo(v));

        Button btnRemover = new Button("🗑️ Remover");
        btnRemover.getStyleClass().add("btn-danger");
        btnRemover.setOnAction(e ->
            DialogHelper.confirmar("Remover veículo", "Tens a certeza que queres remover " + v.getNomeCompleto() + "?")
                .filter(b -> b == ButtonType.YES)
                .ifPresent(b -> {
                    try { veiculoService.removerVeiculo(v.getId()); carregarVeiculos(); }
                    catch (Exception ex) { DialogHelper.erro(ex.getMessage()); }
                }));

        Button btnIndisp = new Button("📅 Indisponibilidade");
        btnIndisp.getStyleClass().add("btn-secondary");
        btnIndisp.setOnAction(e -> abrirModalIndisponibilidade(v));

        btns.getChildren().addAll(btnEditar, btnRemover, btnIndisp);
        row.getChildren().addAll(top, btns);
        return row;
    }

    @FXML
    public void handleAdicionarVeiculo() { abrirModalVeiculo(null); }

    private void abrirModalVeiculo(Veiculo vExistente) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(vExistente == null ? "Adicionar Veículo" : "Editar Veículo");

        TextField marca     = campo(vExistente != null ? vExistente.getMarca() : "", "Ex: Toyota");
        TextField modelo    = campo(vExistente != null ? vExistente.getModelo() : "", "Ex: Corolla");
        TextField ano       = campo(vExistente != null ? String.valueOf(vExistente.getAno()) : "", "Ex: 2020");
        TextField matricula = campo(vExistente != null && vExistente.getMatricula() != null ? vExistente.getMatricula() : "", "Ex: AB-12-CD");

        ComboBox<String> comb = new ComboBox<>();
        comb.getItems().addAll("GASOLINA", "GASOLEO", "ELETRICO", "GPL", "HIBRIDO");
        comb.setValue(vExistente != null ? vExistente.getCombustivel() : "GASOLINA");
        comb.getStyleClass().add("filter-combo"); comb.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> trans = new ComboBox<>();
        trans.getItems().addAll("MANUAL", "AUTOMATICA");
        trans.setValue(vExistente != null ? vExistente.getTransmissao() : "MANUAL");
        trans.getStyleClass().add("filter-combo"); trans.setMaxWidth(Double.MAX_VALUE);

        TextField loc     = campo(vExistente != null ? vExistente.getLocalizacao() : "", "Ex: Porto");
        TextField preco   = campo(vExistente != null ? String.valueOf(vExistente.getPrecoPorDia()) : "", "Ex: 45.00");
        TextField consumo = campo(vExistente != null ? String.valueOf(vExistente.getConsumo()) : "6.0", "L/100km");
        TextField km      = campo(vExistente != null ? String.valueOf(vExistente.getQuilometragem()) : "0", "Ex: 50000");
        TextField lotacao = campo(vExistente != null ? String.valueOf(vExistente.getLotacao()) : "5", "Ex: 5");
        TextArea desc     = new TextArea(vExistente != null ? vExistente.getDescricao() : "");
        desc.setPrefRowCount(3); desc.setPromptText("Descrição opcional...");
        desc.setStyle("-fx-control-inner-background:#1e1e1e; -fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.09); -fx-border-radius:8; -fx-background-radius:8;");

        // Imagem do veículo
        final File[] imagemSelecionada = { null };
        Label lblImagemAtual = new Label();
        lblImagemAtual.setStyle("-fx-text-fill: #aaa; -fx-font-size:0.82em;");
        if (vExistente != null) {
            java.io.File imgExist = new java.io.File(pt.carguru.Repositories.VeiculoRepository.resolverPathImagem(vExistente.getId()));
            lblImagemAtual.setText(imgExist.exists() ? "✅ Imagem já definida" : "Sem imagem");
        }
        Button btnEscolherImagem = new Button("📷 Escolher imagem...");
        btnEscolherImagem.getStyleClass().add("btn-outline-sm");
        btnEscolherImagem.setMaxWidth(Double.MAX_VALUE);
        btnEscolherImagem.setOnAction(ev -> {
            FileChooser fc2 = new FileChooser();
            fc2.setTitle("Selecionar imagem do veículo");
            fc2.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.jpg","*.jpeg","*.png","*.gif","*.webp"));
            File picked = fc2.showOpenDialog(App.getStage());
            if (picked != null) {
                imagemSelecionada[0] = picked;
                lblImagemAtual.setText("📷 " + picked.getName());
            }
        });

        VBox content = new VBox(8,
            lbl("Marca:"), marca, lbl("Modelo:"), modelo, lbl("Ano:"), ano,
            lbl("Matrícula:"), matricula, lbl("Combustível:"), comb,
            lbl("Transmissão:"), trans, lbl("Localização:"), loc,
            lbl("Preço/dia (€):"), preco, lbl("Consumo (L/100km):"), consumo,
            lbl("Quilómetros:"), km, lbl("Lotação:"), lotacao, lbl("Descrição:"), desc,
            lbl("Imagem:"), btnEscolherImagem, lblImagemAtual);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #141414;");

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true); sp.setPrefHeight(500);
        sp.setStyle("-fx-background-color: #141414; -fx-background: #141414;");

        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().setStyle("-fx-background-color: #141414;");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        DialogHelper.estilizar(dialog);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int anoVal    = Integer.parseInt(ano.getText().trim());
                    double precoV = Double.parseDouble(preco.getText().replace(",", ".").trim());
                    double consV  = Double.parseDouble(consumo.getText().replace(",", ".").trim());
                    int kmVal     = Integer.parseInt(km.getText().trim());
                    int lotV      = Integer.parseInt(lotacao.getText().trim());
                    pt.carguru.Models.Veiculo veiculoSalvo;
                    if (vExistente == null) {
                        veiculoSalvo = veiculoService.adicionarVeiculo(marca.getText(), modelo.getText(), anoVal,
                            comb.getValue(), trans.getValue(), loc.getText(), precoV, consV,
                            desc.getText(), matricula.getText(), kmVal, lotV);
                        DialogHelper.sucesso("Veículo submetido para aprovação!");
                    } else {
                        veiculoService.editarVeiculo(vExistente.getId(), marca.getText(), modelo.getText(), anoVal,
                            comb.getValue(), trans.getValue(), loc.getText(), precoV, consV,
                            desc.getText(), matricula.getText(), kmVal, lotV);
                        veiculoSalvo = vExistente;
                        DialogHelper.sucesso("Veículo atualizado!");
                    }
                    // Guardar imagem se foi selecionada
                    if (imagemSelecionada[0] != null) {
                        try {
                            java.io.File destDir = new java.io.File(System.getProperty("user.home") + "/.carguru");
                            destDir.mkdirs();
                            java.io.File dest = new java.io.File(pt.carguru.Repositories.VeiculoRepository.resolverPathImagem(veiculoSalvo.getId()));
                            java.nio.file.Files.copy(imagemSelecionada[0].toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        } catch (Exception imgEx) {
                            DialogHelper.erro("Veículo guardado mas falhou ao copiar imagem: " + imgEx.getMessage());
                        }
                    }
                    carregarVeiculos();
                } catch (NumberFormatException ex) {
                    DialogHelper.erro("Verifica os campos numéricos (Ano, Preço, Consumo, Km, Lotação).");
                } catch (Exception e) { DialogHelper.erro(e.getMessage()); }
            }
        });
    }

    private void abrirModalIndisponibilidade(Veiculo v) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Indisponibilidade — " + v.getNomeCompleto());

        DatePicker dpI = new DatePicker(LocalDate.now());
        DatePicker dpF = new DatePicker(LocalDate.now().plusDays(7));
        VBox lista = new VBox(6);

        Button btnAdd = new Button("+ Adicionar período");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> {
            try {
                veiculoService.adicionarIndisponibilidade(v.getId(), dpI.getValue(), dpF.getValue());
                recarregarListaIndisp(v, lista);
                DialogHelper.sucesso("Período adicionado!");
            } catch (Exception ex) { DialogHelper.erro(ex.getMessage()); }
        });

        recarregarListaIndisp(v, lista);

        VBox content = new VBox(10,
            lbl("Início:"), dpI, lbl("Fim:"), dpF, btnAdd,
            new Separator(), lbl("Períodos definidos:"), lista);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: #141414;");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: #141414;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        DialogHelper.estilizar(dialog);
        dialog.showAndWait();
    }

    private void recarregarListaIndisp(Veiculo v, VBox lista) {
        lista.getChildren().clear();
        try {
            for (Indisponibilidade ind : veiculoService.listarIndisponibilidades(v.getId())) {
                Label txt = new Label("📅 " + ind.getInicio() + " → " + ind.getFim());
                txt.setStyle("-fx-text-fill: #ccc;");
                Button rem = new Button("✕ Remover");
                rem.getStyleClass().add("btn-danger");
                rem.setOnAction(ev -> {
                    try { veiculoService.removerIndisponibilidade(ind.getId()); recarregarListaIndisp(v, lista); }
                    catch (Exception ex) { DialogHelper.erro(ex.getMessage()); }
                });
                HBox h = new HBox(10, txt, rem);
                h.setAlignment(Pos.CENTER_LEFT);
                lista.getChildren().add(h);
            }
            if (lista.getChildren().isEmpty()) {
                Label vazio = new Label("Sem períodos definidos.");
                vazio.setStyle("-fx-text-fill: #666;");
                lista.getChildren().add(vazio);
            }
        } catch (Exception ex) { DialogHelper.erro(ex.getMessage()); }
    }

    private TextField campo(String valor, String prompt) {
        TextField tf = new TextField(valor);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("form-input");
        return tf;
    }

    private Label lbl(String txt) {
        Label l = new Label(txt);
        l.getStyleClass().add("form-label");
        return l;
    }

    @FXML public void irParaHome()     { App.navigateTo("Home"); }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos()  { App.navigateTo("Vehicles"); }
    @FXML public void irParaReservas()  { App.navigateTo("Reservas"); }
    @FXML public void irParaAdmin()     { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout() { NavbarHelper.logout(); }
}
