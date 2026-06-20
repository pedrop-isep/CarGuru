package pt.carguru.Services;

import pt.carguru.Models.RendimentoMensalVeiculo;
import pt.carguru.Models.ReservasPorLocalizacao;
import pt.carguru.Models.Veiculo;
import pt.carguru.Repositories.ReservaRepository;
import pt.carguru.Repositories.VeiculoRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Estatísticas globais da plataforma para o painel de administração
 * (gráfico de rendimento mensal por veículo e distribuição geográfica das reservas).
 * Apenas acessível a administradores.
 */
public class EstatisticasService {
    private final ReservaRepository reservaRepo = new ReservaRepository();
    private final VeiculoRepository veiculoRepo = new VeiculoRepository();

    /**
     * Rendimento mensal por veículo (apenas alugueres concluídos), com filtros
     * opcionais de período e veículo. Passa null em qualquer parâmetro para não
     * aplicar esse filtro.
     */
    public List<RendimentoMensalVeiculo> getRendimentoMensalPorVeiculo(LocalDate de, LocalDate ate, Integer veiculoId) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return reservaRepo.getRendimentoMensalPorVeiculo(de, ate, veiculoId);
    }

    /**
     * Distribuição geográfica das reservas (contagem por cidade do veículo),
     * com filtros opcionais de período e veículo.
     */
    public List<ReservasPorLocalizacao> getReservasPorLocalizacao(LocalDate de, LocalDate ate, Integer veiculoId) throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return reservaRepo.getReservasPorLocalizacao(de, ate, veiculoId);
    }

    /** Lista de veículos para popular o filtro de veículo do painel de estatísticas. */
    public List<Veiculo> listarVeiculosParaFiltro() throws SQLException {
        if (!Session.isAdmin()) throw new IllegalStateException("Sem permissão.");
        return veiculoRepo.findAll();
    }
}
