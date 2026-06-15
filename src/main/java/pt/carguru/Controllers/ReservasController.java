package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Reserva;
import pt.carguru.Services.ReservaService;
import pt.carguru.Utils.DialogHelper;
import pt.carguru.Utils.Session;

import java.util.List;

public class ReservasController {
    @FXML private VBox reservasLocatarioList;
    @FXML private VBox reservasProprietarioList;
    @FXML private Button btnAdmin;

    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        if (Session.getUser() == null) { App.navigateTo("Login"); return; }
        NavbarHelper.configurar(btnAdmin);
        carregarLocatario();
        carregarProprietario();
    }

    private void carregarLocatario() {
        try {
            List<Reserva> lista = reservaService.minhasReservasComoLocatario();
            reservasLocatarioList.getChildren().clear();
            if (lista.isEmpty()) {
                reservasLocatarioList.getChildren().add(vazio("Ainda não tens reservas como locatário."));
            } else {
                for (Reserva r : lista) reservasLocatarioList.getChildren().add(cardLocatario(r));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private void carregarProprietario() {
        try {
            List<Reserva> lista = reservaService.minhasReservasComoProprietario();
            reservasProprietarioList.getChildren().clear();
            if (lista.isEmpty()) {
                reservasProprietarioList.getChildren().add(vazio("Ainda não tens pedidos de reserva nos teus veículos."));
            } else {
                for (Reserva r : lista) reservasProprietarioList.getChildren().add(cardProprietario(r));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

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
        Label total = new Label(String.format("💶 %.2f€", r.getTotal()));
        total.getStyleClass().add("reserva-total");
        Label prop = new Label("👤 Proprietário: " + (r.getProprietarioNome() != null ? r.getProprietarioNome() : "-"));
        prop.getStyleClass().add("reserva-datas");

        HBox btns = new HBox(8);
        if ("pendente".equals(r.getEstado()) || "confirmada".equals(r.getEstado())) {
            Button btnC = new Button("❌ Cancelar");
            btnC.getStyleClass().add("btn-danger");
            btnC.setOnAction(e -> {
                if (confirmar("Cancelar reserva?")) {
                    try { reservaService.cancelarReserva(r.getId()); carregarLocatario(); }
                    catch (Exception ex) { mostrarErro(ex.getMessage()); }
                }
            });
            btns.getChildren().add(btnC);
        }
        if ("confirmada".equals(r.getEstado()) && r.getKmInicial() == null) {
            Button btnK = new Button("🔢 Registo Km Inicial");
            btnK.getStyleClass().add("btn-secondary");
            btnK.setOnAction(e -> pedirKm("Km Inicial", km -> {
                try { reservaService.registarKmInicial(r.getId(), km); carregarLocatario(); mostrarSucesso("Km inicial registado!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }));
            btns.getChildren().add(btnK);
        }
        if ("confirmada".equals(r.getEstado()) && r.getKmInicial() != null && r.getKmFinal() == null) {
            Button btnF = new Button("🏁 Km Final + Liquidar");
            btnF.getStyleClass().add("btn-success");
            btnF.setOnAction(e -> pedirKm("Km Final", km -> {
                try { reservaService.registarKmFinalELiquidar(r.getId(), km, 1.70, 6.0); carregarLocatario(); mostrarSucesso("Reserva concluída e liquidada!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }));
            btns.getChildren().add(btnF);
        }
        if ("concluida".equals(r.getEstado()) && r.getAvaliacao() == null) {
            Button btnA = new Button("⭐ Avaliar");
            btnA.getStyleClass().add("btn-primary");
            btnA.setOnAction(e -> abrirAvaliacao(r));
            btns.getChildren().add(btnA);
        }

        card.getChildren().addAll(top, datas, total, prop);
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
                try { reservaService.aprovarReserva(r.getId()); carregarProprietario(); mostrarSucesso("Reserva aprovada!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });
            Button btnRe = new Button("❌ Recusar");
            btnRe.getStyleClass().add("btn-danger");
            btnRe.setOnAction(e -> {
                if (confirmar("Recusar reserva?")) {
                    try { reservaService.cancelarReserva(r.getId()); carregarProprietario(); }
                    catch (Exception ex) { mostrarErro(ex.getMessage()); }
                }
            });
            btns.getChildren().addAll(btnAp, btnRe);
        }

        card.getChildren().addAll(top, loc, datas, total);
        if (!btns.getChildren().isEmpty()) card.getChildren().add(btns);
        return card;
    }

    private String estadoEmoji(String estado) {
        return switch (estado) {
            case "pendente" -> "🕐";
            case "confirmada" -> "✅";
            case "cancelada" -> "❌";
            case "concluida" -> "🏁";
            default -> "•";
        };
    }

    private Label vazio(String msg) {
        Label l = new Label(msg);
        l.getStyleClass().add("conta-email");
        l.setPadding(new Insets(16));
        return l;
    }

    private void pedirKm(String titulo, java.util.function.Consumer<Integer> cb) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(titulo); dlg.setHeaderText(titulo + ":"); dlg.setContentText("Quilómetros:");
        dlg.showAndWait().ifPresent(v -> {
            try { cb.accept(Integer.parseInt(v.trim())); }
            catch (NumberFormatException e) { mostrarErro("Valor inválido."); }
        });
    }

    private void abrirAvaliacao(Reserva r) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Avaliar reserva");
        ComboBox<Integer> estrelas = new ComboBox<>();
        estrelas.getItems().addAll(1,2,3,4,5); estrelas.setValue(5);
        TextArea comentario = new TextArea(); comentario.setPromptText("Comentário (opcional)"); comentario.setPrefRowCount(3);
        VBox c = new VBox(10, new Label("Avaliação (estrelas):"), estrelas, new Label("Comentário:"), comentario);
        c.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(c);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dlg.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try { reservaService.avaliarReserva(r.getId(), estrelas.getValue(), comentario.getText()); carregarLocatario(); mostrarSucesso("Avaliação submetida!"); }
                catch (Exception e) { mostrarErro(e.getMessage()); }
            }
        });
    }

    private boolean confirmar(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    @FXML public void irParaHome()     { App.navigateTo("Home"); }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos()  { App.navigateTo("Vehicles"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void irParaAdmin()     { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout()          { Session.clear(); App.navigateTo("Home"); }

    private void mostrarErro(String msg)    { DialogHelper.erro(msg); }
    private void mostrarSucesso(String msg) { DialogHelper.sucesso(msg); }
}
