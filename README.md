# CarGuru - Plataforma de Aluguer de Veículos entre Particulares

O **CarGuru** é uma aplicação desktop desenvolvida em Java com interface gráfica, baseada no padrão arquitetural **MVC (Model-View-Controller)**. A plataforma funciona em regime *self-service* (sem condutor), permitindo que qualquer utilizador atue simultaneamente como proprietário (anunciando os seus veículos) e como locatário (pesquisando e reservando veículos de terceiros).

---

## Estrutura do Projeto (Arquitetura)

O código fonte está organizado dentro do pacote principal `pt.carguru` para garantir a separação de responsabilidades:

*   **`Controllers`**: Mediação entre as Views e os Services. Controla o fluxo das ações do utilizador (ex: submissão de reservas, login, filtros).
*   **`Models`**: Contém as entidades de dados do sistema (Utilizador, Veículo, Reserva, Transação, Combustível).
*   **`Repositories`**: Camada de persistência. Contém os métodos de CRUD para comunicação direta com a Base de Dados.
*   **`Services`**: Concentra a lógica de negócio pesada, tais como:
    *   Algoritmo de preço dinâmico (+20% em fins de semana, +30% em época alta, -10% para $\ge$ 7 dias).
    *   Algoritmo de custo total estimado com base nos "kms previstos".
    *   Validações de sobreposição de reservas e verificação de saldo/caução.
*   **`Views`**: Interface gráfica (GUI) contendo os painéis de Login, Dashboard, Procurar Veículos, Conta Pessoal e Painel de Administração.
*   **`Utils`**:
    *   `DatabaseConnection`: Gestão centralizada do ciclo de vida da ligação à BD.
    *   `PasswordHasher`: Componente obrigatório de segurança para criptografia das passwords dos utilizadores.

---

## Funcionalidades Principais Implementadas

###  Gestão de Utilizadores & Conta
*   Registo com hashing obrigatório de password, Login, Logout e Recuperação de credenciais.
*   Gestão de carteira digital (saldo em euros, depósitos e levantamentos).
*   Histórico completo de transações e exportação de relatórios em formato CSV.

### Consulta e Aluguer de Veículos
*   Pesquisa avançada com filtros (marca, preço, localização, transmissão, combustível, lotação e datas).
*   **Filtro Inteligente de Kms Previstos**: Ordenação automática dos veículos pelo custo total estimado (Renda Dinâmica + Consumo Estimado).
*   Fluxo de reserva com caução preventiva de 20% e sistema de aprovação/rejeição pelo proprietário em até 24h.
*   Cálculo final do aluguer baseado na quilometragem real percorrida e no preço do combustível corrente.
*   Sistema de avaliação mútua (1 a 5 estrelas + comentário) após a devolução.

### Painel de Administração
*   Moderação e bloqueio de anúncios de veículos.
*   **Gestão de Combustíveis**: Definição do preço base e simulação automatizada de variação aleatória de preços a cada 10 minutos, com registo histórico em BD.
*   Resolução de disputas e gestão/bloqueio de utilizadores abusivos.

---

## Tecnologias Utilizadas

*   **Java**: Linguagem de programação principal.
*   **Java Swing / JavaFX**: Para a construção da Interface Gráfica (GUI).
*   **Maven**: Gestão de dependências (`.mvn`) e automação do build.
*   **Base de Dados**: Persistência do histórico de transações, preços e registos.

---

## Como Executar o Projeto

1. Clone o repositório para a sua máquina local.
2. Abra o **IntelliJ IDEA**.
3. Selecione **Open** e aponte para a pasta raiz `untitled [CarGuru]`.
4. Aguarde o Maven descarregar as dependências automaticamente.
5. Execute a classe principal de inicialização do sistema (geralmente localizada em `pt.carguru` ou no pacote `Controllers`).

---

## Organização de Pastas

```text
untitled [CarGuru]/
│
├── .idea/                 # Configurações do ambiente IntelliJ
├── .mvn/                  # Arquivos e wrappers do Maven
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── pt/
│   │   │       └── carguru/
│   │   │           ├── Controllers/   # Fluxo de navegação e ações
│   │   │           ├── Models/        # Entidades (User, Veiculo, Reserva...)
│   │   │           ├── Repositories/  # Consultas SQL / Persistência
│   │   │           ├── Services/      # Regras de Negócio e Algoritmos
│   │   │           ├── Utils/         # DatabaseConnection e PasswordHasher
│   │   │           └── Views/         # Janelas e Componentes Gráficos
│   └── test/              # Testes unitários do sistema
└── target/                # Binários gerados pelo Maven