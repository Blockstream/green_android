package com.greenaddress.greenbits.ui.assets;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.gdk.GDKTwoFactorCall;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;
import com.greenaddress.greenbits.ui.TabbedMainActivity;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;
import com.greenaddress.greenbits.ui.send.ScanActivity;
import com.greenaddress.greenbits.ui.send.SendAmountActivity;
import com.greenaddress.greenbits.wallets.HardwareCodeResolver;

import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;

public class AssetsSelectActivity extends LoggedActivity implements AssetsAdapter.OnAssetSelected {

    private RecyclerView assetsList;
    private Map<String, Long> mAssetsBalances;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (modelIsNullOrDisconnected())
            return;

        UI.preventScreenshots(this);

        setTitleBackTransparent();
        setContentView(R.layout.activity_assets_selection);

        final String callingActivity = getCallingActivity() != null ? getCallingActivity().getClassName() : "";
        if (callingActivity.equals(TabbedMainActivity.class.getName())) {
            final String accountName = getModel().getSubaccountsDataObservable().getSubaccountsDataWithPointer(
                getModel().getCurrentSubaccount()).getNameWithDefault(getString(R.string.id_main_account));
            setTitle(accountName);
        } else if (callingActivity.equals(ScanActivity.class.getName())) {
            setTitle(R.string.id_select_asset);
        }

        assetsList = findViewById(R.id.assetsList);
        assetsList.setLayoutManager(new LinearLayoutManager(this));
        try {
            mAssetsBalances = getModel().getCurrentAccountBalanceData();

            final AssetsAdapter adapter = new AssetsAdapter(mAssetsBalances, getNetwork(), this, getModel());
            assetsList.setAdapter(adapter);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            setResult(RESULT_CANCELED);
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAssetSelected(final String assetId) {
        if (getIntent().hasExtra(PrefKeys.INTENT_STRING_TX)) {
            // Update transaction in send navigation flow
            try {
                final ObjectNode tx = updateTransaction(assetId);
                final Intent intent = new Intent(this, SendAmountActivity.class);
                removeUtxosIfTooBig(tx);
                intent.putExtra(PrefKeys.INTENT_STRING_TX, tx.toString());
                startActivityForResult(intent, REQUEST_BITCOIN_URL_SEND);
            } catch (final Exception e) {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        // Nothing for btc
        if ("btc".equals(assetId))
            return;

        // Open selected asset detail page
        final Intent intent = new Intent(this, AssetActivity.class);
        final AssetInfoData assetInfo = getModel().getAssetsObservable().getAssetsInfos().get(assetId);
        intent.putExtra("ASSET_ID", assetId)
        .putExtra("ASSET_INFO", assetInfo)
        .putExtra("SATOSHI", mAssetsBalances.get(assetId));
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BITCOIN_URL_SEND && resultCode == RESULT_OK) {
            setResult(resultCode);
            finishOnUiThread();
        }
    }

    private ObjectNode updateTransaction(final String assetId) throws Exception {
        final String tx = getIntent().getStringExtra(PrefKeys.INTENT_STRING_TX);
        final ObjectNode txJson = new ObjectMapper().readValue(tx, ObjectNode.class);
        final ObjectNode addressee = (ObjectNode) txJson.get("addressees").get(0);
        addressee.put("asset_tag", assetId);
        final GDKTwoFactorCall call = getSession().createTransactionRaw(null, txJson);
        return call.resolve(null, new HardwareCodeResolver(this));
    }
}