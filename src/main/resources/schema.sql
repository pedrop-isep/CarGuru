-- =============================================
--  CarGuru — Schema da Base de Dados MySQL
-- =============================================

CREATE DATABASE IF NOT EXISTS carguru
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE carguru;

-- ---- Utilizadores ----
CREATE TABLE IF NOT EXISTS utilizadores (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(150)        NOT NULL,
    email         VARCHAR(255)        NOT NULL UNIQUE,
    password_hash VARCHAR(255)        NOT NULL,
    nif           CHAR(9),
    role          ENUM('utilizador','admin') DEFAULT 'utilizador',
    saldo         DECIMAL(10,2)       DEFAULT 0.00,
    ativo         BOOLEAN             DEFAULT TRUE,
    criado_em     TIMESTAMP           DEFAULT CURRENT_TIMESTAMP
);

-- Admin de teste (password: admin123)
INSERT IGNORE INTO utilizadores (nome, email, password_hash, nif, role, saldo, ativo)
VALUES ('Administrador', 'admin@carguru.pt',
        '$2a$12$Lg8F0fHOK3zqr8DhBkQkPelUxEMgdJeomWQkYB4JXO5gWijXSiSri',
        '000000000', 'admin', 1000.00, TRUE);

-- ---- Veículos ----
CREATE TABLE IF NOT EXISTS veiculos (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    proprietario_id INT             NOT NULL,
    marca           VARCHAR(100)    NOT NULL,
    modelo          VARCHAR(100)    NOT NULL,
    ano             YEAR            NOT NULL,
    combustivel     ENUM('Gasolina','Gasóleo','Elétrico','GPL','Híbrido') DEFAULT 'Gasolina',
    transmissao     ENUM('Manual','Automática') DEFAULT 'Manual',
    localizacao     VARCHAR(200)    NOT NULL,
    preco_por_dia   DECIMAL(8,2)    NOT NULL,
    consumo         DECIMAL(5,2)    DEFAULT 6.00,
    descricao       TEXT,
    estado          ENUM('pendente','aprovado','rejeitado') DEFAULT 'pendente',
    data_criacao    DATE,
    FOREIGN KEY (proprietario_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- ---- Reservas ----
CREATE TABLE IF NOT EXISTS reservas (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    veiculo_id          INT             NOT NULL,
    locatario_id        INT             NOT NULL,
    proprietario_id     INT             NOT NULL,
    data_inicio         DATE            NOT NULL,
    data_fim            DATE            NOT NULL,
    total               DECIMAL(10,2)   NOT NULL,
    estado              ENUM('pendente','confirmada','cancelada','concluida') DEFAULT 'pendente',
    km_inicial          INT,
    km_final            INT,
    custo_combustivel   DECIMAL(10,2),
    avaliacao           TINYINT CHECK (avaliacao BETWEEN 1 AND 5),
    comentario_avaliacao TEXT,
    criado_em           TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (veiculo_id)      REFERENCES veiculos(id)     ON DELETE CASCADE,
    FOREIGN KEY (locatario_id)    REFERENCES utilizadores(id) ON DELETE CASCADE,
    FOREIGN KEY (proprietario_id) REFERENCES utilizadores(id) ON DELETE CASCADE
);

-- ---- Indisponibilidades ----
CREATE TABLE IF NOT EXISTS indisponibilidades (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    veiculo_id  INT     NOT NULL,
    inicio      DATE    NOT NULL,
    fim         DATE    NOT NULL,
    FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE CASCADE
);
