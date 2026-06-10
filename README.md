# 🚗 Plataforma de Aluguer de Veículos entre Particulares

Aplicação desktop desenvolvida em **Java** com interface gráfica (GUI) para gestão de aluguer de veículos entre particulares em regime **self-service** (sem condutor). Qualquer utilizador pode anunciar os seus próprios veículos para aluguer, bem como pesquisar, reservar e alugar veículos de outros utilizadores.

> Trabalho Prático 2 — LP2

---

## 📋 Índice

- [Funcionalidades](#-funcionalidades)
- [Requisitos](#-requisitos)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Como Executar](#-como-executar)
- [Algoritmos Implementados](#-algoritmos-implementados)
- [Base de Dados](#-base-de-dados)
- [Extras Implementados](#-extras-implementados)

---

## ✨ Funcionalidades

### 👤 Gestão de Utilizadores
- Registo com email, nome, NIF, carta de condução e password (com hashing)
- Login / Logout
- Recuperação de password
- Cada utilizador pode ser simultaneamente **proprietário** e **locatário**

### 🔍 Consulta de Veículos
- Listagem de veículos disponíveis com marca, modelo, ano, combustível, consumo, preço/dia, localização e avaliação média
- Consulta detalhada: fotos, especificações técnicas, avaliações e histórico de alugueres
- Filtros por marca, preço, localização, transmissão, combustível, lotação e datas
- **Filtro "kms previstos"**: ordena por custo total estimado (renda + combustível)
- Preço dinâmico: +20% fins de semana, +30% época alta, -10% para ≥ 7 dias

### 💼 Conta Pessoal
- Saldo em conta com depósito e levantamento de fundos
- Gestão de veículos próprios (adicionar, editar, remover)
- Calendário de indisponibilidade por veículo
- Histórico de alugueres como locatário e como proprietário
- Histórico completo de transações

### 📅 Reservas e Alugueres
- Pedido de reserva com datas de início e fim
- Proprietário tem **24h** para aceitar ou rejeitar; expiração automática
- Sem sobreposição de reservas confirmadas para o mesmo veículo
- **Caução de 20%** sobre o custo total estimado (devolvida no fim sem incidentes)
- Custo final = renda + combustível efetivo (com base em km inicial/final)
- Cancelamento até **48h antes** do início com reembolso parcial
- Avaliação mútua (1-5 estrelas + comentário) após devolução

### 🖥️ Interface Gráfica
- Painéis distintos: Login, Dashboard, Procurar Veículos, Conta, As Minhas Reservas
- Mensagens de erro e confirmação amigáveis
- Feedback visual (loadings, confirmações, etc.)

### 🔧 Administração
- Dois perfis: administradores e utilizadores
- Validação e bloqueio de anúncios de veículos
- Gestão de preços de combustível por tipo (gasolina, gasóleo, GPL, elétrico)
- Variação aleatória automática do preço a cada 10 minutos
- Registo do preço corrente em BD hora a hora
- Resolução de disputas entre proprietários e locatários
- Histórico de alugueres com filtragem por período
- Bloqueio de utilizadores com comportamento abusivo

---

## 🛠️ Requisitos

- **Java** 17 ou superior
- **Maven** ou **Gradle** (conforme configuração do projeto)
- Base de dados relacional (ex: SQLite, PostgreSQL ou MySQL)
- IDE recomendada: IntelliJ IDEA ou Eclipse

---

## 📁 Estrutura do Projeto

```
src/
├── main/
│   ├── java/
│   │   ├── model/          # Entidades (Utilizador, Veiculo, Reserva, ...)
│   │   ├── dao/            # Acesso à base de dados
│   │   ├── service/        # Lógica de negócio
│   │   ├── ui/             # Painéis e componentes gráficos (Swing/JavaFX)
│   │   └── util/           # Utilitários (hashing, datas, preços, ...)
│   └── resources/
│       └── db/             # Scripts SQL de inicialização
└── test/                   # Testes unitários
```

---

## ▶️ Como Executar

1. Clona o repositório:
   ```bash
   git clone https://github.com/<utilizador>/<repositorio>.git
   cd <repositorio>
   ```

2. Compila o projeto:
   ```bash
   mvn clean install
   # ou
   gradle build
   ```

3. Inicializa a base de dados executando os scripts em `src/main/resources/db/`.

4. Executa a aplicação:
   ```bash
   mvn exec:java -Dexec.mainClass="Main"
   # ou
   java -jar target/<nome>.jar
   ```

---

## ⚙️ Algoritmos Implementados

### Preço Dinâmico
```
preço_efetivo = preço_base_dia
    × (1.20 se fim de semana)
    × (1.30 se época alta: Julho, Agosto, Natal)
    × (0.90 se duração ≥ 7 dias)
```

### Custo Total Estimado (com kms previstos)
```
custo_total = (n_dias × preço_dia_dinâmico)
            + (kms_previstos / 100 × consumo × preço_combustível_corrente)
```

### Custo Final do Aluguer
```
renda      = n_dias × preço/dia dinâmico
combustível = ((km_final − km_inicial) / 100) × consumo × preço_combustível_no_fim
custo_total = renda + combustível
```

### Caução
```
caução = 20% × (renda + combustível_estimado)
# combustível estimado usa 200 km/dia por defeito se não houver histórico
```

### Variação do Preço de Combustível
O preço corrente varia automaticamente a cada **10 minutos** com base num algoritmo de variação aleatória em torno do preço base definido pelo administrador. O preço é registado na base de dados **hora a hora**.

---

## 🗄️ Base de Dados

Principais tabelas:

| Tabela | Descrição |
|---|---|
| `utilizadores` | Dados dos utilizadores registados |
| `veiculos` | Anúncios de veículos disponíveis |
| `reservas` | Pedidos e alugueres (com estado) |
| `transacoes` | Movimentos de saldo em conta |
| `avaliacoes` | Avaliações entre locatário e proprietário |
| `precos_combustivel` | Histórico de preços por tipo de combustível |
| `indisponibilidades` | Calendário de indisponibilidade por veículo |

---

## ⭐ Extras Implementados

- **Notificações por e-mail**: alerta de reserva aceite, lembrete de início e de devolução
- **Exportação CSV**: histórico de alugueres exportável
- **Dashboard estatístico**: gráficos de rendimento mensal por veículo e distribuição geográfica das reservas

---

## 👥 Autores

| Nome | Número |
|---|---|
| ... | ... |
| ... | ... |

---

## 📄 Licença

Este projeto foi desenvolvido para fins académicos no âmbito da unidade curricular **LP2**.
