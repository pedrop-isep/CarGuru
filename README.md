# 🚗 CarGuru — JavaFX Desktop App

Plataforma de aluguer peer-to-peer de veículos entre particulares.

---

## 🎨 Tema

Dark theme com paleta **preto + vermelho**:
- Fundo `#0d0d0d`, cards `#141414`, inputs `#1e1e1e`
- Accent vermelho `#dc2626` / `#ef4444`
- Tipografia `Segoe UI` / sans-serif

---

## 🗃️ Base de Dados — Setup

1. Instalar **MySQL 8+** ou **MariaDB 10.4+**
2. Criar a base de dados e executar o schema:
```sql
CREATE DATABASE carguru CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
mysql -u root -p carguru < src/main/resources/schema.sql
```
3. Confirmar credenciais em `DatabaseConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/carguru?...";
private static final String USER = "root";
private static final String PASSWORD = "";
```

### Conta Admin pré-existente (criar manualmente ou via script):
| Email | Password | Role |
|-------|----------|------|
| admin@carguru.pt | admin123 | ADMINISTRADOR |

---

## ▶️ Executar

```bash
mvn javafx:run
```

---

## ✅ Funcionalidades

### Correções desta versão
| # | Correção |
|---|---------|
| 1 | Admin não pode suspender a si próprio |
| 2 | Tamanho da janela mantido ao trocar de página |
| 3 | Botão Admin escondido para não-admins |
| 4 | Homepage pública criada |
| 5 | Barra de navegação completa em todas as páginas |
| 6 | Delete é sempre soft (estado = REMOVIDO) |
| 7 | Perfil: foto de perfil (adicionar e trocar) |
| 8 | Veículos: card com nome/foto + modal de detalhes/reserva |
| 9 | Admin pode remover veículos de qualquer utilizador |
| 10 | Confirmação para depositar/levantar saldo |
| 11 | BD adaptada para carguru.sql |
| 12 | Paleta mudada de laranja para vermelho |

### Sprint 1
- Login, Registo, Recuperação de password
- Perfil pessoal com foto de perfil
- Adicionar / Editar / Remover (soft) veículos
- Períodos de indisponibilidade

### Sprint 2
- Pesquisa e filtros de veículos
- Detalhe do veículo em modal
- Pedido, Aprovação, Cancelamento de reservas
- Registo Km inicial e final + liquidação
- Avaliações pós-aluguer
- Painel admin: validar veículos, gerir utilizadores, histórico
