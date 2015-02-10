package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.GreenAddressApplication;
import com.greenaddress.greenbits.QrBitmap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.utils.ExchangeRate;
import org.bitcoinj.utils.Fiat;
import org.bitcoinj.utils.MonetaryFormat;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import de.schildbach.wallet.ui.ScanActivity;

public class BitBoatActivity extends ActionBarActivity {

    int payMethod;
    boolean converting, pausing;
    Handler handler;
    final SettableFuture<ExchangeRate> sfPrice = SettableFuture.create();
    final SettableFuture<ExchangeRate> ppPrice = SettableFuture.create();
    final SettableFuture<ExchangeRate> mcPrice = SettableFuture.create();
    private Coin sfAvailable, ppAvailable, mcAvailable;
    private EditText amountEdit;
    private EditText amountFiatEdit;
    private MonetaryFormat bitcoinFormat;
    private boolean updatingPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        setContentView(R.layout.activity_bit_boat);

        amountEdit = (EditText) findViewById(R.id.amountEditText);
        amountFiatEdit = (EditText) findViewById(R.id.amountFiatEditText);

        final String btcUnit = (String) ((GreenAddressApplication) getApplication()).gaService.getAppearanceValue("unit");
        final String country = ((GreenAddressApplication) getApplication()).gaService.getCountry();
        final TextView bitcoinScale = (TextView) findViewById(R.id.bitcoinScaleText);
        final TextView bitcoinUnitText = (TextView) findViewById(R.id.bitcoinUnitText);
        bitcoinFormat = CurrencyMapper.mapBtcUnitToFormat(btcUnit);
        bitcoinScale.setText(Html.fromHtml(CurrencyMapper.mapBtcUnitToPrefix(btcUnit)));
        if (btcUnit == null || btcUnit.equals("bits")) {
            bitcoinUnitText.setText("bits ");
        } else {
            bitcoinUnitText.setText(Html.fromHtml("&#xf15a; "));
        }

        final List<Map<String, Object>> paymentMethods = new ArrayList<>();
        if (country.equals("IT")) {
            Map<String, Object> superflash = new HashMap<>();
            Map<String, Object> postepay = new HashMap<>();
            superflash.put("name", "Superflash");
            superflash.put("id", BitBoatTransaction.PAYMETHOD_SUPERFLASH);
            postepay.put("name", "Postepay");
            postepay.put("id", BitBoatTransaction.PAYMETHOD_POSTEPAY);
            paymentMethods.add(superflash);
            paymentMethods.add(postepay);
        } else { // FR
            Map<String, Object> mandatcompte = new HashMap<>();
            mandatcompte.put("name", "Mandat Compte");
            mandatcompte.put("id", BitBoatTransaction.PAYMETHOD_MANDATCOMPTE);
            paymentMethods.add(mandatcompte);
        }


        Spinner methodOfPayment = ((Spinner) findViewById(R.id.methodOfPayment));

        methodOfPayment.setAdapter(new SimpleAdapter(
                this,
                paymentMethods,
                R.layout.simple_list_element,
                new String[]{"name"},
                new int[]{R.id.textView}
        ));
        methodOfPayment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> item = paymentMethods.get(position);
                payMethod = ((Number) item.get("id")).intValue();
                convertBtcToFiat();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        HttpUriRequest request = new HttpGet("https://www.bitboat.net/_details");
        final ListenableFuture<String> req = execHTTP(request);

