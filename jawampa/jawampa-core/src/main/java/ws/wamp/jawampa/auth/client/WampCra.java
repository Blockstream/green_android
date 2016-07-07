package ws.wamp.jawampa.auth.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import ws.wamp.jawampa.WampMessages;

public class WampCra implements ClientSideAuthentication {
    public static final String AUTH_METHOD = "wampcra";
    private final String key;

    public WampCra(String key) {
        this.key = key;
    }

    @Override
    public String getAuthMethod() {
        return AUTH_METHOD;
    }

    private String calculateHmacSHA256(String challenge, ObjectMapper objectMapper) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(challenge.getBytes());
        return objectMapper.convertValue( rawHmac, String.class );
    }


    @Override
    public WampMessages.AuthenticateMessage handleChallenge(WampMessages.ChallengeMessage message, ObjectMapper objectMapper) {
        String challenge = message.extra.get( "challenge" ).asText();
        try {
            String signature = calculateHmacSHA256( challenge, objectMapper );
            return new WampMessages.AuthenticateMessage(signature, objectMapper.createObjectNode() );
        } catch( InvalidKeyException e ) {
            throw new RuntimeException( "InvalidKeyException while calculating HMAC" );
        } catch( NoSuchAlgorithmException e ) {
            throw new RuntimeException( "NoSuchAlgorithmException while calculating HMAC" );
        }
    }
}
