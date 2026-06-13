package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pt.carguru.App;
import pt.carguru.Models.Indisponibilidade;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.UserService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.Session;

import java.time.LocalDate;
import java.util.List;

public class ContaController {
    // Perfil
    @FXML private Label avatarLabel;
    @FXML private Label nomeLabel;
    @FXML private Label emailLabel;
    @FXML private TextField perfilNome;
    @FXML private TextField perfilEmail;
    @FXML private TextField perfilNif;
    @FXML private Label perfilErro;

    // Saldo
    @FXML private Label saldoLabel;

    // Veículos
    @FXML private VBox veiculosList;

    private final UserService userService = new UserService();
    private final VeiculoService veiculoService = new VeiculoService();

    @FXML
    public void initialize() {
        User user = Session.getUser();
        if (user == null) { App.navigateTo("Login"); return; }
        carregarPerfil(user);
        carregarVeiculos();
    }

    private void carregarPerfil(User user) {
        avatarLabel.setText(user.getInitials());
        nomeLabel.setText(user.getNome());
        emailLabel.setText(user.getEmail());
        perfilNome.setText(user.getNome());
        perfilEmail.setText(user.getEmail());
        perfilNif.setText(user.getNif() != null ? user.getNif() : "");
        saldoLabel.setText(String.format("%.2f€", user.getSaldo()));
    }

    @FXML
    public void handleGuardarPerfil() {
        perfilErro.setText("");
        try {
            userService.atualizarPerfil(perfilNome.getText(), perfilNif.getText());
            carregarPerfil(Session.getUser());
            mostrarSucesso("Perfil atualizado com sucesso!");
        } catch (Exception e) { perfilErro.setText(e.getMessage()); }
    }

    @FXML
    public void handleDepositar() {
        TextInputDialog dlg = new TextInputDialog("50");
        dlg.setTitle("Depositar"); dlg.setHeaderText("Valor a depositar (€):"); dlg.setContentText("Valor:");
        dlg.showAndWait().ifPresent(val -> {
            try {
                userService.depositar(Double.parseDouble(val));
                saldoLabel.setText(String.format("%.2f€", Session.getUser().getSaldo()));
                mostrarSucesso("Depósito realizado!");
            } catch (Exception e) { mostrarErro(e.getMessage()); }
        });
    }

    @FXML
    public void handleLevantar() {
        TextInputDialog dlg = new TextInputDialog("50");
        dlg.setTitle("Levantar"); dlg.setHeaderText("Valor a levantar (€):"); dlg.setContentText("Valor:");
        dlg.showAndWait().ifPresent(val -> {
            try {
                userService.levantar(Double.parseDouble(val));
                saldoLabel.setText(String.format("%.2f€", Session.getUser().getSaldo()));
                mostrarSucesso("Levantamento realizado!");
            } catch (Exception e) { mostrarErro(e.getMessage()); }
        });
    }

