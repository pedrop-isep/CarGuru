package pt.carguru.Utils;

import pt.carguru.Models.User;
import pt.carguru.Repositories.UserRepository;

public class Session {
    private static User currentUser = null;
    private static final UserRepository userRepo = new UserRepository();

    public static void setUser(User user) { currentUser = user; }
    public static User getUser() { return currentUser; }
    public static boolean isLoggedIn() { return currentUser != null; }
    public static boolean isAdmin() {
        return currentUser != null && "ADMINISTRADOR".equalsIgnoreCase(currentUser.getRole());
    }
    public static void clear() { currentUser = null; }

    /**
     * Recarrega os dados do utilizador atual a partir da base de dados (nome, saldo, etc.)
     * e atualiza a sessão em memória. Deve ser chamado sempre que uma operação altera
     * dados do utilizador diretamente na base de dados (ex.: liquidação de reservas),
     * para evitar que o saldo mostrado na interface fique desatualizado até um novo login.
     */
    public static void refresh() {
        if (currentUser == null) return;
        try {
            userRepo.findById(currentUser.getId()).ifPresent(u -> currentUser = u);
        } catch (Exception ignored) {
            // Em caso de falha de ligação, mantém os dados em memória.
        }
    }
}
