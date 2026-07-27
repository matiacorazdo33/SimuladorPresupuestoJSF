package edu.unl.cc.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/**
 * Envia el correo de recuperacion de cuenta usando Jakarta Mail (API
 * estandar de Jakarta EE, equivalente a spring-boot-starter-mail). Si las
 * credenciales SMTP no estan configuradas (o el envio falla por cualquier
 * motivo) no interrumpe el flujo de la aplicacion: el enlace se imprime en
 * la consola del servidor para poder seguir probando sin correo real.
 */
@ApplicationScoped
public class EmailService {

    // Completa estos datos para enviar correos reales. Con Gmail: activa la
    // verificacion en 2 pasos y genera una "contraseña de aplicación" en
    // https://myaccount.google.com/apppasswords (la contraseña normal no
    // funciona aquí).
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = ""; // ej. tu_correo@gmail.com
    private static final String SMTP_PASSWORD = ""; // contraseña de aplicación

    public static final String BASE_URL = "http://localhost:9080";

    public void enviarCorreoRecuperacion(String destinatario, String nombreUsuario, String enlaceRecuperacion) {
        if (SMTP_USERNAME.isBlank() || SMTP_PASSWORD.isBlank()) {
            imprimirEnConsola(destinatario, enlaceRecuperacion, "SMTP no configurado (EmailService.java)");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                }
            });

            Message mensaje = new MimeMessage(session);
            mensaje.setFrom(new InternetAddress(SMTP_USERNAME));
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            mensaje.setSubject("FinFlow · Recupera el acceso a tu cuenta");
            mensaje.setText(
                    "Hola " + nombreUsuario + ",\n\n" +
                    "Detectamos 3 intentos fallidos de inicio de sesión en tu cuenta de FinFlow " +
                    "y bloqueamos el acceso por seguridad.\n\n" +
                    "Si fuiste tú, restablece tu contraseña aquí (válido por 30 minutos):\n" +
                    enlaceRecuperacion + "\n\n" +
                    "Si no fuiste tú, puedes ignorar este mensaje; tu cuenta sigue segura.\n\n" +
                    "— El equipo de FinFlow"
            );

            Transport.send(mensaje);
        } catch (MessagingException e) {
            imprimirEnConsola(destinatario, enlaceRecuperacion, e.getMessage());
        }
    }

    private void imprimirEnConsola(String destinatario, String enlace, String motivo) {
        System.out.println("==============================================");
        System.out.println(" No se pudo enviar el correo de recuperación.");
        System.out.println(" Motivo: " + motivo);
        System.out.println(" Enlace de recuperación para " + destinatario + ":");
        System.out.println(" " + enlace);
        System.out.println("==============================================");
    }
}