    private void carregarVeiculos() {
        try {
            List<Veiculo> veiculos = veiculoService.listarMeusVeiculos();
            veiculosList.getChildren().clear();
            if (veiculos.isEmpty()) {
                veiculosList.getChildren().add(new Label("Ainda não tens veículos. Clica em + Adicionar."));
            } else {
                for (Veiculo v : veiculos) veiculosList.getChildren().add(criarRowVeiculo(v));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox criarRowVeiculo(Veiculo v) {
        VBox row = new VBox(4);
        row.getStyleClass().add("veiculo-row");
        row.setPadding(new Insets(10));

        Label info = new Label("🚗 " + v.getNomeCompleto() + "  |  " + v.getLocalizacao() + "  |  " + String.format("%.2f€/dia", v.getPrecoPorDia()) + "  |  Estado: " + v.getEstado());
        info.setWrapText(true);

        Button btnEditar = new Button("✏️ Editar");
        Button btnRemover = new Button("🗑️ Remover");
        Button btnIndisp = new Button("📅 Indisponibilidade");

        btnEditar.setOnAction(e -> abrirModalEditar(v));
        btnRemover.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Remover " + v.getNomeCompleto() + "?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try { veiculoService.removerVeiculo(v.getId()); carregarVeiculos(); }
                    catch (Exception ex) { mostrarErro(ex.getMessage()); }
                }
            });
        });
        btnIndisp.setOnAction(e -> abrirModalIndisponibilidade(v));

        javafx.scene.layout.HBox btns = new javafx.scene.layout.HBox(8, btnEditar, btnRemover, btnIndisp);
        row.getChildren().addAll(info, btns);
        return row;
    }

    @FXML
    public void handleAdicionarVeiculo() {
        abrirModalVeiculo(null);
    }

    private void abrirModalVeiculo(Veiculo vExistente) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(vExistente == null ? "Adicionar Veículo" : "Editar Veículo");

        TextField marca = new TextField(vExistente != null ? vExistente.getMarca() : "");
        TextField modelo = new TextField(vExistente != null ? vExistente.getModelo() : "");
        TextField ano = new TextField(vExistente != null ? String.valueOf(vExistente.getAno()) : "");
        ComboBox<String> combustivel = new ComboBox<>(); combustivel.getItems().addAll("Gasolina","Gasóleo","Elétrico","GPL","Híbrido"); combustivel.setValue(vExistente != null ? vExistente.getCombustivel() : "Gasolina");
        ComboBox<String> transmissao = new ComboBox<>(); transmissao.getItems().addAll("Manual","Automática"); transmissao.setValue(vExistente != null ? vExistente.getTransmissao() : "Manual");
        TextField localizacao = new TextField(vExistente != null ? vExistente.getLocalizacao() : "");
        TextField preco = new TextField(vExistente != null ? String.valueOf(vExistente.getPrecoPorDia()) : "");
        TextField consumo = new TextField(vExistente != null ? String.valueOf(vExistente.getConsumo()) : "6.0");
        TextArea descricao = new TextArea(vExistente != null ? vExistente.getDescricao() : ""); descricao.setPrefRowCount(3);

        VBox content = new VBox(8,
            new Label("Marca:"), marca, new Label("Modelo:"), modelo, new Label("Ano:"), ano,
            new Label("Combustível:"), combustivel, new Label("Transmissão:"), transmissao,
            new Label("Localização:"), localizacao, new Label("Preço/dia (€):"), preco,
            new Label("Consumo (L/100km):"), consumo, new Label("Descrição:"), descricao);
        content.setPadding(new Insets(16));
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true); sp.setPrefHeight(450);
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    int anoVal = Integer.parseInt(ano.getText());
                    double precoVal = Double.parseDouble(preco.getText());
                    double consumoVal = Double.parseDouble(consumo.getText());
                    if (vExistente == null) {
                        veiculoService.adicionarVeiculo(marca.getText(), modelo.getText(), anoVal, combustivel.getValue(), transmissao.getValue(), localizacao.getText(), precoVal, consumoVal, descricao.getText());
                        mostrarSucesso("Veículo submetido para aprovação!");
                    } else {
                        veiculoService.editarVeiculo(vExistente.getId(), marca.getText(), modelo.getText(), anoVal, combustivel.getValue(), transmissao.getValue(), localizacao.getText(), precoVal, consumoVal, descricao.getText());
                        mostrarSucesso("Veículo atualizado!");
                    }
                    carregarVeiculos();
                } catch (Exception e) { mostrarErro(e.getMessage()); }
            }
        });
    }

    private void abrirModalEditar(Veiculo v) { abrirModalVeiculo(v); }

    private void abrirModalIndisponibilidade(Veiculo v) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Indisponibilidade — " + v.getNomeCompleto());

        DatePicker dpInicio = new DatePicker(LocalDate.now());
        DatePicker dpFim = new DatePicker(LocalDate.now().plusDays(7));
        VBox lista = new VBox(6);

        Runnable recarregar = () -> {
            lista.getChildren().clear();
            try {
                veiculoService.listarIndisponibilidades(v.getId()).forEach(ind -> {
                    Button rem = new Button("✕");
                    rem.setOnAction(ev -> {
                        try { veiculoService.removerIndisponibilidade(ind.getId()); recarregar.run(); }
                        catch (Exception ex) { mostrarErro(ex.getMessage()); }
                    });
                    javafx.scene.layout.HBox h = new javafx.scene.layout.HBox(8, new Label(ind.getInicio() + " → " + ind.getFim()), rem);
                    lista.getChildren().add(h);
                });
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        };

        Button btnAdd = new Button("+ Adicionar período");
        btnAdd.setOnAction(e -> {
            try {
                veiculoService.adicionarIndisponibilidade(v.getId(), dpInicio.getValue(), dpFim.getValue());
                recarregar.run();
            } catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });

        recarregar.run();
        VBox content = new VBox(10, new Label("Início:"), dpInicio, new Label("Fim:"), dpFim, btnAdd, new Separator(), new Label("Períodos definidos:"), lista);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos() { App.navigateTo("Vehicles"); }
    @FXML public void irParaReservas() { App.navigateTo("Reservas"); }
    @FXML public void irParaAdmin() { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout() { Session.clear(); App.navigateTo("Login"); }

    private void mostrarErro(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void mostrarSucesso(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
