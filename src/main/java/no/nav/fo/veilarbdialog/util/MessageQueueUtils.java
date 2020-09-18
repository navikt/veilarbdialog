package no.nav.fo.veilarbdialog.util;

import org.springframework.jms.core.MessageCreator;

import javax.jms.TextMessage;

public class MessageQueueUtils {

    public static MessageCreator messageCreator(final String hendelse, String uuid) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", uuid);
            return msg;
        };
    }

}
