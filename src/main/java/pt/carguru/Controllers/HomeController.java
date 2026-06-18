package pt.carguru.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import pt.carguru.App;
import pt.carguru.Utils.NavbarHelper;
import pt.carguru.Utils.Session;

public class HomeController {

    @FXML private HBox navPublica;
    @FXML private HBox navAutenticada;
    @FXML private Button btnAdmin;

    @FXML
    public void initialize() {
        boolean loggedIn = Session.getUser() != null;
        if (navPublica != null)     { navPublica.setVisible(!loggedIn);  navPublica.setManaged(!loggedIn); }
        if (navAutenticada != null) { navAutenticada.setVisible(loggedIn); navAutenticada.setManaged(loggedIn); }
        if (btnAdmin != null) NavbarHelper.configurar(btnAdmin);
    }

    @FXML public void irParaHome()     { /* já estamos aqui */ }
    @FXML public void irParaLogin()     { App.navigateTo("Login"); }
    @FXML public void irParaRegisto()   { App.navigateTo("Login"); }
    @FXML public void irParaAnunciar()  {
        if (Session.getUser() != null) App.navigateTo("Conta");
        else App.navigateTo("Login");
    }
    @FXML public void irParaDashboard() { App.navigateTo("Dashboard"); }
    @FXML public void irParaVeiculos() {
        if (Session.getUser() != null) App.navigateTo("Vehicles");
        else App.navigateTo("Login");
    }
    @FXML public void irParaReservas()  { App.navigateTo("Reservas"); }
    @FXML public void irParaConta()     { App.navigateTo("Conta"); }
    @FXML public void irParaAdmin()     { if (Session.isAdmin()) App.navigateTo("Admin"); }
    @FXML public void logout() { NavbarHelper.logout(); }
}
