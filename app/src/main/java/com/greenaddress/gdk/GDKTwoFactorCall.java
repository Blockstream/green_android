package com.greenaddress.gdk;

import android.util.Log;

import com.blockstream.libgreenaddress.GDK;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.Bridge;
import com.greenaddress.greenapi.data.TwoFactorStatusData;

public class GDKTwoFactorCall {
    private Object mTwoFactorCall;
    private TwoFactorStatusData mStatus;
    private static ObjectMapper mObjectMapper = new ObjectMapper();

    GDKTwoFactorCall(final Object twoFactorCall) {
        mTwoFactorCall = twoFactorCall;
        mObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public TwoFactorStatusData getStatus() {
        return mStatus;
    }

    private void updateStatus() throws JsonProcessingException {
        mStatus = twofactorGetStatus();
    }

    /**
     * This method must be called in a separate thread if requires GUI input, otherwise it blocks
     * @param methodResolver callback to choose the method from the list of available ones
     * @param codeResolver callback to input the code
     * @throws Exception
     */
    public ObjectNode resolve(final MethodResolver methodResolver, final CodeResolver codeResolver) throws Exception {
        while (true) {
            updateStatus();
            switch (mStatus.getStatus()) {
                case "call":
                    Log.d("RSV", "call " + mStatus);
                    twofactorCall();
                    break;
                case "request_code":
                    Log.d("RSV", "request_code " + mStatus);
                    final SettableFuture<String> method = methodResolver.method(mStatus.getMethods());
                    final String chosenMethod = method.get();
                    Log.d("RSV", "request_code choosen method " + chosenMethod);
                    if (chosenMethod == null) {
                        throw new Exception("id_action_canceled");
                    }
                    twofactorRequestCode(chosenMethod);
                    break;
                case "resolve_code":
                    Log.d("RSV", "resolve_code " + mStatus);
                    final String value;
                    if (mStatus.getDevice() != null) {
                        try {
                            value = codeResolver.requestDataFromDeviceV3(mStatus.getRequiredData()).blockingGet();
                        } catch (final Exception e) {
                            Log.d("RSV", "error " + mStatus);
                            throw new Exception(e.getMessage());
                        }
                    } else {
                        value = codeResolver.code(mStatus.getMethod(), mStatus.getAttemptsRemaining()).get();
                    }
                    Log.d("RSV", "resolve_code input " + value);
                    if (value == null) {
                        throw new Exception("id_action_canceled");
                    }
                    twofactorResolveCode(value);
                    break;
                case "error":
                    Log.d("RSV", "error " + mStatus);
                    destroyTwofactorCall();
                    throw new Exception(mStatus.getError());
                case "done":
                    Log.d("RSV", "done " + mStatus);
                    destroyTwofactorCall();
                    if (mStatus.getResult() == null) {
                        return mObjectMapper.createObjectNode();
                    }
                    return mStatus.getResult();
            }
        }
    }

    private void twofactorRequestCode(final String method) {
        GDK.auth_handler_request_code(mTwoFactorCall, method);
    }

    private void twofactorResolveCode(final String value) {
        GDK.auth_handler_resolve_code(mTwoFactorCall, value);
    }

    private void twofactorCall() {
        GDK.auth_handler_call(mTwoFactorCall);
    }

    private void destroyTwofactorCall() {
        GDK.destroy_auth_handler(mTwoFactorCall);
    }


    private TwoFactorStatusData twofactorGetStatus() throws JsonProcessingException {
        final ObjectNode jsonNode = Bridge.INSTANCE.toJackson(GDK.auth_handler_get_status(mTwoFactorCall));
        final TwoFactorStatusData mStatus = mObjectMapper.treeToValue(jsonNode, TwoFactorStatusData.class);
        GDKSession.debugEqual(jsonNode, mStatus);
        return mStatus;
    }

    @Override
    public String toString() {
        return "GDKTwoFactorCall{" + "mStatus=" + mStatus + '}';
    }
}