        Futures.addCallback(req, new FutureCallback<String>() {
            @Override
            public void onSuccess(@Nullable String result) {
                Log.d("BitBoat", result);
                Map<String, Object> json = null;
                try {
                     json = new MappingJsonFactory().getCodec().readValue(
                            result, Map.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                Map<String, Object> pp = (Map<String, Object>) json.get("pp");
                Map<String, Object> ppBtc = (Map<String, Object>) pp.get("btc");
                Double ppBtcPrice = (Double) ppBtc.get("price");

                Map<String, Object> sf = (Map<String, Object>) json.get("sf");
                Map<String, Object> sfBtc = (Map<String, Object>) sf.get("btc");
                Double sfBtcPrice = (Double) sfBtc.get("price");

                Map<String, Object> mc = (Map<String, Object>) json.get("mc");
                Map<String, Object> mcBtc = (Map<String, Object>) mc.get("btc");
                Double mcBtcPrice = (Double) sfBtc.get("price");

                ppPrice.set(new ExchangeRate(Fiat.parseFiat("EUR", ppBtcPrice.toString())));
                sfPrice.set(new ExchangeRate(Fiat.parseFiat("EUR", sfBtcPrice.toString())));
                mcPrice.set(new ExchangeRate(Fiat.parseFiat("EUR", mcBtcPrice.toString())));

                ppAvailable = Coin.parseCoin(ppBtc.get("disp").toString());
                sfAvailable = Coin.parseCoin(sfBtc.get("disp").toString());
                mcAvailable = Coin.parseCoin(mcBtc.get("disp").toString());
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
        amountFiatEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertFiatToBtc();
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        amountEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                convertBtcToFiat();
            }

            @Override
            public void afterTextChanged(final Editable s) {

            }
        });

        final Button buyBtcButton = (Button) findViewById(R.id.buyBtcButton);

        buyBtcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Coin available = payMethod == BitBoatTransaction.PAYMETHOD_POSTEPAY ? ppAvailable :
                                 payMethod == BitBoatTransaction.PAYMETHOD_SUPERFLASH ? sfAvailable :
                                 payMethod == BitBoatTransaction.PAYMETHOD_MANDATCOMPTE ? mcAvailable :
                                 Coin.valueOf(0);
                if (bitcoinFormat.parse(amountEdit.getText().toString()).compareTo(available) > 0) {
                    Toast.makeText(BitBoatActivity.this,
                            new Formatter().format(getResources().getString(R.string.onlyBtcForMethodOfPayment),
                                    bitcoinFormat.postfixCode().format(available)).toString(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                final HttpPost post = new HttpPost("https://www.bitboat.net/__new_order");
                final List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("ctype", "btc"));
                nameValuePairs.add(new BasicNameValuePair("ttype",
                        payMethod == BitBoatTransaction.PAYMETHOD_SUPERFLASH ? "sf" :
                        payMethod == BitBoatTransaction.PAYMETHOD_POSTEPAY ? "pp" :
                        "mc"));
                nameValuePairs.add(new BasicNameValuePair("email", "info@greenaddress.it"));
                nameValuePairs.add(new BasicNameValuePair("amount", bitcoinFormat.parse(amountEdit.getText().toString()).toPlainString()));
                nameValuePairs.add(new BasicNameValuePair("eur", amountFiatEdit.getText().toString()));
                nameValuePairs.add(new BasicNameValuePair("platform", "Android"));
                nameValuePairs.add(new BasicNameValuePair("userAgent", (String) (new DefaultHttpClient()).getParams().getParameter(CoreProtocolPNames.USER_AGENT)));

                final SettableFuture<Boolean> dialogFuture = SettableFuture.create();
                final CharSequence title, contents, positive, negative;
                if (country.equals("IT")) {
                    title = "Per favore, accetti le condizioni d'uso per procedere all'acquisto";
                    contents = Html.fromHtml(
                                    "                        <p>Bitboat (questo sito) è una piattaforma per l'acquisto di Bitcoin o altra valuta digitale, con pagamenti corrisposti mediante circuiti Intesa Superflash o Poste Italiane Postepay.</p>\n" +
                                    "                        <p>Accettando queste condizioni <strong>l'acquirente (Lei, fruitore del sito) conferma</strong> che:</p>\n" +
                                    "                        <p>1) In caso di pagamento mediante \"ricarica online\", dichiara di essere il titolare del conto o carta da cui verrà effettuato il pagamento.</p>\n" +
                                    "                        <p>2) L'indirizzo Bitcoin (o altra valuta digitale) di destinazione è un facente parte di un suo <a target=\"_blank\" href=\"https://bitcoin.org/it/scegli-il-tuo-portafoglio\">wallet</a>, anche noto come \"portafogli digitale\", e non un servizio terzo. L'acquirente non utilizzerà Bitboat per inviare Bitcoin a servizi terzi.</p>\n" +
                                    "                        <p>3) Sta acquistando Bitcoin (o altra valuta digitale) per finalità lecite.</p>\n" +
                                    "                        <p>4) Ricaricherà <strong>esattamente</strong> la somma richiesta, o, se impossibilitato, non procederà alla ricarica. Se ritenuto necessario da Bitboat, acconsente a comunicare, successivamente, ulteriori dati inerenti la sua identità ed il pagamento effettuato.</p>\n" +
                                    "                        <p>5) E' a conoscenza del fatto che né \"Intesa Sanpaolo\" né \"Poste Italiane\" sono affiliate alle attività svolte su questo sito.</p>\n" +
                                    "                        <hr>\n" +
                                    "                        <p>Premendo il pulsante \"Accetto\", procederà all'acquisto, confermando espressamente le condizioni di cui sopra. Se non è d'accordo annulli l'acquisto con il pulsante \"Non accetto\".</p>\n");
                    positive = "Accetto";
                    negative = "Non accetto";
                } else {  // FR
                    title = "Conditions d'utilisation";
                    contents = Html.fromHtml(
                                    "                        <p>Bitboat (ce site) est une plateforme d'échange de monnaie numérique (Bitcoin), le réglement s'effectue par mandat compte, un service de la banque postale.</p>\n" +
                                    "                        <p></p>\n" +
                                    "                        <p>1) Vous avez pris connaissance et acceptez les points évoqués dans notre FAQ (https://www.bitboat.net/fr/help)</p>\n" +
                                    "                        <p>2) Vous achetez des bitcoins à des fins licites.</p>\n" +
                                    "                        <p>3) Vous avez conscience que Bitboat n'est affilié en aucune façon à La banque Postale. Bitboat est un service de mise en relation de particuliers.</p>\n" +
                                    "                        <p></p>\n" +
                                    "                        <p></p>\n" +
                                    "                        <hr>\n" +
                                    "                        <p>En cliquant sur le bouton «Accepter», vous approuvez les conditions de vente ci-dessus.</p>"
                    );
                    positive = "Accepter";
                    negative = "Refuser";
                }
                (new MaterialDialog.Builder(BitBoatActivity.this)).
                        title(title)
                        .content(contents)
                        .positiveText(positive)
                        .negativeText(negative)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                List<ListenableFuture<String>> addresses = new ArrayList<ListenableFuture<String>>();
                                addresses.add(Futures.transform(((GreenAddressApplication) getApplication()).gaService.getNewAddress(0), new Function<QrBitmap, String>() {
                                    @Nullable
                                    @Override
                                    public String apply(@Nullable QrBitmap input) {
                                        return input.data;
                                    }
                                }));
                                addresses.add(((GreenAddressApplication) getApplication()).gaService.fundReceivingId("GA2nxNXvFENfGM9K27KckeGqNB2JTi"));
                                Futures.addCallback(Futures.allAsList(addresses), new FutureCallback<List<String>>() {
                                            @Override
                                            public void onSuccess(@Nullable List<String> result) {
                                                nameValuePairs.add(new BasicNameValuePair("dest", result.get(0)));
                                                nameValuePairs.add(new BasicNameValuePair("ref", result.get(1)));
                                                try {
                                                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                                                } catch (UnsupportedEncodingException e) {
                                                    throw new RuntimeException(e);
                                                }
                                                dialogFuture.set(true);
                                            }

                                            @Override
                                            public void onFailure(Throwable t) {
                                                t.printStackTrace();
                                                Toast.makeText(BitBoatActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        })
                        .build().show();
                    ListenableFuture<String> confirmedHTTP = Futures.transform(dialogFuture, new AsyncFunction<Boolean, String>() {
                        @Override
                        public ListenableFuture<String> apply(Boolean input) throws Exception {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buyBtcButton.setEnabled(false);
                                    amountEdit.setEnabled(false);
                                    amountFiatEdit.setEnabled(false);
                                }
                            });
                            return execHTTP(post);
                        }
                    });
                    Futures.addCallback(confirmedHTTP, new FutureCallback<String>() {
                        @Override
                        public void onSuccess(@Nullable String result) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buyBtcButton.setEnabled(true);
                                    amountEdit.setEnabled(true);
                                    amountFiatEdit.setEnabled(true);
                                    amountEdit.setText("");
                                }
                            });

                            Map<String, Object> json = null;
                            try {
                                json = new MappingJsonFactory().getCodec().readValue(
                                        result, Map.class);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            String id = (String) json.get("id");
                            ArrayList<String> pending = (ArrayList) ((GreenAddressApplication) getApplication()).gaService.getAppearanceValue("pending_bitboat_ids");
                            if (pending == null) pending = new ArrayList<>();
                            pending.add(id);
                            Futures.addCallback(((GreenAddressApplication) getApplication()).gaService.setAppearanceValue("pending_bitboat_ids", pending, false), new FutureCallback<Boolean>() {
                                @Override
                                public void onSuccess(@Nullable Boolean result) {
                                    updatePendingOrders();
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    t.printStackTrace();
                                }
                            });
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            t.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(BitBoatActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                                    buyBtcButton.setEnabled(true);
                                    amountEdit.setEnabled(true);
                                    amountFiatEdit.setEnabled(true);
                                }
                            });
                        }
                    });
                }
            });
        if (!updatingPending) {
            updatePendingOrders();
        }
    }

    private void updatePendingOrders() {
        if (((GreenAddressApplication) getApplication()).getConnectionObservable().getState() != ConnectivityObservable.State.LOGGEDIN) {
            return;
        }
        final ArrayList<String> pending = (ArrayList) ((GreenAddressApplication) getApplication()).gaService.getAppearanceValue("pending_bitboat_ids");
        final ArrayList<ListenableFuture<String>> results = new ArrayList<>();
        if (pending == null) return;
        for (final String id : pending) {
            results.add(Futures.transform(execHTTP(new HttpGet("https://www.bitboat.net/__validate/" + id)), new AsyncFunction<String, String>() {
                @Override
                public ListenableFuture<String> apply(String input) throws Exception {
                    return execHTTP(new HttpGet("https://www.bitboat.net/__order_summary?uuid=" + id));
                }
            }));
        }
        Futures.addCallback(Futures.allAsList(results), new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable List<String> results) {
                final ArrayList<String> newPending = new ArrayList<>();
                final LinkedList<BitBoatTransaction> currentList = new LinkedList<>();
                int i = 0;
                boolean anyToCheckAgain = false;
                for (String res : results) {
                    Log.d("BitBoat", res);
                    ArrayList<Object> json = null;
                    try {
                        json = new MappingJsonFactory().getCodec().readValue(res, ArrayList.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Map<?, ?> map = (Map<?, ?>) json.get(0);
                    boolean isRelevant = false;
                    if (map.get("status").equals("submitted") || map.get("status").equals("pending") || map.get("status").equals("accepted") || map.get("status").equals("queued")) {
                        newPending.add(pending.get(i));
                        anyToCheckAgain = true;
                        isRelevant = true;
                    }
                    if (map.get("status").equals("accepted")) {
                        Map<?, ?> vendor = (Map<?, ?>) map.get("vendor");
                        Map<?, ?> vendor_v = (Map<?, ?>) vendor.get("v");
                        Map<?, ?> vendor_v_value = (Map<?, ?>) vendor_v.get("value");
                        currentList.add(0, new BitBoatTransaction(
                                new Date(((Number) map.get("timestamp")).longValue()),
                                map.get("fb").toString(),
                                map.get("ttype").equals("sf") ?
                                        BitBoatTransaction.PAYMETHOD_SUPERFLASH :
                                map.get("ttype").equals("pp") ?
                                        BitBoatTransaction.PAYMETHOD_POSTEPAY :
                                        BitBoatTransaction.PAYMETHOD_MANDATCOMPTE,
                                Coin.parseCoin(vendor.get("amount").toString()),
                                Fiat.parseFiat("EUR", vendor.get("eur").toString()),
                                vendor_v_value.get("pp").toString(),
                                (String) vendor_v_value.get("cf"),
                                (String) vendor_v.get("key")
                        ));
                    } else if (isRelevant) {
                        final String status;
                        if (map.get("status").equals("submitted")) {
                            status = getResources().getString(R.string.txStatusSubmitted);
                        } else if (map.get("status").equals("pending")) {
                            status = getResources().getString(R.string.txStatusPending);
                        } else if (map.get("status").equals("accepted")) {
                            status = getResources().getString(R.string.txStatusAccepted);
                        } else if (map.get("status").equals("queued")) {
                            status = getResources().getString(R.string.txStatusQueued);
                        } else {
                            status = getResources().getString(R.string.txStatusUnknown);
                        }
                        currentList.add(0, new BitBoatTransaction(
                                new Date(((Number) map.get("timestamp")).longValue()),
                                map.get("fb") != null ? map.get("fb").toString() : "",
                                -1,
                                Coin.parseCoin(map.get("amount").toString()),
                                null,
                                "", "", status
                        ));
                    }
                    i += 1;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (((GreenAddressApplication) getApplication()).getConnectionObservable().getState() != ConnectivityObservable.State.LOGGEDIN) {
                            return;
                        }

                        final ListView listView = (ListView) findViewById(R.id.listView);
                        final String btcUnit = (String) ((GreenAddressApplication) getApplication()).gaService.getAppearanceValue("unit");
                        if (listView.getAdapter() != null) {
                            ((ListBitBoatTxsAdapter) listView.getAdapter()).clear();
                            for (BitBoatTransaction tx : currentList) {
                                ((ListBitBoatTxsAdapter) listView.getAdapter()).add(tx);
                            }
                            ((ListBitBoatTxsAdapter) listView.getAdapter()).notifyDataSetChanged();
                        } else {
                            listView.setAdapter(new ListBitBoatTxsAdapter(BitBoatActivity.this, R.layout.list_element_bitboat, currentList, btcUnit));
                        }
                    }
                });

                ((GreenAddressApplication) getApplication()).gaService.setAppearanceValue("pending_bitboat_ids", newPending, false);

                if (anyToCheckAgain) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updatePendingOrders();
                        }
                    }, 10000);
                    updatingPending = true;
                } else {
                    updatingPending = false;
                }

            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(BitBoatActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacksAndMessages(null);
    }

    private ListenableFuture<String> execHTTP(HttpUriRequest request) {
        final SettableFuture<String> req = SettableFuture.create();
        (new AsyncTask<HttpUriRequest, String, String>() {
            @Override
            protected String doInBackground(HttpUriRequest... request) {
                HttpClient client = new DefaultHttpClient();
                String responseStr = null;
                HttpResponse response;
                try {
                    response = client.execute(request[0]);
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        responseStr = out.toString();
                        out.close();
                    } else {
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (IOException e) {
                    req.setException(e);
                }
                req.set(responseStr);
                return responseStr;
            }
        }).execute(request);
        return req;
    }

    private void convertBtcToFiat() {
        if (converting || pausing) {
            return;
        }
        converting = true;
        Futures.addCallback(getExchangeRateFuture(), new FutureCallback<ExchangeRate>() {
            @Override
            public void onSuccess(@Nullable final ExchangeRate rate) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Coin btcValue = bitcoinFormat.parse(amountEdit.getText().toString());
                            Fiat fiatValue = rate.coinToFiat(btcValue);
                            // strip extra decimals (over 2 places) because that's what the old JS client does
                            fiatValue = fiatValue.subtract(fiatValue.divideAndRemainder((long) Math.pow(10, Fiat.SMALLEST_UNIT_EXPONENT - 2))[1]);
                            amountFiatEdit.setText(fiatValue.toPlainString());
                        } catch (final ArithmeticException | IllegalArgumentException e) {
                            amountFiatEdit.setText("");
                        }
                    }
                });
                converting = false;
            }

            @Override
            public void onFailure(Throwable t) {
                converting = false;
                t.printStackTrace();
            }
        });
    }

    private void convertFiatToBtc() {
        if (converting || pausing) {
            return;
        }
        converting = true;
        Futures.addCallback(getExchangeRateFuture(), new FutureCallback<ExchangeRate>() {
            @Override
            public void onSuccess(@Nullable final ExchangeRate rate) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Fiat fiatValue = Fiat.parseFiat("EUR", amountFiatEdit.getText().toString());
                            amountEdit.setText(bitcoinFormat.noCode().format(rate.fiatToCoin(fiatValue)));
                        } catch (final ArithmeticException | IllegalArgumentException e) {
                            amountEdit.setText("");
                        }
                    }
                });
                converting = false;
            }

            @Override
            public void onFailure(Throwable t) {;
                converting = false;
                t.printStackTrace();
            }
        });
    }

    private ListenableFuture<ExchangeRate> getExchangeRateFuture() {
        ListenableFuture<ExchangeRate> exchangeFuture;
        if (payMethod == BitBoatTransaction.PAYMETHOD_POSTEPAY) {
            exchangeFuture = ppPrice;
        } else if (payMethod == BitBoatTransaction.PAYMETHOD_SUPERFLASH) {
            exchangeFuture = sfPrice;
        } else {
            exchangeFuture = mcPrice;
        }
        return exchangeFuture;
    }

    @Override
    public void onPause() {
        super.onPause();
        pausing = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        pausing = false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.bitboat, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();
        if (id == R.id.action_bitboat) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bitboat.net/")));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
