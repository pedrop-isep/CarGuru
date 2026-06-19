package pt.carguru.Services;

import pt.carguru.Models.Transacao;
import pt.carguru.Models.User;
import pt.carguru.Repositories.ReservaRepository;
import pt.carguru.Repositories.TransacaoRepository;
import pt.carguru.Repositories.UserRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class UserService {
    private final UserRepository userRepo = new UserRepository();
    private final TransacaoRepository transacaoRepo = new TransacaoRepository();
    private final ReservaRepository reservaRepo = new ReservaRepository();

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

    public void atualizarCarta(String nCarta, LocalDate validadeCarta) throws SQLException {
        User user = Session.getUser();
        if (user == null) throw new IllegalStateException("Não estás autenticado.");
        if (nCarta == null || nCarta.isBlank()) throw new IllegalArgumentException("Número da carta obrigatório.");
        if (validadeCarta == null) throw new IllegalArgumentException("Validade da carta obrigatória.");
        if (validadeCarta.isBefore(LocalDate.now())) throw new IllegalArgumentException("A carta de condução está expirada.");
        user.setNCartaConducao(nCarta.trim());
        user.setValidadeCarta(validadeCarta);
        userRepo.updateCarta(user);
        Session.setUser(user);
    }

    public void depositar(double valor) throws SQLException {
        if (valor <= 0) throw new IllegalArgumentException("Valor inválido.");
        User user = Session.getUser();
        user.setSaldo(user.getSaldo() + valor);
        userRepo.update(user);
        transacaoRepo.registar(user.getId(), "DEPOSITO", valor,
                String.format("Depósito de %.2f€ na conta", valor), user.getSaldo(), null, null);
        Session.setUser(user);
    }

    public void levantar(double valor) throws SQLException {
        if (valor <= 0) throw new IllegalArgumentException("Valor inválido.");
        User user = Session.getUser();
        double caucoesAtivas = reservaRepo.getCaucoesAtivas(user.getId());
        double disponivel = user.getSaldo() - caucoesAtivas;
        if (valor > disponivel)
            throw new IllegalArgumentException(String.format(
                "Saldo disponível insuficiente. Disponível: %.2f€ (Saldo: %.2f€ − Cauções ativas: %.2f€)",
                disponivel, user.getSaldo(), caucoesAtivas));
        user.setSaldo(user.getSaldo() - valor);
        userRepo.update(user);
        transacaoRepo.registar(user.getId(), "LEVANTAMENTO", valor,
                String.format("Levantamento de %.2f€ da conta", valor), user.getSaldo(), null, null);
        Session.setUser(user);
    }

    /** Saldo total menos o valor retido em cauções de reservas ainda ativas (pendentes/aceites). */
    public double getSaldoDisponivel(User user) throws SQLException {
        return user.getSaldo() - reservaRepo.getCaucoesAtivas(user.getId());
    }

    public double getCaucoesAtivas(User user) throws SQLException {
        return reservaRepo.getCaucoesAtivas(user.getId());
    }

    /** Histórico de transações do utilizador atual (mais recentes primeiro). */
    public List<Transacao> listarHistorico(int limit) throws SQLException {
        User user = Session.getUser();
        if (user == null) throw new IllegalStateException("Não estás autenticado.");
        return transacaoRepo.findByUtilizador(user.getId(), limit);
    }

    /** Histórico com filtros opcionais de tipo e período. */
    public List<Transacao> listarHistoricoFiltrado(String tipo, java.time.LocalDate dataInicio, java.time.LocalDate dataFim) throws SQLException {
        User user = Session.getUser();
        if (user == null) throw new IllegalStateException("Não estás autenticado.");
        return transacaoRepo.findByUtilizador(user.getId(), 0, tipo, dataInicio, dataFim);
    }

    public List<User> listarTodos() throws SQLException { return userRepo.findAll(); }

    public void toggleAtivo(int userId) throws SQLException {
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        user.setBloqueado(!user.isBloqueado());
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
        transacaoRepo.registar(user.getId(), "PENALIZACAO", valor,
                "Remoção de saldo por administrador", user.getSaldo(), null, null);
    }

    /** Devolve o utilizador atual com avaliação_media e n_avaliacoes preenchidos. */
    public User getMeuPerfilComAvaliacao() throws SQLException {
        User u = Session.getUser();
        if (u == null) throw new IllegalStateException("Não estás autenticado.");
        double[] av = userRepo.getAvaliacaoMedia(u.getId());
        u.setAvaliacaoMedia(av[0]);
        u.setNAvaliacoes((int) av[1]);
        return u;
    }
}
