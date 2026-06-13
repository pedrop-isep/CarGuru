package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pt.carguru.App;
import pt.carguru.Models.Reserva;
import pt.carguru.Services.ReservaService;
import pt.carguru.Utils.Session;

import java.util.List;

public class ReservasController {
    @FXML private TabPane tabPane;
    @FXML private VBox reservasLocatarioList;
    @FXML private VBox reservasProprietarioList;

    private final ReservaService reservaService = new ReservaService();

    @FXML
    public void initialize() {
        if (Session.getUser() == null) { App.navigateTo("Login"); return; }
        carregarReservasLocatario();
        carregarReservasProprietario();
    }

    private void carregarReservasLocatario() {
        try {
            List<Reserva> reservas = reservaService.minhasReservasComoLocatario();
            reservasLocatarioList.getChildren().clear();
            if (reservas.isEmpty()) {
                reservasLocatarioList.getChildren().add(new Label("Ainda não tens reservas como locatário."));
            } else {
                for (Reserva r : reservas) reservasLocatarioList.getChildren().add(criarCardLocatario(r));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private void carregarReservasProprietario() {
        try {
            List<Reserva> reservas = reservaService.minhasReservasComoProprietario();
            reservasProprietarioList.getChildren().clear();
            if (reservas.isEmpty()) {
                reservasProprietarioList.getChildren().add(new Label("Ainda não tens pedidos de reserva dos teus veículos."));
            } else {
                for (Reserva r : reservas) reservasProprietarioList.getChildren().add(criarCardProprietario(r));
            }
        } catch (Exception e) { mostrarErro(e.getMessage()); }
    }

    private VBox criarCardLocatario(Reserva r) {
        VBox card = new VBox(6);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(12));

        Label info = new Label(String.format("🚗 %s  |  %s → %s  |  %.2f€  |  %s",
                r.getVeiculoNome(), r.getDataInicio(), r.getDataFim(), r.getTotal(), r.getEstado().toUpperCase()));
        info.setWrapText(true);

        HBox btns = new HBox(8);

        if ("pendente".equals(r.getEstado()) || "confirmada".equals(r.getEstado())) {
            Button btnCancelar = new Button("❌ Cancelar");
            btnCancelar.setOnAction(e -> {
                try { reservaService.cancelarReserva(r.getId()); carregarReservasLocatario(); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });
            btns.getChildren().add(btnCancelar);
        }
        if ("confirmada".equals(r.getEstado()) && r.getKmInicial() == null) {
            Button btnKmIni = new Button("🔢 Km Inicial");
            btnKmIni.setOnAction(e -> pedirKm("Km Inicial", km -> {
                try { reservaService.registarKmInicial(r.getId(), km); carregarReservasLocatario(); mostrarSucesso("Km inicial registado!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }));
            btns.getChildren().add(btnKmIni);
        }
        if ("confirmada".equals(r.getEstado()) && r.getKmInicial() != null && r.getKmFinal() == null) {
            Button btnKmFin = new Button("🏁 Km Final + Liquidar");
            btnKmFin.setOnAction(e -> pedirKm("Km Final", km -> {
                try { reservaService.registarKmFinalELiquidar(r.getId(), km, 1.70, 6.0); carregarReservasLocatario(); mostrarSucesso("Reserva concluída e liquidada!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            }));
            btns.getChildren().add(btnKmFin);
        }
        if ("concluida".equals(r.getEstado()) && r.getAvaliacao() == null) {
            Button btnAval = new Button("⭐ Avaliar");
            btnAval.setOnAction(e -> abrirModalAvaliacao(r));
            btns.getChildren().add(btnAval);
        }

        card.getChildren().add(info);
        if (!btns.getChildren().isEmpty()) card.getChildren().add(btns);
        return card;
    }

    private VBox criarCardProprietario(Reserva r) {
        VBox card = new VBox(6);
        card.getStyleClass().add("reserva-card");
        card.setPadding(new Insets(12));

        Label info = new Label(String.format("👤 %s  |  %s → %s  |  %.2f€  |  %s",
                r.getLocatarioNome(), r.getDataInicio(), r.getDataFim(), r.getTotal(), r.getEstado().toUpperCase()));
        info.setWrapText(true);

        HBox btns = new HBox(8);
        if ("pendente".equals(r.getEstado())) {
            Button btnAprovar = new Button("✅ Aprovar");
            Button btnRecusar = new Button("❌ Recusar");
            btnAprovar.setOnAction(e -> {
                try { reservaService.aprovarReserva(r.getId()); carregarReservasProprietario(); mostrarSucesso("Reserva aprovada!"); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });
            btnRecusar.setOnAction(e -> {
                try { reservaService.cancelarReserva(r.getId()); carregarReservasProprietario(); }
                catch (Exception ex) { mostrarErro(ex.getMessage()); }
            });
            btns.getChildren().addAll(btnAprovar, btnRecusar);
        }

        card.getChildren().add(info);
        if (!btns.getChildren().isEmpty()) card.getChildren().add(btns);
        return card;
    }

    private void pedirKm(String titulo, java.util.function.Consumer<Integer> callback) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(titulo); dlg.setHeaderText(titulo + ":"); dlg.setContentText("Quilómetros:");
        dlg.showAndWait().ifPresent(val -> {
            try { callback.accept(Integer.parseInt(val.trim())); }
            catch (NumberFormatException e) { mostrarErro("Valor inválido."); }
        });
    }

    private void abrirModalAvaliacao(Reserva r) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Avaliar reserva");
        ComboBox<Integer> estrelas = new ComboBox<>(); estrelas.getItems().addAll(1,2,3,4,5); estrelas.setValue(5);
        TextArea comentario = new TextArea(); comentario.setPromptText("Comentário (opcional)"); comentario.setPrefRowCount(3);
        VBox content = new VBox(10, new Label("Avaliação (estrelas):"), estrelas, new Label("Comentário:"), comentario);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try { reservaService.avaliarReserva(r.getId(), estrelas.getValue(), comentario.getText()); carregarReservasLocatario(); mostrarSucesso("Avaliação submetida!"); }
                catch (Exception e) { mostrarErro(e.getMessage()); }
            }
        });
    }

    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos() { App.navigateTo("Vehicles"); }
    @FXML public void irParaConta() { App.navigateTo("Conta"); }
    @FXML public void irParaAdmin() { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout() { Session.clear(); App.navigateTo("Login"); }

    private void mostrarErro(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void mostrarSucesso(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
