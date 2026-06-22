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
2. Criar a base de dados e executar o schema (PowerShell):
```powershell
mysql -u root -p -e "CREATE DATABASE carguru CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Get-Content src/main/resources/schema.sql | mysql -u root -p carguru
```
3. Confirmar credenciais em `DatabaseConnection.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/carguru?...";
private static final String USER = "root";
private static final String PASSWORD = "";
```

### Conta Admin
A conta de administrador é criada automaticamente ao correr o `schema.sql` (não é necessário nenhum passo manual):

| Email | Password | Role |
|-------|----------|------|
| admin@carguru.pt | admin123 | ADMINISTRADOR |

---

## ▶️ Executar

```bash
mvn javafx:run
```

---

## 📋 Sobre o Projeto

Trabalho desenvolvido no âmbito da unidade curricular de **LP2**, cujo objetivo é construir uma
aplicação desktop em Java, com interface gráfica, para gestão de aluguer de veículos entre
particulares em regime *self-service* (sem condutor). Qualquer utilizador pode anunciar os seus
próprios veículos para aluguer e, simultaneamente, pesquisar, reservar e alugar veículos de
outros utilizadores.

---

## 📐 Requisitos Funcionais

### RF1 — Gestão de Utilizadores
- Registo com email, nome, NIF, número e validade da carta de condução, e password (com hashing)
- Login com validação de credenciais e Logout
- Recuperação de password
- Cada utilizador pode ser simultaneamente **proprietário** (anuncia veículos) e **locatário** (aluga veículos de outros)

### RF2 — Consulta de Veículos
- Listagem dos veículos disponíveis: marca, modelo, ano, combustível, consumo médio (L/100km ou kWh/100km), preço/dia, localização e avaliação média
- Consulta detalhada: fotos, especificações técnicas, avaliações recebidas e histórico de alugueres
- Filtros por marca, preço, localização, transmissão, combustível, lotação e datas de disponibilidade
- Filtro de **kms previstos**: quando preenchido, ordena os veículos pelo custo total estimado:
  `custo_total_estimado = (n_dias × preço_dia_dinâmico) + (kms_previstos / 100 × consumo × preço_combustível_corrente)`
- Decomposição do custo estimado (renda vs combustível) no detalhe do veículo quando há kms previstos
- Preço/dia dinâmico: +20% em fins de semana, +30% em época alta (Julho/Agosto, Natal), -10% em alugueres ≥ 7 dias

### RF3 — Conta Pessoal
- Saldo em euros, com depósito e levantamento de fundos
- Gestão dos meus veículos (proprietário): adicionar, editar, remover (soft delete), calendário de indisponibilidade, histórico de alugueres e rendimento por veículo
- Listagem dos meus alugueres como locatário (ativos, futuros, terminados)
- Listagem dos meus veículos alugados a outros, com receita gerada
- Histórico completo de transações (pagamentos efetuados e recebidos)

### RF4 — Reservas e Alugueres
- Pedido de reserva com datas de início/fim e cálculo automático do preço total (preço dinâmico)
- Janela de 24h para o proprietário aceitar/rejeitar; expira automaticamente caso contrário
- Validação obrigatória contra sobreposição de reservas confirmadas no mesmo veículo
- Custo total = Renda (`n_dias × preço/dia dinâmico`) + Combustível efetivo (`((km_final − km_inicial) / 100) × consumo × preço_combustível_corrente_no_fim`)
- Validação de saldo suficiente, incluindo caução de 20% sobre o custo total estimado (200 km/dia por defeito sem histórico); caução devolvida no fim sem incidentes
- Liquidação automática no fim do aluguer (débito ao locatário, crédito ao proprietário)
- Cancelamento pelo locatário até 48h antes do início, com reembolso parcial
- Registo de quilometragem inicial e final
- Avaliação mútua pós-aluguer (1 a 5 estrelas + comentário)

### RF5 — Interface Gráfica
- Painéis distintos: Login, Dashboard, Procurar Veículos, Conta, As Minhas Reservas
- Mensagens de erro e confirmação amigáveis, com feedback visual

### RF6 — Administração
- Gestão de utilizadores (perfis Administrador / Utilizador)
- Validação e bloqueio de anúncios de veículos
- Gestão dos preços de combustível: preço base por tipo (€/L ou €/kWh) com variação automática a cada 10 minutos e registo horário em BD
- Resolução de disputas entre proprietários e locatários (reembolso forçado ou penalização)
- Histórico de alugueres filtrável por período
- Bloqueio de utilizadores com comportamento abusivo ou avaliações repetidamente negativas

### ★ Requisitos Extra
- Notificações por e-mail (reserva aceite, aluguer a iniciar, lembrete de devolução)
- Exportação do histórico de alugueres em CSV
- Dashboard com gráficos estatísticos (rendimento mensal por veículo, distribuição geográfica das reservas)
