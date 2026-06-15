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
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.Session;

import java.util.List;
import java.util.Optional;

public class AdminController {
    @FXML private VBox veiculosPendentesList;
    @FXML private VBox todosVeiculosList;
    @FXML private VBox utilizadoresList;
    @FXML private VBox historicoList;

    private final VeiculoService veiculoService = new VeiculoService();
    private final UserService userService = new UserService();
    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        if (!Session.isAdmin()) { App.navigateTo("Dashboard"); return; }
        carregarVeiculosPendentes();
        carregarTodosVeiculos();
        carregarUtilizadores();
        carregarHistorico();
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
        VBox box = new VBox(6);
        box.getStyleClass().add("admin-row");
        box.setPadding(new Insets(10));

        Label info = new Label(String.format("🚗 %s  |  👤 %s  |  📍 %s  |  %.2f€/dia",
            v.getNomeCompleto(), v.getProprietarioNome(), v.getLocalizacao(), v.getPrecoPorDia()));
        info.getStyleClass().add("admin-user-name");
        info.setWrapText(true);

        Button btnAprovar = new Button("✅ Aprovar");
        btnAprovar.getStyleClass().add("btn-success");
        btnAprovar.setOnAction(e -> {
            try { veiculoService.aprovarVeiculo(v.getId()); carregarVeiculosPendentes(); mostrarSucesso("Veículo aprovado!"); }
            catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });
        Button btnRejeitar = new Button("❌ Rejeitar");
        btnRejeitar.getStyleClass().add("btn-danger");
        btnRejeitar.setOnAction(e -> {
            try { veiculoService.rejeitarVeiculo(v.getId()); carregarVeiculosPendentes(); }
            catch (Exception ex) { mostrarErro(ex.getMessage()); }
        });

        box.getChildren().addAll(info, new HBox(8, btnAprovar, btnRejeitar));
        return box;
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
        VBox box = new VBox(6);
        box.getStyleClass().add("admin-row");
        box.setPadding(new Insets(10));

        Label info = new Label(String.format("🚗 %s  |  👤 %s  |  📍 %s  |  %.2f€/dia  |  %s",
            v.getNomeCompleto(), v.getProprietarioNome(), v.getLocalizacao(), v.getPrecoPorDia(), v.getEstado()));
        info.setWrapText(true);

        // Admin pode remover (soft delete) qualquer veículo
        Button btnRemover = new Button("🗑️ Remover");
        btnRemover.getStyleClass().add("btn-danger");
        btnRemover.setOnAction(e -> {
            Optional<ButtonType> res = confirmar("Remover veículo?",
                "Tens a certeza que queres remover " + v.getNomeCompleto() + "?\nEsta ação é reversível apenas por base de dados.");
            if (res.isPresent() && res.get() == ButtonType.YES) {
                try { veiculoService.removerVeiculoAdmin(v.getId()); carregarTodosVeiculos(); mostrarSucesso("Veículo removido."); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }
        });

        box.getChildren().addAll(info, new HBox(8, btnRemover));
        return box;
    }

    private void carregarUtilizadores() {
        try {
            List<User> users = userService.listarTodos();
            utilizadoresList.getChildren().clear();
            int adminId = Session.getUser().getId();

            for (User u : users) {
                VBox box = new VBox(6);
                box.getStyleClass().add("admin-row");
                box.setPadding(new Insets(10));

                Label nome = new Label("👤 " + u.getNome() + "  |  " + u.getEmail() +
                    "  |  NIF: " + (u.getNif() != null ? u.getNif() : "-") +
                    "  |  Saldo: " + String.format("%.2f€", u.getSaldo()) +
                    "  |  " + (u.isBloqueado() ? "🔴 Suspenso" : "🟢 Ativo") +
                    "  |  Role: " + u.getRole());
                nome.getStyleClass().add("admin-user-name");
                nome.setWrapText(true);

                HBox btns = new HBox(8);

                // Admin NÃO pode suspender a si próprio
                if (u.getId() != adminId) {
                    Button btnToggle = new Button(u.isBloqueado() ? "✅ Reativar" : "🚫 Suspender");
                    btnToggle.getStyleClass().add(u.isBloqueado() ? "btn-success" : "btn-danger");
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

                box.getChildren().addAll(nome, btns);
                utilizadoresList.getChildren().add(box);
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }


    private void carregarHistorico() {
        try {
            List<Reserva> reservas = reservaService.listarTodas();
            historicoList.getChildren().clear();
            if (reservas.isEmpty()) {
                historicoList.getChildren().add(labelInfo("Sem reservas no histórico."));
            } else {
                for (Reserva r : reservas) {
                    VBox box = new VBox(4);
                    box.getStyleClass().add("admin-row");
                    box.setPadding(new Insets(8));
                    Label l = new Label(String.format("#%d  |  🚗 %s  |  👤 %s  |  %s → %s  |  %.2f€  |  %s",
                        r.getId(), r.getVeiculoNome(), r.getLocatarioNome(),
                        r.getDataInicio(), r.getDataFim(), r.getTotal(),
                        r.getEstado().toUpperCase()));
                    l.setWrapText(true);
                    box.getChildren().add(l);
                    historicoList.getChildren().add(box);
                }
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private Label labelInfo(String txt) {
        Label l = new Label(txt);
        l.getStyleClass().add("conta-email");
        l.setPadding(new Insets(10));
        return l;
    }

    private Optional<ButtonType> confirmar(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(titulo);
        a.setHeaderText(null);
        return a.showAndWait();
    }

    @FXML public void irParaHome()     { App.navigateTo("Home"); }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos()  { App.navigateTo("Vehicles"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void logout()          { Session.clear(); App.navigateTo("Home"); }

    private void mostrarErro(String msg)   { DialogHelper.erro(msg); }
    private void mostrarSucesso(String msg){ DialogHelper.sucesso(msg); }
}
