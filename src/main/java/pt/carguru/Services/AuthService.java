package pt.carguru.Services;

import pt.carguru.Models.User;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Utils.EmailSender;
import pt.carguru.Utils.PasswordHasher;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuthService {
    private final UserRepository userRepo = new UserRepository();

    public User login(String email, String password) throws SQLException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password obrigatória.");

        User user = userRepo.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Email ou password incorretos."));

        if (!PasswordHasher.verify(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Email ou password incorretos.");
        if (user.isBloqueado())
            throw new IllegalStateException("Conta suspensa. Contacta o suporte.");

        Session.setUser(user);
        return user;
    }

    public User registar(String nome, String email, String password, String confirmPassword,
                          String nif, String nCarta, LocalDate validadeCarta) throws SQLException {
        if (nome == null || nome.isBlank())        throw new IllegalArgumentException("Nome obrigatório.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("Email inválido.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password deve ter pelo menos 6 caracteres.");
        if (!password.equals(confirmPassword))     throw new IllegalArgumentException("As passwords não coincidem.");
        if (nif == null || !nif.matches("\\d{9}")) throw new IllegalArgumentException("NIF deve ter 9 dígitos.");
        if (nCarta == null || nCarta.isBlank())    throw new IllegalArgumentException("Número de carta obrigatório.");
        if (validadeCarta == null || validadeCarta.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Validade da carta inválida.");
        if (userRepo.emailExists(email.trim().toLowerCase()))
            throw new IllegalArgumentException("Já existe uma conta com este email.");

        User user = new User();
        user.setNome(nome.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setNif(nif.trim());
        user.setNCartaConducao(nCarta.trim());
        user.setValidadeCarta(validadeCarta);
        user.setRole("UTILIZADOR");
        user.setSaldo(0.0);

        userRepo.save(user);
        Session.setUser(user);
        return user;
    }

    /** Gera token, guarda na BD e tenta enviar por email. Retorna token para demo. */
    public String iniciarRecuperacao(String email) throws SQLException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");
        User user = userRepo.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Não existe conta com este email."));

        String token = UUID.randomUUID().toString();
        userRepo.saveResetToken(user.getId(), token, LocalDateTime.now().plusHours(2));

        // Tenta enviar email (não bloqueia se falhar)
        try {
            EmailSender.enviarTokenRecuperacao(user.getEmail(), token);
        } catch (Exception e) {
            System.err.println("[AUTH] Email não enviado: " + e.getMessage());
        }
        return token;
    }

    public void redefinirPassword(String token, String novaPassword, String confirmPassword) throws SQLException {
        if (novaPassword == null || novaPassword.length() < 6)
            throw new IllegalArgumentException("Password deve ter pelo menos 6 caracteres.");
        if (!novaPassword.equals(confirmPassword))
            throw new IllegalArgumentException("As passwords não coincidem.");
        User user = userRepo.findByResetToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));
        userRepo.updatePassword(user.getId(), PasswordHasher.hash(novaPassword));
    }
}
