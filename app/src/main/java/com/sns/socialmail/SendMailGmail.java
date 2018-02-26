package com.sns.socialmail;


import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


public class SendMailGmail {

    final String emailPort = "587";// gmail's smtp port
    //final String emailHost = "smtp.gmail.com";
    final String emailHost = "smtp.googlemail.com";

    String fromEmail;
    String fromPassword;
    List<String> toEmailList;
    String emailSubject;
    String emailBody;
    String filePath;

    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;
    BodyPart messageBodyPart;
    // Create message part for attaching file
    BodyPart messageFilePart;
    // Create a multipart message
    Multipart multipart;

    public SendMailGmail(String fromEmail, String fromPassword,
                         List<String> toEmailList, String emailSubject, String emailBody, String filePath) {
        this.fromEmail = fromEmail;
        this.fromPassword = fromPassword;
        this.toEmailList = toEmailList;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;
        this.filePath = filePath;

        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.host", emailHost);
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.starttls.enable", "true");
        emailProperties.put("mail.smtp.auth", "true");
        emailProperties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        //emailProperties.put("mail.debug", "true");
        Log.i("SendMailGmail", "Mail server properties set.");
    }

    public MimeMessage createEmailMessage() throws MessagingException, UnsupportedEncodingException {

        mailSession = Session.getDefaultInstance(emailProperties);
        //mailSession = Session.getDefaultInstance(emailProperties, null);


        emailMessage = new MimeMessage(mailSession);
        emailMessage.setFrom(new InternetAddress(fromEmail, fromEmail));
        //for (Object toEmail : toEmailList) {
        for (String toEmail : toEmailList) {
            Log.i("SendMailGmail","toEmail: "+toEmail);
            emailMessage.addRecipient(Message.RecipientType.CC,
                    new InternetAddress(toEmail));
       }

        emailMessage.setSubject(emailSubject);

        //Create MimeBodyPart object and set your message text
        messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(emailBody);

        //Create Multipart object
        multipart = new MimeMultipart();

        //Add body to multipart
        multipart.addBodyPart(messageBodyPart);

        //ADD ATTACHMENT IF ANY
        if (filePath != "null") {
            //Create new MimeBodyPart object and set DataHandler object to this
            messageFilePart = new MimeBodyPart();

            DataSource source = new FileDataSource(filePath);
            messageFilePart.setDataHandler(new DataHandler(source));
            messageFilePart.setFileName(new File(filePath).getName());
            {
            }

            multipart.addBodyPart(messageFilePart);
        }
        //set the multipart object to the message object
        emailMessage.setContent(multipart, "text/html");

        Log.i("SendMailGmail", "Email Message created.");
        return emailMessage;
    }

    public void sendEmail() throws MessagingException {

        try {
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(emailHost, fromEmail, fromPassword);
            //transport.connect(emailHost, fromEmail, "ya29.GltjBUuskIeJUPT8PgNIXvqcpOnsgo4Y8zV5vwbvpWe6Z5R3jWSHnvN5_Bxmy15OnN4jPyT3mwJLavQ7FxmE2ey5YnYRea44ea-otZ92Oz4X41GaaZbKunTwMHGu");
            Log.i("SendMailGmail", "allrecipients: " + emailMessage.getAllRecipients());
            transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
            transport.close();
            Log.i("SendMailGmail", "Email sent successfully.");
        }
        catch (MessagingException e){
            throw new RuntimeException(e);
        }

    }
}



