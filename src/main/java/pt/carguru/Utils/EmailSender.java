package pt.carguru.Utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.Session;
import java.util.Properties;

public class EmailSender {

    // Configuração — usa uma conta Gmail
    private static final String REMETENTE = "enzoppenzo@gmail.com";
    private static final String PASSWORD   = "pvwp lygv lnnp npjq"; // App Password do Gmail

    private static Session criarSessao() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMETENTE, PASSWORD);
            }
        });
    }

    public static void send(String destinatario, String assunto, String corpo) {
        try {
            Session session = criarSessao();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(REMETENTE));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(assunto);
            message.setText(corpo);
            Transport.send(message);
            System.out.println("[EMAIL] Enviado para: " + destinatario);
        } catch (MessagingException e) {
            System.err.println("[EMAIL] Erro ao enviar: " + e.getMessage());
        }
    }

    // Métodos específicos para o CarGuru
    public static void enviarTokenRecuperacao(String destinatario, String token) {
        String assunto = "CarGuru — Recuperação de Password";
        String corpo = """
                Olá,
                
                Recebemos um pedido de recuperação de password para a tua conta CarGuru.
                
                O teu código de recuperação é:
                
                %s
                
                Este código é válido durante 1 hora.
                
                Se não fizeste este pedido, ignora este email.
                
                CarGuru
                """.formatted(token);
        send(destinatario, assunto, corpo);
    }

    public static void enviarConfirmacaoReserva(String destinatario, String nomeVeiculo, String dataInicio, String dataFim) {
        String assunto = "CarGuru — Reserva Confirmada";
        String corpo = """
                Olá,
                
                A tua reserva foi confirmada!
                
                Veículo: %s
                De: %s
                Até: %s
                
                Boas viagens! 🚗
                
                Equipa CarGuru
                """.formatted(nomeVeiculo, dataInicio, dataFim);
        send(destinatario, assunto, corpo);
    }
    /**
     * Notifica o proprietário que o seu anúncio de veículo foi aprovado pelo administrador.
     */
    public static void enviarAprovacaoVeiculo(String destinatario, String nomeProprietario, String nomeVeiculo) {
        String assunto = "CarGuru — Anúncio Aprovado 🎉";
        String corpo = """
                Olá %s,

                Ótimas notícias! O teu anúncio foi aprovado pela nossa equipa de administração.

                Veículo: %s

                O veículo já se encontra disponível na plataforma e pode ser reservado por outros utilizadores.

                Obrigado por fazeres parte da comunidade CarGuru! 🚗

                Equipa CarGuru
                """.formatted(nomeProprietario, nomeVeiculo);
        send(destinatario, assunto, corpo);
    }

    /**
     * Notifica o proprietário que o seu anúncio de veículo foi rejeitado, incluindo o motivo.
     */
    public static void enviarRejeicaoVeiculo(String destinatario, String nomeProprietario,
                                              String nomeVeiculo, String motivo) {
        String assunto = "CarGuru — Anúncio Rejeitado";
        String corpo = """
                Olá %s,

                Após análise, o teu anúncio não foi aprovado pela nossa equipa de administração.

                Veículo: %s

                Motivo da rejeição:
                %s

                Podes corrigir os dados indicados e submeter novamente o teu veículo na área "Conta".
                Se tiveres dúvidas, contacta o suporte.

                Equipa CarGuru
                """.formatted(nomeProprietario, nomeVeiculo, motivo);
        send(destinatario, assunto, corpo);
    }

    /**
     * Notifica ambas as partes de uma disputa que foi resolvida pelo administrador.
     *
     * @param destinatario  email do utilizador a notificar
     * @param nomeDestinatario nome do utilizador
     * @param disputaId     ID da disputa
     * @param veiculoNome   nome do veículo em causa
     * @param decisao       texto da decisão escrita pelo admin
     * @param estadoFinal   "RESOLVIDA_PROPRIETARIO" | "RESOLVIDA_LOCATARIO" | "ENCERRADA"
     * @param valorMovimento valor financeiro movimentado (0 se nenhum)
     * @param papelDestinatario "locatário" ou "proprietário"
     */
    public static void enviarResolucaoDisputa(String destinatario, String nomeDestinatario,
                                               int disputaId, String veiculoNome,
                                               String decisao, String estadoFinal,
                                               double valorMovimento, String papelDestinatario) {
        String assunto = String.format("CarGuru — Disputa #%d Resolvida", disputaId);

        String resultadoFinanceiro = "";
        if (valorMovimento > 0.01) {
            resultadoFinanceiro = switch (estadoFinal.toUpperCase()) {
                case "RESOLVIDA_PROPRIETARIO" -> "locatário".equals(papelDestinatario)
                        ? String.format("💸 Foi aplicada uma penalização de %.2f€ na tua conta.", valorMovimento)
                        : String.format("💶 Foi creditado o valor de %.2f€ na tua conta.", valorMovimento);
                case "RESOLVIDA_LOCATARIO" -> "locatário".equals(papelDestinatario)
                        ? String.format("💶 A caução de %.2f€ foi devolvida à tua conta.", valorMovimento)
                        : "Não houve transferência financeira para a tua conta nesta resolução.";
                default -> "";
            };
        }

        String corpo = """
                Olá %s,

                A Disputa #%d relativa ao veículo %s foi resolvida pela equipa de administração CarGuru.

                ─────────────────────────────────────────
                Decisão do administrador:
                %s
                ─────────────────────────────────────────
                %s

                Se tiveres questões sobre esta decisão, contacta o suporte CarGuru.

                Equipa CarGuru
                """.formatted(nomeDestinatario, disputaId, veiculoNome, decisao,
                              resultadoFinanceiro.isBlank() ? "Não houve movimentos financeiros nesta resolução." : resultadoFinanceiro);
        send(destinatario, assunto, corpo);
    }


}