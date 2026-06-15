package pt.carguru.Controllers;

import javafx.fxml.FXML;
import pt.carguru.App;

public class HomeController {

    @FXML
    public void irParaLogin() {
        App.navigateTo("Login");
    }

    @FXML
    public void irParaRegisto() {
        App.navigateTo("Login");
        // O login view tem tab de registo — seleccionamos depois via controller
    }
}
