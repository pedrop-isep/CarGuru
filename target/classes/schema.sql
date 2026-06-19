-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Tempo de geração: 13-Jun-2026 às 18:38
-- Versão do servidor: 10.4.32-MariaDB
-- versão do PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Banco de dados: `carguru`
--

-- --------------------------------------------------------

--
-- Estrutura da tabela `alugueres`
--

CREATE TABLE `alugueres` (
  `id` int(11) NOT NULL,
  `reserva_id` int(11) NOT NULL,
  `km_inicial` int(11) NOT NULL,
  `km_final` int(11) DEFAULT NULL,
  `data_inicio` datetime NOT NULL DEFAULT current_timestamp(),
  `data_fim` datetime DEFAULT NULL,
  `custo_renda` decimal(10,2) NOT NULL,
  `custo_combustivel` decimal(10,2) DEFAULT NULL COMMENT '((km_final-km_inicial)/100) x consumo x preco_combustivel_fim',
  `custo_total` decimal(10,2) DEFAULT NULL,
  `preco_combustivel_fim` decimal(6,4) DEFAULT NULL COMMENT 'Preco/L ou /kWh no momento do fecho',
  `caucao_devolvida` tinyint(1) NOT NULL DEFAULT 0,
  `incidente` tinyint(1) NOT NULL DEFAULT 0
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `avaliacoes`
--

CREATE TABLE `avaliacoes` (
  `id` int(11) NOT NULL,
  `aluguer_id` int(11) NOT NULL,
  `avaliador_id` int(11) NOT NULL,
  `avaliado_id` int(11) NOT NULL,
  `estrelas` smallint(6) NOT NULL,
  `comentario` text DEFAULT NULL,
  `tipo` enum('PROPRIETARIO','LOCATARIO') NOT NULL,
  `data` datetime NOT NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `disputas`
--

CREATE TABLE `disputas` (
  `id` int(11) NOT NULL,
  `aluguer_id` int(11) NOT NULL,
  `iniciador_id` int(11) NOT NULL,
  `admin_id` int(11) DEFAULT NULL,
  `descricao` text NOT NULL,
  `estado` enum('ABERTA','EM_ANALISE','RESOLVIDA_PROPRIETARIO','RESOLVIDA_LOCATARIO','ENCERRADA') NOT NULL DEFAULT 'ABERTA',
  `resolucao` text DEFAULT NULL,
  `reembolso_forcado` decimal(10,2) DEFAULT NULL,
  `penalizacao` decimal(10,2) DEFAULT NULL,
  `data_criacao` datetime NOT NULL DEFAULT current_timestamp(),
  `data_resolucao` datetime DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `fotos_veiculo`
--

CREATE TABLE `fotos_veiculo` (
  `id` int(11) NOT NULL,
  `veiculo_id` int(11) NOT NULL,
  `url` varchar(500) NOT NULL,
  `ordem` smallint(6) NOT NULL DEFAULT 0,
  `data_criacao` datetime NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fotos dos veículos anunciados.';

-- --------------------------------------------------------

--
-- Estrutura da tabela `historico_precos`
--

CREATE TABLE `historico_precos` (
  `id` int(11) NOT NULL,
  `preco_id` int(11) NOT NULL,
  `tipo_combustivel` enum('GASOLINA','GASOLEO','GPL','ELETRICO') NOT NULL,
  `preco` decimal(6,4) NOT NULL,
  `registado_em` datetime NOT NULL DEFAULT current_timestamp()
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `indisponibilidades`
--

CREATE TABLE `indisponibilidades` (
  `id` int(11) NOT NULL,
  `veiculo_id` int(11) NOT NULL,
  `data_inicio` date NOT NULL,
  `data_fim` date NOT NULL,
  `motivo` varchar(255) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `notificacoes`
--

CREATE TABLE `notificacoes` (
  `id` int(11) NOT NULL,
  `utilizador_id` int(11) NOT NULL,
  `tipo` enum('RESERVA_ACEITE','RESERVA_REJEITADA','RESERVA_EXPIRADA','ALUGUER_INICIO','LEMBRETE_DEVOLUCAO','ALUGUER_CONCLUIDO','DISPUTA_ABERTA','DISPUTA_RESOLVIDA','AVALIACAO_RECEBIDA','SISTEMA') NOT NULL,
  `mensagem` text NOT NULL,
  `lida` tinyint(1) NOT NULL DEFAULT 0,
  `data_envio` datetime NOT NULL DEFAULT current_timestamp(),
  `referencia_id` int(11) DEFAULT NULL,
  `referencia_tipo` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Notificações enviadas aos utilizadores.';

-- --------------------------------------------------------

--
-- Estrutura da tabela `precos_combustivel`
--

CREATE TABLE `precos_combustivel` (
  `id` int(11) NOT NULL,
  `tipo_combustivel` enum('GASOLINA','GASOLEO','GPL','ELETRICO') NOT NULL,
  `preco_base` decimal(6,4) NOT NULL COMMENT 'euros/L ou euros/kWh, definido pelo admin',
  `preco_corrente` decimal(6,4) NOT NULL COMMENT 'Varia a cada 10 minutos por algoritmo',
  `ultima_atualizacao` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `registado_por` int(11) NOT NULL
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `reservas`
--

CREATE TABLE `reservas` (
  `id` int(11) NOT NULL,
  `veiculo_id` int(11) NOT NULL,
  `locatario_id` int(11) NOT NULL,
  `data_inicio` date NOT NULL,
  `data_fim` date NOT NULL,
  `estado` enum('PENDENTE','ACEITE','REJEITADA','CANCELADA','EXPIRADA','CONCLUIDA') NOT NULL DEFAULT 'PENDENTE',
  `preco_dia_dinamico` decimal(8,2) NOT NULL,
  `custo_renda` decimal(10,2) NOT NULL,
  `kms_previstos` int(11) DEFAULT NULL,
  `caucao` decimal(10,2) NOT NULL,
  `data_pedido` datetime NOT NULL DEFAULT current_timestamp(),
  `data_expiracao` datetime NOT NULL COMMENT 'data_pedido + 24h',
  `data_aceitacao` datetime DEFAULT NULL,
  `data_cancelamento` datetime DEFAULT NULL,
  `motivo_cancelamento` varchar(500) DEFAULT NULL,
  `km_inicial` int(11) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `transacoes`
--

CREATE TABLE `transacoes` (
  `id` int(11) NOT NULL,
  `utilizador_id` int(11) NOT NULL,
  `tipo` enum('DEPOSITO','LEVANTAMENTO','PAGAMENTO_ALUGUER','RECEITA_ALUGUER','CAUCAO_RETIDA','CAUCAO_DEVOLVIDA','REEMBOLSO','PENALIZACAO') NOT NULL,
  `valor` decimal(12,2) NOT NULL,
  `descricao` varchar(500) NOT NULL,
  `data` datetime NOT NULL DEFAULT current_timestamp(),
  `saldo_apos` decimal(12,2) NOT NULL,
  `referencia_id` int(11) DEFAULT NULL COMMENT 'ID da reserva ou aluguer relacionado',
  `referencia_tipo` varchar(50) DEFAULT NULL COMMENT 'reserva | aluguer'
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `utilizadores`
--

CREATE TABLE `utilizadores` (
  `id` int(11) NOT NULL,
  `email` varchar(255) NOT NULL,
  `nome` varchar(150) NOT NULL,
  `nif` char(9) NOT NULL,
  `n_carta_conducao` varchar(20) NOT NULL,
  `validade_carta` date NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `saldo` decimal(12,2) NOT NULL DEFAULT 0.00,
  `role` enum('UTILIZADOR','ADMINISTRADOR') NOT NULL DEFAULT 'UTILIZADOR',
  `bloqueado` tinyint(1) NOT NULL DEFAULT 0,
  `data_registo` datetime NOT NULL DEFAULT current_timestamp(),
  `token_recuperacao` varchar(255) DEFAULT NULL,
  `token_expira_em` datetime DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Estrutura da tabela `veiculos`
--

CREATE TABLE `veiculos` (
  `id` int(11) NOT NULL,
  `proprietario_id` int(11) NOT NULL,
  `matricula` varchar(10) NOT NULL,
  `marca` varchar(80) NOT NULL,
  `modelo` varchar(80) NOT NULL,
  `ano` smallint(6) NOT NULL,
  `tipo_combustivel` enum('GASOLINA','GASOLEO','GPL','ELETRICO') NOT NULL,
  `consumo_medio` decimal(5,2) NOT NULL COMMENT 'L/100km ou kWh/100km',
  `tipo_transmissao` enum('MANUAL','AUTOMATICA') NOT NULL,
  `lotacao` smallint(6) NOT NULL,
  `quilometragem` int(11) NOT NULL,
  `preco_dia_base` decimal(8,2) NOT NULL,
  `cidade` varchar(100) NOT NULL,
  `distrito` varchar(80) NOT NULL,
  `codigo_postal` varchar(10) DEFAULT NULL,
  `estado` enum('PENDENTE_VALIDACAO','DISPONIVEL','BLOQUEADO','REMOVIDO') NOT NULL DEFAULT 'PENDENTE_VALIDACAO',
  `avaliacao_media` decimal(3,2) NOT NULL DEFAULT 0.00,
  `n_avaliacoes` int(11) NOT NULL DEFAULT 0,
  `validado` tinyint(1) NOT NULL DEFAULT 0,
  `motivo_rejeicao` text DEFAULT NULL COMMENT 'Motivo de rejeição preenchido pelo administrador',
  `data_criacao` datetime NOT NULL DEFAULT current_timestamp(),
  `data_atualizacao` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

--
-- Índices para tabelas despejadas
--

--
-- Índices para tabela `alugueres`
--
ALTER TABLE `alugueres`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_alugueres_reserva` (`reserva_id`),
  ADD KEY `idx_alugueres_reserva` (`reserva_id`);

--
-- Índices para tabela `avaliacoes`
--
ALTER TABLE `avaliacoes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_avaliacoes_aluguer_tipo` (`aluguer_id`,`tipo`),
  ADD KEY `fk_avaliacoes_avaliador` (`avaliador_id`),
  ADD KEY `idx_avaliacoes_avaliado` (`avaliado_id`);

--
-- Índices para tabela `disputas`
--
ALTER TABLE `disputas`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_disputas_aluguer` (`aluguer_id`),
  ADD KEY `fk_disputas_iniciador` (`iniciador_id`),
  ADD KEY `fk_disputas_admin` (`admin_id`);

--
-- Índices para tabela `fotos_veiculo`
--
ALTER TABLE `fotos_veiculo`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_fotos_veiculo_ordem` (`veiculo_id`,`ordem`),
  ADD KEY `idx_fotos_veiculo` (`veiculo_id`,`ordem`);

--
-- Índices para tabela `historico_precos`
--
ALTER TABLE `historico_precos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_historico_preco` (`preco_id`),
  ADD KEY `idx_historico_precos_tipo_data` (`tipo_combustivel`,`registado_em`);

--
-- Índices para tabela `indisponibilidades`
--
ALTER TABLE `indisponibilidades`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_indisponibilidades_v` (`veiculo_id`,`data_inicio`,`data_fim`);

--
-- Índices para tabela `notificacoes`
--
ALTER TABLE `notificacoes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_notificacoes_utilizador` (`utilizador_id`,`lida`,`data_envio`);

--
-- Índices para tabela `precos_combustivel`
--
ALTER TABLE `precos_combustivel`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_precos_tipo` (`tipo_combustivel`),
  ADD KEY `fk_precos_registado_por` (`registado_por`);

--
-- Índices para tabela `reservas`
--
ALTER TABLE `reservas`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_reservas_locatario` (`locatario_id`),
  ADD KEY `idx_reservas_estado` (`estado`),
  ADD KEY `idx_reservas_veiculo_datas` (`veiculo_id`,`data_inicio`,`data_fim`);

--
-- Índices para tabela `transacoes`
--
ALTER TABLE `transacoes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_transacoes_utilizador` (`utilizador_id`,`data`);

--
-- Índices para tabela `utilizadores`
--
ALTER TABLE `utilizadores`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_utilizadores_email` (`email`),
  ADD UNIQUE KEY `uq_utilizadores_nif` (`nif`),
  ADD UNIQUE KEY `uq_utilizadores_carta` (`n_carta_conducao`);

--
-- Índices para tabela `veiculos`
--
ALTER TABLE `veiculos`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_veiculos_matricula` (`matricula`),
  ADD KEY `idx_veiculos_proprietario` (`proprietario_id`),
  ADD KEY `idx_veiculos_estado_cidade` (`estado`,`cidade`),
  ADD KEY `idx_veiculos_combustivel` (`tipo_combustivel`);

--
-- AUTO_INCREMENT de tabelas despejadas
--

--
-- AUTO_INCREMENT de tabela `alugueres`
--
ALTER TABLE `alugueres`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `avaliacoes`
--
ALTER TABLE `avaliacoes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `disputas`
--
ALTER TABLE `disputas`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `fotos_veiculo`
--
ALTER TABLE `fotos_veiculo`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `historico_precos`
--
ALTER TABLE `historico_precos`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `indisponibilidades`
--
ALTER TABLE `indisponibilidades`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `notificacoes`
--
ALTER TABLE `notificacoes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `precos_combustivel`
--
ALTER TABLE `precos_combustivel`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `reservas`
--
ALTER TABLE `reservas`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `transacoes`
--
ALTER TABLE `transacoes`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `utilizadores`
--
ALTER TABLE `utilizadores`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de tabela `veiculos`
--
ALTER TABLE `veiculos`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Restrições para despejos de tabelas
--

--
-- Limitadores para a tabela `alugueres`
--
ALTER TABLE `alugueres`
  ADD CONSTRAINT `fk_alugueres_reserva` FOREIGN KEY (`reserva_id`) REFERENCES `reservas` (`id`);

--
-- Limitadores para a tabela `avaliacoes`
--
ALTER TABLE `avaliacoes`
  ADD CONSTRAINT `fk_avaliacoes_aluguer` FOREIGN KEY (`aluguer_id`) REFERENCES `alugueres` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_avaliacoes_avaliado` FOREIGN KEY (`avaliado_id`) REFERENCES `utilizadores` (`id`),
  ADD CONSTRAINT `fk_avaliacoes_avaliador` FOREIGN KEY (`avaliador_id`) REFERENCES `utilizadores` (`id`);

--
-- Limitadores para a tabela `disputas`
--
ALTER TABLE `disputas`
  ADD CONSTRAINT `fk_disputas_admin` FOREIGN KEY (`admin_id`) REFERENCES `utilizadores` (`id`),
  ADD CONSTRAINT `fk_disputas_aluguer` FOREIGN KEY (`aluguer_id`) REFERENCES `alugueres` (`id`),
  ADD CONSTRAINT `fk_disputas_iniciador` FOREIGN KEY (`iniciador_id`) REFERENCES `utilizadores` (`id`);

--
-- Limitadores para a tabela `fotos_veiculo`
--
ALTER TABLE `fotos_veiculo`
  ADD CONSTRAINT `fk_fotos_veiculo` FOREIGN KEY (`veiculo_id`) REFERENCES `veiculos` (`id`) ON DELETE CASCADE;

--
-- Limitadores para a tabela `historico_precos`
--
ALTER TABLE `historico_precos`
  ADD CONSTRAINT `fk_historico_preco` FOREIGN KEY (`preco_id`) REFERENCES `precos_combustivel` (`id`);

--
-- Limitadores para a tabela `indisponibilidades`
--
ALTER TABLE `indisponibilidades`
  ADD CONSTRAINT `fk_indisponibilidades_veiculo` FOREIGN KEY (`veiculo_id`) REFERENCES `veiculos` (`id`) ON DELETE CASCADE;

--
-- Limitadores para a tabela `notificacoes`
--
ALTER TABLE `notificacoes`
  ADD CONSTRAINT `fk_notificacoes_utilizador` FOREIGN KEY (`utilizador_id`) REFERENCES `utilizadores` (`id`) ON DELETE CASCADE;

--
-- Limitadores para a tabela `precos_combustivel`
--
ALTER TABLE `precos_combustivel`
  ADD CONSTRAINT `fk_precos_registado_por` FOREIGN KEY (`registado_por`) REFERENCES `utilizadores` (`id`);

--
-- Limitadores para a tabela `reservas`
--
ALTER TABLE `reservas`
  ADD CONSTRAINT `fk_reservas_locatario` FOREIGN KEY (`locatario_id`) REFERENCES `utilizadores` (`id`),
  ADD CONSTRAINT `fk_reservas_veiculo` FOREIGN KEY (`veiculo_id`) REFERENCES `veiculos` (`id`);

--
-- Limitadores para a tabela `transacoes`
--
ALTER TABLE `transacoes`
  ADD CONSTRAINT `fk_transacoes_utilizador` FOREIGN KEY (`utilizador_id`) REFERENCES `utilizadores` (`id`);

--
-- Limitadores para a tabela `veiculos`
--
ALTER TABLE `veiculos`
  ADD CONSTRAINT `fk_veiculos_proprietario` FOREIGN KEY (`proprietario_id`) REFERENCES `utilizadores` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;


-- ─────────────────────────────────────────────────────────────────────────────
-- Migração: suporte a motivo de rejeição de veículos (User Story: validação admin)
-- Executar uma única vez numa BD existente:
--   ALTER TABLE `veiculos` ADD COLUMN `motivo_rejeicao` TEXT DEFAULT NULL
--     AFTER `validado`;
-- Em instalações novas o CREATE TABLE abaixo já inclui a coluna.
--
-- Notificação por email ao proprietário: NÃO requer coluna nova em `veiculos`.
-- O email do proprietário é obtido via JOIN à tabela `utilizadores` (coluna
-- `email` já existente), como `proprietario_email`, em todas as queries de
-- VeiculoRepository (findById, findByProprietario, findAprovados,
-- findDisponiveisPorDatas, findPendentes, findAll).
-- ─────────────────────────────────────────────────────────────────────────────
