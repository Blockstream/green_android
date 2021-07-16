package com.greenaddress.greenbits.ui.assets;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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

import static com.greenaddress.greenbits.ui.TabbedMainActivity.REQUEST_BITCOIN_URL_SEND;

public class AssetsSelectActivity extends LoggedActivity implements AssetsAdapter.OnAssetSelected {

    private RecyclerView mRecyclerView;
    private Map<String, Long> mAssetsBalances;
    private Disposable refreshDisposable;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitleBackTransparent();
        setContentView(R.layout.activity_assets_selection);

        final String callingActivity = getCallingActivity() != null ? getCallingActivity().getClassName() : "";
        if (callingActivity.equals(TabbedMainActivity.class.getName())) {
            setTitle(R.string.id_total_balance);
        } else if (callingActivity.equals(ScanActivity.class.getName())) {
            setTitle(R.string.id_select_asset);
        }

        mRecyclerView = findViewById(R.id.assetsList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        refresh();
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
        if (getNetwork().getPolicyAsset().equals(assetId))
            return;

        // Open selected asset detail page
        final Intent intent = new Intent(this, AssetActivity.class);
        final AssetInfoData assetInfo = getSession().getRegistry().getAssetInfo(assetId);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshDisposable != null)
            refreshDisposable.dispose();
    }

    private void refresh() {
        final Integer subaccount = getActiveAccount();
        startLoading();
        refreshDisposable = Observable.just(getSession())
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .map((session) -> {
            return getSession().getBalance(this, subaccount);
        }).subscribe((subaccounts) -> {
            stopLoading();
            mAssetsBalances = subaccounts;
            final AssetsAdapter adapter = new AssetsAdapter(this, mAssetsBalances, getNetwork(), this);
            mRecyclerView.setAdapter(adapter);
        }, (final Throwable e) -> {
            stopLoading();
        });
    }

    private ObjectNode updateTransaction(final String assetId) throws Exception {
        final String tx = getIntent().getStringExtra(PrefKeys.INTENT_STRING_TX);
        final ObjectNode txJson = new ObjectMapper().readValue(tx, ObjectNode.class);
        final ObjectNode addressee = (ObjectNode) txJson.get("addressees").get(0);
        addressee.put("asset_id", assetId);
        final GDKTwoFactorCall call = getSession().createTransactionRaw(null, txJson);
        return call.resolve(new HardwareCodeResolver(this), null);
    }
}