package pt.carguru.Controllers;

import javafx.scene.control.Button;
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
}
