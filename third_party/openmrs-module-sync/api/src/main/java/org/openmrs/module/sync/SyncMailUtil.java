/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.notification.MessageException;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for configuring and validating email settings, and sending emails
 */
public class SyncMailUtil implements GlobalPropertyListener {

    private static final Log log = LogFactory.getLog(SyncMailUtil.class);

	// ***** CONSTANTS *****

	public static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport_protocol";
	public static final String MAIL_SMTP_HOST = "mail.smtp_host";
	public static final String MAIL_SMTP_PORT = "mail.smtp_port";
	public static final String MAIL_SMTP_AUTH = "mail.smtp_auth";
	public static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
	public static final String MAIL_USER = "mail.user";
	public static final String MAIL_PASSWORD = "mail.password";
	public static final String MAIL_DEFAULT_CONTENT_TYPE = "mail.default_content_type";
	public static final String MAIL_FROM = "mail.from";
	public static final String MAIL_DEBUG = "mail.debug";
	public static final String SYNC_ADMIN_EMAIL = "sync.admin_email";

	public static List<String> getMailGlobalProperties() {
		return Arrays.asList(
			MAIL_TRANSPORT_PROTOCOL, MAIL_SMTP_HOST, MAIL_SMTP_PORT, MAIL_SMTP_AUTH, MAIL_SMTP_STARTTLS_ENABLE,
			MAIL_USER, MAIL_PASSWORD, MAIL_DEFAULT_CONTENT_TYPE, MAIL_FROM, MAIL_DEBUG, SYNC_ADMIN_EMAIL
		);
	}

	//***** UTILITY METHODS

	/**
	 * Cached mail session.  This is set to null and re-loaded anytime a mail global property changes
	 */
	private static Session mailSession = null;

	/**
	 * @return the currently configured email settings
	 */
	public static Map<String, String> getCurrentlyConfiguredSettings() {
		AdministrationService adminService = Context.getAdministrationService();
		Map<String, String> settings = new LinkedHashMap<String, String>();
		for (String s : getMailGlobalProperties()) {
			settings.put(s, adminService.getGlobalProperty(s));
		}
		return settings;
	}

	/**
	 * @return the currently cached mail session, or a new mail session if any mail configuration is changed
	 */
    public static Session getMailSession() {
        if (mailSession == null ) {
        	mailSession = createSession(getCurrentlyConfiguredSettings());
		}
        return mailSession;
    }

	public static Session createSession(Map<String, String> settings) {
		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", settings.get(MAIL_TRANSPORT_PROTOCOL));
		props.setProperty("mail.smtp.host", settings.get(MAIL_SMTP_HOST));
		props.setProperty("mail.smtp.port", settings.get(MAIL_SMTP_PORT));
		props.setProperty("mail.smtp.auth", settings.get(MAIL_SMTP_AUTH));
		props.setProperty("mail.smtp.starttls.enable", settings.get(MAIL_SMTP_STARTTLS_ENABLE));
		props.setProperty("mail.from", settings.get(MAIL_FROM));
		props.setProperty("mail.debug", settings.get(MAIL_DEBUG));

		final String mailUser = settings.get(MAIL_USER);
		final String mailPw = settings.get(MAIL_PASSWORD);

		Authenticator auth = new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailUser, mailPw);
			}
		};
		return Session.getInstance(props, auth);
	}

	public static String validateSettings(Map<String, String> settings) {
		MessageSourceService mss = Context.getMessageSourceService();
		try {
			Session session = createSession(settings);
			Transport transport = session.getTransport();
			transport.connect();
			transport.close();
			return mss.getMessage("sync.emailConfig.connectionSuccessful");
		}
		catch (Exception e) {
			return mss.getMessage("sync.emailConfig.connectionFailed") + ": " + e.getMessage();
		}
	}

	/**
	 * Sends a message using the current mail session
	 */
    public static void sendMessage(String recipients, String subject, String body) throws MessageException {
        try {
            Message message = new MimeMessage(getMailSession());
            message.setSentDate(new Date());
            if (StringUtils.isNotBlank(subject)) {
                message.setSubject(subject);
            }
            if (StringUtils.isNotBlank(recipients)) {
                for (String recipient : recipients.split("\\,")) {
                    message.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(recipient));
                }
            }
            if (StringUtils.isNotBlank(body)) {
                Multipart multipart = new MimeMultipart();
                MimeBodyPart contentBodyPart = new MimeBodyPart();
                contentBodyPart.setContent(body, "text/html");
                multipart.addBodyPart(contentBodyPart);
                message.setContent(multipart);
            }
			log.info("Sending email with subject <" + subject + "> to <"+recipients+">");
			log.debug("Mail has contents: \n" + body);
            Transport.send(message);
			log.debug("Message sent without errors");
        }
		catch (Exception e) {
            log.error("Message could not be sent due to " + e.getMessage(), e);
            throw new MessageException(e);
        }
    }

	@Override
	public boolean supportsPropertyName(String s) {
		return getMailGlobalProperties().contains(s);
	}

	@Override
	public void globalPropertyChanged(GlobalProperty globalProperty) {
		log.debug("Global property <" + globalProperty.getProperty() + "> changed, resetting mail session");
		mailSession = null;
	}

	@Override
	public void globalPropertyDeleted(String s) {
		log.debug("Global property <" + s + "> deleted, resetting mail session");
		mailSession = null;
	}
}
