package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Disputa;
import pt.carguru.Models.Reserva;
import pt.carguru.Models.User;
import pt.carguru.Models.Veiculo;
import pt.carguru.Services.DisputaService;
import pt.carguru.Services.ReservaService;
import pt.carguru.Services.UserService;
import pt.carguru.Services.VeiculoService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.NavbarHelper;
import pt.carguru.Utils.Session;

import java.util.List;
import java.util.Optional;

public class AdminController {
    @FXML private VBox veiculosPendentesList;
    @FXML private VBox todosVeiculosList;
    @FXML private VBox utilizadoresList;
    @FXML private VBox historicoList;
    @FXML private VBox disputasList;

    private final VeiculoService veiculoService = new VeiculoService();
    private final UserService userService = new UserService();
    private final ReservaService reservaService = new ReservaService();
    private final DisputaService disputaService = new DisputaService();

    @FXML
    public void initialize() {
        if (!Session.isAdmin()) { App.navigateTo("Dashboard"); return; }
        carregarVeiculosPendentes();
        carregarTodosVeiculos();
        carregarUtilizadores();
        carregarHistorico();
        carregarDisputas();
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

    private void carregarDisputas() {
        try {
            List<Disputa> disputas = disputaService.listarTodas();
            disputasList.getChildren().clear();
            if (disputas.isEmpty()) {
                disputasList.getChildren().add(labelInfo("✅ Não há disputas registadas."));
            } else {
                for (Disputa d : disputas)
                    disputasList.getChildren().add(rowDisputa(d));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox rowDisputa(Disputa d) {
        VBox box = new VBox(8);
        box.getStyleClass().add("admin-row");
        box.setPadding(new javafx.geometry.Insets(12));

        // Cabeçalho
        Label titulo = new Label(String.format("⚖️ Disputa #%d  |  Reserva #%d  |  🚗 %s",
                d.getId(), d.getReservaId(), d.getVeiculoNome()));
        titulo.getStyleClass().add("admin-user-name");
        titulo.setWrapText(true);

        Label partes = new Label(String.format(
                "👤 Locatário: %s   |   🏠 Proprietário: %s   |   💰 Caução: %.2f€",
                d.getLocatarioNome(), d.getProprietarioNome(), d.getCaucao()));
        partes.setStyle("-fx-text-fill: #aaa; -fx-font-size: 0.85em;");
        partes.setWrapText(true);

        Label estado = new Label(d.getEstadoLabel());
        estado.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 0.85em;");

        Label descricaoLbl = new Label("📋 " + d.getDescricao());
        descricaoLbl.setStyle("-fx-text-fill: #ccc; -fx-font-size: 0.82em;");
        descricaoLbl.setWrapText(true);

        box.getChildren().addAll(titulo, partes, estado, descricaoLbl);

        if (d.getResolucao() != null && !d.getResolucao().isBlank()) {
            Label resLbl = new Label("✏️ Resolução: " + d.getResolucao());
            resLbl.setStyle("-fx-text-fill: #6ee7b7; -fx-font-size: 0.82em;");
            resLbl.setWrapText(true);
            box.getChildren().add(resLbl);
        }

        // Botões de ação (apenas para disputas não resolvidas)
        if (!d.isResolvida()) {
            HBox btns = new HBox(8);

            Button btnAnalise = new Button("🔍 Marcar Em Análise");
            btnAnalise.getStyleClass().add("btn-primary");
            btnAnalise.setOnAction(e -> {
                try {
                    disputaService.iniciarAnalise(d.getId());
                    carregarDisputas();
                    mostrarSucesso("Disputa marcada como Em Análise.");
                } catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });

            Button btnFavProp = new Button("🏠 Resolver → Proprietário");
            btnFavProp.getStyleClass().add("btn-danger");
            btnFavProp.setOnAction(e -> abrirDialogoResolucao(d, "proprietario"));

            Button btnFavLoc = new Button("👤 Resolver → Locatário");
            btnFavLoc.getStyleClass().add("btn-success");
            btnFavLoc.setOnAction(e -> abrirDialogoResolucao(d, "locatario"));

            Button btnEncerrar = new Button("⚫ Encerrar sem penalização");
            btnEncerrar.setStyle("-fx-background-color: #374151; -fx-text-fill: #d1d5db; " +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-font-size: 0.82em;");
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

        VBox conteudo = new VBox(10, infoLbl, resolucaoLbl, tfResolucao);

        // Campo de valor apenas quando há transferência financeira
        javafx.scene.control.TextField tfValor = null;
        Label valorLbl = null;
        if (!modo.equals("encerrar")) {
            String labelValor = modo.equals("proprietario")
                    ? String.format("Penalização ao locatário (máx. %.2f€):", d.getCaucao())
                    : String.format("Valor a devolver ao locatário (máx. %.2f€):", d.getCaucao());
            valorLbl = new Label(labelValor);
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
                        mostrarSucesso(String.format("Disputa resolvida a favor do proprietário. Penalização: %.2f€", pen));
                    }
                    case "locatario" -> {
                        double ree = parseValor(tfValorFinal, d.getCaucao());
                        disputaService.resolverFavorLocatario(d.getId(), resolucao, ree);
                        mostrarSucesso(String.format("Disputa resolvida a favor do locatário. Devolvido: %.2f€", ree));
                    }
                    default -> {
                        disputaService.encerrar(d.getId(), resolucao);
                        mostrarSucesso("Disputa encerrada sem penalização.");
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
    @FXML public void irParaReservas()   { App.navigateTo("Reservas"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void logout() { NavbarHelper.logout(); }

    private void mostrarErro(String msg)   { DialogHelper.erro(msg); }
    private void mostrarSucesso(String msg){ DialogHelper.sucesso(msg); }
}
