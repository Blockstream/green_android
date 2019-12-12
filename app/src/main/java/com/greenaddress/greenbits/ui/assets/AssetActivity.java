package com.greenaddress.greenbits.ui.assets;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenaddress.greenapi.data.AssetInfoData;
import com.greenaddress.greenbits.ui.LoggedActivity;
import com.greenaddress.greenbits.ui.R;
import com.greenaddress.greenbits.ui.UI;

/**
 * Activity showing details about an asset (L-BTC excluded)
 */
public class AssetActivity extends LoggedActivity {

    private static final String TAG = AssetActivity.class.getSimpleName();
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    private TextView mIdText;
    private TextView mPrecisionText;
    private TextView mTickerText;
    private TextView mNameText;
    private TextView mDomainText;
    private TextView mAssetBalanceText;

    private AssetInfoData mAssetInfo;
    private String mAssetId;
    private Long mSatoshi;

    @Override
    protected int getMainViewId() {
        return R.layout.activity_asset;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getModel() == null) {
            toFirst();
            return;
        }

        setResult(RESULT_OK);
        UI.preventScreenshots(this);

        setTitleBackTransparent();

        mIdText = UI.find(this, R.id.idText);
        mPrecisionText = UI.find(this, R.id.precisionText);
        mTickerText = UI.find(this, R.id.tickerText);
        mNameText = UI.find(this, R.id.nameText);
        mDomainText = UI.find(this, R.id.domainText);
        mAssetBalanceText = UI.find(this, R.id.assetBalanceText);

        mAssetId = getIntent().getStringExtra("ASSET_ID");
        mAssetInfo = (AssetInfoData) getIntent().getSerializableExtra("ASSET_INFO");
        mSatoshi = getIntent().getLongExtra("SATOSHI", 0L);

        mIdText.setText(mAssetId);
        if (mAssetInfo != null) {
            mIdText.setText(getAssetInfo().getAssetId());
            mTickerText.setText(getAssetInfo().getTicker() == null ? "" : getAssetInfo().getTicker());
            mNameText.setText(getAssetInfo().getName());
            mPrecisionText.setText(
                getAssetInfo().getPrecision() == null ? "0" : getAssetInfo().getPrecision().toString());
            mDomainText.setText(
                getAssetInfo().getEntity().getDomain() == null ? "" : getAssetInfo().getEntity().getDomain());
        } else {
            // only unregistered assets won't have name, ticker, domain
            mNameText.setText(getString(R.string.id_no_registered_name_for_this));
            mPrecisionText.setText("0");
            mTickerText.setText(getString(R.string.id_no_registered_ticker_for_this));
            mDomainText.setText(getString(R.string.id_unknown));
        }
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private AssetInfoData getAssetInfo() {
        final AssetInfoData assetInfoDefault = new AssetInfoData(mAssetId);
        return mAssetInfo == null ? assetInfoDefault : mAssetInfo;
    }

    private void refresh() {
        try {
            final String amount = getModel().getAsset(mSatoshi, mAssetId, getAssetInfo(), true);
            mAssetBalanceText.setText(amount);
        } catch (final Exception e) {
            Log.e(TAG, "Conversion error: " + e.getLocalizedMessage());
        }
    }
}