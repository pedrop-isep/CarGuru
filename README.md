# 🚗 CarGuru — JavaFX Desktop App

Plataforma de aluguer peer-to-peer de veículos entre particulares.
Migração do frontend Sprint 3 (HTML/CSS/JS) para JavaFX com backend Java completo.

---

## 📁 Estrutura do Projeto

```
src/main/
├── java/pt/carguru/
│   ├── App.java                        ← Entry point (JavaFX Application)
│   ├── Models/
│   │   ├── User.java                   ← Utilizador (id, nome, email, role, saldo…)
│   │   ├── Veiculo.java                ← Veículo (marca, modelo, preco, estado…)
│   │   ├── Reserva.java                ← Reserva (datas, total, km, avaliação…)
│   │   └── Indisponibilidade.java      ← Períodos de indisponibilidade
│   ├── Repositories/
│   │   ├── UserRepository.java         ← CRUD utilizadores
│   │   ├── VeiculoRepository.java      ← CRUD + filtros de pesquisa
│   │   ├── ReservaRepository.java      ← CRUD + validação sobreposição
│   │   └── IndisponibilidadeRepository.java
│   ├── Services/
│   │   ├── AuthService.java            ← Login, Registo, Recuperação password
│   │   ├── UserService.java            ← Perfil, Saldo, Admin gestão users
│   │   ├── VeiculoService.java         ← Adicionar/Editar/Remover/Aprovar veículos
│   │   └── ReservaService.java         ← Criar/Aprovar/Cancelar/Km/Liquidar/Avaliar
│   ├── Controllers/
│   │   ├── AuthController.java         ← Login + Registo + Recuperar Password
│   │   ├── DashboardController.java    ← Vista principal após login
│   │   ├── VehiclesController.java     ← Pesquisa e reserva de veículos
│   │   ├── ContaController.java        ← Perfil + Saldo + Gestão de veículos próprios
│   │   ├── ReservasController.java     ← Reservas como locatário e proprietário
│   │   └── AdminController.java        ← Painel admin (veículos, utilizadores, histórico)
│   └── Utils/
│       ├── DatabaseConnection.java     ← Singleton JDBC MySQL
│       ├── PasswordHasher.java         ← BCrypt hash/verify
│       └── Session.java                ← Utilizador autenticado em memória
└── resources/pt/carguru/
    ├── Views/
    │   ├── LoginView.fxml
    │   ├── DashboardView.fxml
    │   ├── VehiclesView.fxml
    │   ├── ContaView.fxml
    │   ├── ReservasView.fxml
    │   └── AdminView.fxml
    ├── css/style.css                   ← Dark theme inspirado no Sprint 3
    └── schema.sql                      ← Script criação BD MySQL
```

---

## 🗃️ Base de Dados — Setup

1. Instalar **MySQL 8+**
2. Executar o script SQL:
```sql
mysql -u root -p < src/main/resources/schema.sql
```
3. Confirmar em `DatabaseConnection.java` as credenciais:
```java
private static final String URL      = "jdbc:mysql://localhost:3306/carguru";
private static final String USER     = "root";
private static final String PASSWORD = "";
```

### Conta Admin pré-criada
| Campo    | Valor              |
|----------|--------------------|
| Email    | admin@carguru.pt   |
| Password | admin123           |
| Role     | admin              |

---

## ▶️ Executar

### Via Maven (recomendado)
```bash
mvn javafx:run
```

### Compilar JAR
```bash
mvn package
java -jar target/carguru-1.0-SNAPSHOT.jar
```

> **Nota**: É necessário Java 17+ com JavaFX 17. Se o JDK não incluir JavaFX, o plugin Maven trata disso automaticamente.

---

## ✅ Funcionalidades implementadas (Sprint 1 + 2)

### Sprint 1 — Base da plataforma
| ID      | Funcionalidade                          | Implementado em           |
|---------|-----------------------------------------|---------------------------|
| CAR-11  | Login                                   | AuthService + AuthController |
| CAR-6   | Registo de utilizador                   | AuthService + AuthController |
| CAR-17  | Logout                                  | Session.clear() em todas as vistas |
| CAR-23  | Recuperação de password                 | AuthService + AuthController |
| CAR-44  | Perfil dual (proprietário/locatário)    | UserService + ContaController |
| CAR-28  | Gestão de perfil pessoal                | UserService + ContaController |
| CAR-9   | Adicionar veículo                       | VeiculoService + ContaController |
| CAR-13  | Editar veículo                          | VeiculoService + ContaController |
| CAR-15  | Remover veículo                         | VeiculoService + ContaController |
| CAR-16  | Definir período de indisponibilidade    | VeiculoService + ContaController |

### Sprint 2 — Pesquisa e Reservas
| ID      | Funcionalidade                          | Implementado em           |
|---------|-----------------------------------------|---------------------------|
| CAR-20  | Listagem de veículos disponíveis        | VeiculoService + VehiclesController |
| CAR-26  | Detalhe do veículo                      | Modal em VehiclesController |
| CAR-22  | Filtros de pesquisa avançados           | VeiculoRepository.findAprovados() |
| CAR-24  | Filtro de custo total estimado          | Cálculo no modal de reserva |
| CAR-8   | Pedido de reserva                       | ReservaService.criarReserva() |
| CAR-10  | Aprovação de reserva pelo proprietário  | ReservaService.aprovarReserva() |
| CAR-12  | Validação de sobreposição de reservas   | ReservaRepository.existeSobreposicao() |
| CAR-21  | Cancelamento de reserva                 | ReservaService.cancelarReserva() |
| CAR-14  | Registo de quilometragem inicial        | ReservaService.registarKmInicial() |
| CAR-18  | Registo de km final e liquidação        | ReservaService.registarKmFinalELiquidar() |

---

## 🎨 Design

Dark theme inspirado no Sprint 3 HTML/CSS:
- Fundo `#0d0d0d`, cards `#161616`, inputs `#242424`
- Accent laranja `#ff6a00`
- Tipografia `Segoe UI` / sans-serif
- Estilo definido em `src/main/resources/pt/carguru/css/style.css`
