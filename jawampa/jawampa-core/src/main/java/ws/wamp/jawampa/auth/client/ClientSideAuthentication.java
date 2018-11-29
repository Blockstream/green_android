package ws.wamp.jawampa.auth.client;

import ws.wamp.jawampa.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.WampMessages.ChallengeMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ClientSideAuthentication {
    String getAuthMethod();
    AuthenticateMessage handleChallenge( ChallengeMessage message, ObjectMapper objectMapper );
}
