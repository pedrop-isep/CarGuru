package pt.carguru.Utils;

import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import pt.carguru.App;
import pt.carguru.Utils.Session;

/**
 * Helper para configurar a navbar em todas as vistas:
 * - Esconde botão Admin a não-admins
 */
public class NavbarHelper {
    public static void configurar(Button btnAdmin) {
        if (btnAdmin != null) {
            boolean isAdmin = Session.isAdmin();
            btnAdmin.setVisible(isAdmin);
            btnAdmin.setManaged(isAdmin);
        }
    }

    public static void logout() {
        DialogHelper.confirmar("Logout", "Tens a certeza que queres sair?")
                .ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        Session.clear();
                        App.navigateTo("Home");
                    }
                });
    }

}
