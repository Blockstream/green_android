package com.greenaddress.greenbits.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.BalanceData;
import com.greenaddress.greenbits.ui.components.AssetsAdapter;
import com.greenaddress.greenbits.ui.preferences.PrefKeys;

import org.bitcoinj.core.AddressFormatException;

import java.util.Map;

import static com.greenaddress.gdk.GDKSession.getSession;
import static com.greenaddress.greenbits.ui.ScanActivity.INTENT_STRING_TX;
import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;

public class AssetsSelectActivity extends LoggedActivity implements AssetsAdapter.OnAssetSelected {

    private RecyclerView assetsList;
    private Map<String, BalanceData> mAssetsBalances;

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        if (mService == null || mService.getModel() == null) {
            toFirst();
            return;
        }
        setTitleBackTransparent();
        setContentView(R.layout.activity_assets_selection);

        final String callingActivity = getCallingActivity() != null ? getCallingActivity().getClassName() : "";
        if (callingActivity.equals(TabbedMainActivity.class.getName())) {
            final String accountName = getModel().getSubaccountDataObservable().getSubaccountDataWithPointer(
                getModel().getCurrentSubaccount()).getNameWithDefault(getString(R.string.id_main_account));
            setTitle(accountName);
        } else if (callingActivity.equals(ScanActivity.class.getName())) {
            setTitle(R.string.id_select_asset);
        }

        assetsList = findViewById(R.id.assetsList);
        assetsList.setLayoutManager(new LinearLayoutManager(this));
        try {
            mAssetsBalances = getModel().getCurrentAccountBalanceData();

            final AssetsAdapter adapter = new AssetsAdapter(mAssetsBalances, mService, this);
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
    protected void onResumeWithService() {
        super.onResumeWithService();
        if (mService == null || mService.getModel() == null)
            return;
        if (mService.isDisconnected()) {
            return;
        }
    }

    @Override
    protected void onPauseWithService() {
        super.onPauseWithService();
        if (mService == null || mService.getModel() == null)
            return;
    }

    @Override
    public void onAssetSelected(final String assetId) {
        if (getIntent().hasExtra(INTENT_STRING_TX)) {
            // Update transaction in send navigation flow
            try {
                final ObjectNode tx = updateTransaction(assetId);
                final Intent intent = new Intent(this, SendAmountActivity.class);
                intent.putExtra(INTENT_STRING_TX, tx.toString());
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
        intent.putExtra("ASSET_ID", assetId)
        .putExtra("ASSET_INFO", mAssetsBalances.get(assetId).getAssetInfo())
        .putExtra("SATOSHI", mAssetsBalances.get(assetId).getSatoshi());
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
        final String tx = getIntent().getStringExtra(INTENT_STRING_TX);
        final ObjectNode txJson = new ObjectMapper().readValue(tx, ObjectNode.class);
        final ObjectNode addressee = (ObjectNode) txJson.get("addressees").get(0);
        addressee.put("asset_tag", assetId);
        return getSession().createTransactionRaw(txJson);
    }
}