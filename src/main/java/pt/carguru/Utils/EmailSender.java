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
}