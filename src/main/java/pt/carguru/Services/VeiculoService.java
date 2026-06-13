package pt.carguru.Services;

import pt.carguru.Models.Indisponibilidade;
import pt.carguru.Models.Veiculo;
import pt.carguru.Repositories.IndisponibilidadeRepository;
import pt.carguru.Repositories.VeiculoRepository;
import pt.carguru.Utils.Session;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class VeiculoService {
    private final VeiculoRepository veiculoRepo = new VeiculoRepository();
    private final IndisponibilidadeRepository indispRepo = new IndisponibilidadeRepository();

    /** CAR-9: Adicionar veículo */
    public Veiculo adicionarVeiculo(String marca, String modelo, int ano, String combustivel,
                                    String transmissao, String localizacao, double preco, double consumo, String descricao) throws SQLException {
        validarCamposVeiculo(marca, modelo, ano, localizacao, preco);
        Veiculo v = new Veiculo(0, Session.getUser().getId(), marca, modelo, ano,
                combustivel, transmissao, localizacao, preco, consumo, descricao, "pendente");
        return veiculoRepo.save(v);
    }

    /** CAR-13: Editar veículo */
    public void editarVeiculo(int id, String marca, String modelo, int ano, String combustivel,
                               String transmissao, String localizacao, double preco, double consumo, String descricao) throws SQLException {
        Veiculo v = veiculoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        if (v.getProprietarioId() != Session.getUser().getId()) throw new IllegalStateException("Sem permissão.");
        validarCamposVeiculo(marca, modelo, ano, localizacao, preco);
        v.setMarca(marca); v.setModelo(modelo); v.setAno(ano); v.setCombustivel(combustivel);
        v.setTransmissao(transmissao); v.setLocalizacao(localizacao);
        v.setPrecoPorDia(preco); v.setConsumo(consumo); v.setDescricao(descricao);
        veiculoRepo.update(v);
    }

    /** CAR-15: Remover veículo */
    public void removerVeiculo(int id) throws SQLException {
        Veiculo v = veiculoRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        if (v.getProprietarioId() != Session.getUser().getId()) throw new IllegalStateException("Sem permissão.");
        veiculoRepo.delete(id);
    }

    /** CAR-16: Definir período de indisponibilidade */
    public void adicionarIndisponibilidade(int veiculoId, LocalDate inicio, LocalDate fim) throws SQLException {
        if (inicio == null || fim == null || !fim.isAfter(inicio))
            throw new IllegalArgumentException("Período inválido.");
        Veiculo v = veiculoRepo.findById(veiculoId).orElseThrow(() -> new IllegalArgumentException("Veículo não encontrado."));
        if (v.getProprietarioId() != Session.getUser().getId()) throw new IllegalStateException("Sem permissão.");
        indispRepo.save(new Indisponibilidade(veiculoId, inicio, fim));
    }

    public void removerIndisponibilidade(int id) throws SQLException {
        indispRepo.delete(id);
    }

    public List<Indisponibilidade> listarIndisponibilidades(int veiculoId) throws SQLException {
        return indispRepo.findByVeiculo(veiculoId);
    }

    /** CAR-20: Listagem de veículos disponíveis com filtros */
    public List<Veiculo> pesquisarVeiculos(String marca, String combustivel, String transmissao,
                                            String localizacao, double precoMax) throws SQLException {
        return veiculoRepo.findAprovados(marca, combustivel, transmissao, localizacao, precoMax);
    }

    public List<Veiculo> listarMeusVeiculos() throws SQLException {
        return veiculoRepo.findByProprietario(Session.getUser().getId());
    }

    public List<Veiculo> listarPendentes() throws SQLException {
        return veiculoRepo.findPendentes();
    }

    public void aprovarVeiculo(int id) throws SQLException { veiculoRepo.updateEstado(id, "aprovado"); }
    public void rejeitarVeiculo(int id) throws SQLException { veiculoRepo.updateEstado(id, "rejeitado"); }

    private void validarCamposVeiculo(String marca, String modelo, int ano, String localizacao, double preco) {
        if (marca == null || marca.isBlank()) throw new IllegalArgumentException("Marca obrigatória.");
        if (modelo == null || modelo.isBlank()) throw new IllegalArgumentException("Modelo obrigatório.");
        if (ano < 1900 || ano > LocalDate.now().getYear() + 1) throw new IllegalArgumentException("Ano inválido.");
        if (localizacao == null || localizacao.isBlank()) throw new IllegalArgumentException("Localização obrigatória.");
        if (preco <= 0) throw new IllegalArgumentException("Preço deve ser positivo.");
    }
}
