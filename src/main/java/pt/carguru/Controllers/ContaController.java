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
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.UserService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.Session;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
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
    @FXML private VBox veiculosList;
    @FXML private Button btnAdmin;

    private final UserService userService = new UserService();
    private final VeiculoService veiculoService = new VeiculoService();

    @FXML
    public void initialize() {
        User user = Session.getUser();
        if (user == null) { App.navigateTo("Login"); return; }
        NavbarHelper.configurar(btnAdmin);
        carregarPerfil(user);
        carregarVeiculos();
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
        saldoLabel.setText(String.format("%.2f€", user.getSaldo()));
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
                                saldoLabel.setText(String.format("%.2f€", Session.getUser().getSaldo()));
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
                                saldoLabel.setText(String.format("%.2f€", Session.getUser().getSaldo()));
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
    @FXML public void logout()          { Session.clear(); App.navigateTo("Home"); }
}
