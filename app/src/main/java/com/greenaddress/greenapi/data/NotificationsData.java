package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class NotificationsData extends JSONData {
    private boolean emailIncoming;
    private boolean emailOutgoing;

    public boolean isEmailIncoming() {
        return emailIncoming;
    }

    public void setEmailIncoming(boolean emailIncoming) {
        this.emailIncoming = emailIncoming;
    }

    public boolean isEmailOutgoing() {
        return emailOutgoing;
    }

    public void setEmailOutgoing(boolean emailOutgoing) {
        this.emailOutgoing = emailOutgoing;
    }
}
