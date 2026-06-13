package pt.carguru.Services;

import pt.carguru.Models.User;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Utils.PasswordHasher;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepo = new UserRepository();

    /** CAR-11: Login */
    public User login(String email, String password) throws SQLException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password obrigatória.");

        Optional<User> opt = userRepo.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) throw new IllegalArgumentException("Email ou password incorretos.");

        User user = opt.get();
        if (!user.isAtivo()) throw new IllegalArgumentException("Conta suspensa. Contacte o suporte.");
        if (!PasswordHasher.verify(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Email ou password incorretos.");

        Session.setUser(user);
        return user;
    }

    /** CAR-6: Registo de utilizador */
    public User registar(String nome, String email, String nif, String password, String confirmPassword) throws SQLException {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");
        if (nif == null || !nif.matches("\\d{9}")) throw new IllegalArgumentException("NIF deve ter 9 dígitos.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password deve ter pelo menos 6 caracteres.");
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("As passwords não coincidem.");

        String emailNorm = email.trim().toLowerCase();
        if (userRepo.emailExists(emailNorm)) throw new IllegalArgumentException("Este email já está registado.");

        User user = new User();
        user.setNome(nome.trim());
        user.setEmail(emailNorm);
        user.setNif(nif.trim());
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setRole("utilizador");
        user.setSaldo(0.0);
        user.setAtivo(true);

        return userRepo.save(user);
    }

    /** CAR-17: Logout */
    public void logout() {
        Session.clear();
    }

    /** CAR-23: Recuperação de password (gera token – simplificado) */
    public String iniciarRecuperacaoPassword(String email) throws SQLException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");
        Optional<User> opt = userRepo.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) throw new IllegalArgumentException("Não existe nenhuma conta com esse email.");
        // Em produção enviaria um email com token. Para fins académicos retorna mensagem de sucesso.
        return "Instruções enviadas para " + email + ". Verifica a tua caixa de entrada.";
    }
}
