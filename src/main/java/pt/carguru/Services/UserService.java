package pt.carguru.Services;

import pt.carguru.Models.User;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserRepository userRepo = new UserRepository();

    public void atualizarPerfil(String novoNome, String novoNif) throws SQLException {
        User user = Session.getUser();
        if (user == null) throw new IllegalStateException("Não estás autenticado.");
        if (novoNome == null || novoNome.isBlank()) throw new IllegalArgumentException("Nome obrigatório.");
        if (novoNif != null && !novoNif.isBlank() && !novoNif.matches("\\d{9}"))
            throw new IllegalArgumentException("NIF deve ter 9 dígitos.");
        user.setNome(novoNome.trim());
        if (novoNif != null && !novoNif.isBlank()) user.setNif(novoNif.trim());
        userRepo.update(user);
        Session.setUser(user);
    }

    public void depositar(double valor) throws SQLException {
        if (valor <= 0) throw new IllegalArgumentException("Valor inválido.");
        User user = Session.getUser();
        user.setSaldo(user.getSaldo() + valor);
        userRepo.update(user);
        Session.setUser(user);
    }

    public void levantar(double valor) throws SQLException {
        if (valor <= 0) throw new IllegalArgumentException("Valor inválido.");
        User user = Session.getUser();
        if (user.getSaldo() < valor) throw new IllegalArgumentException("Saldo insuficiente.");
        user.setSaldo(user.getSaldo() - valor);
        userRepo.update(user);
        Session.setUser(user);
    }

    public List<User> listarTodos() throws SQLException { return userRepo.findAll(); }

    public void toggleAtivo(int userId) throws SQLException {
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        user.setBloqueado(!user.isBloqueado());
        userRepo.update(user);
    }

    // Admin: adicionar saldo a qualquer utilizador
    public void adicionarSaldoAdmin(int userId, double valor) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        if (valor <= 0) throw new IllegalArgumentException("Valor inválido.");
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        user.setSaldo(user.getSaldo() + valor);
        userRepo.update(user);
    }

    // Admin: remover saldo de qualquer utilizador
    public void removerSaldoAdmin(int userId, double valor) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        if (valor <= 0) throw new IllegalArgumentException("Valor inválido.");
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        if (user.getSaldo() < valor) throw new IllegalArgumentException("Saldo insuficiente.");
        user.setSaldo(user.getSaldo() - valor);
        userRepo.update(user);
    }
}
