package pt.carguru.Services;

import pt.carguru.Models.User;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Utils.PasswordHasher;
import pt.carguru.Utils.Session;
import pt.carguru.Utils.EmailSender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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

    /** Registo de utilizador */
    public User registar(String nome, String email, String nif, String nCartaConducao,
                         String validadeCarta, String password, String confirmPassword) throws SQLException {

        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");
        if (nif == null || !nif.matches("\\d{9}")) throw new IllegalArgumentException("NIF deve ter 9 dígitos.");
        if (nCartaConducao == null || nCartaConducao.isBlank()) throw new IllegalArgumentException("Número de carta obrigatório.");
        if (validadeCarta == null || validadeCarta.isBlank()) throw new IllegalArgumentException("Validade da carta obrigatória.");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password deve ter pelo menos 6 caracteres.");
        if (!password.equals(confirmPassword)) throw new IllegalArgumentException("As passwords não coincidem.");

        // Validar formato e validade da carta
        LocalDate validade;
        try {
            validade = LocalDate.parse(validadeCarta); // formato YYYY-MM-DD
        } catch (Exception e) {
            throw new IllegalArgumentException("Data de validade inválida. Use o formato AAAA-MM-DD.");
        }
        if (validade.isBefore(LocalDate.now())) throw new IllegalArgumentException("Carta de condução expirada.");

        String emailNorm = email.trim().toLowerCase();
        if (userRepo.emailExists(emailNorm)) throw new IllegalArgumentException("Este email já está registado.");

        User user = new User();
        user.setNome(nome.trim());
        user.setEmail(emailNorm);
        user.setNif(nif.trim());
        user.setNCartaConducao(nCartaConducao.trim());
        user.setValidadeCarta(validade);
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setRole("UTILIZADOR");
        user.setSaldo(0.0);
        user.setBloqueado(false);

        return userRepo.save(user);
    }

    /** Recuperação de Password */
    public void iniciarRecuperacaoPassword(String email) throws SQLException {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email obrigatório.");

        Optional<User> opt = userRepo.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) throw new IllegalArgumentException("Não existe nenhuma conta com esse email.");

        // Gera token aleatório
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        userRepo.saveResetToken(opt.get().getId(), token, expiry);

        // Envia email (podes usar o EmailSender que ainda não tens, ou simular)
        EmailSender.enviarTokenRecuperacao(opt.get().getEmail(), token);
    }

    public void concluirRecuperacaoPassword(String token, String novaPassword, String confirmar) throws SQLException {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("Token obrigatório.");
        if (novaPassword == null || novaPassword.length() < 6) throw new IllegalArgumentException("Password deve ter pelo menos 6 caracteres.");
        if (!novaPassword.equals(confirmar)) throw new IllegalArgumentException("As passwords não coincidem.");

        Optional<User> opt = userRepo.findByResetToken(token.trim());
        if (opt.isEmpty()) throw new IllegalArgumentException("Código inválido ou expirado.");

        User user = opt.get();
        userRepo.updatePassword(user.getId(), PasswordHasher.hash(novaPassword));
        userRepo.clearResetToken(user.getId());
    }

    /** CAR-17: Logout */
    public void logout() {
        Session.clear();
    }
}
