package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.greenaddress.greenapi.data.AssetInfoData;

public class AssetActivity extends LoggedActivity {

    private static final String TAG = AssetActivity.class.getSimpleName();

    private TextView mIdText;
    private EditText mPrecisionText;
    private EditText mTickerText;
    private EditText mNameText;

    private AssetInfoData mAssetInfo;
    private String mAssetId;
    private Long mSatoshi;

    @Override
    protected int getMainViewId() {
        return R.layout.activity_asset;
    }

    @Override
    protected void onCreateWithService(final Bundle savedInstanceState) {
        if (mService == null || mService.getModel() == null) {
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

        mAssetId = (String) getIntent().getSerializableExtra("ASSET_ID");
        mAssetInfo = (AssetInfoData) getIntent().getSerializableExtra("ASSET_INFO");
        mSatoshi = (Long) getIntent().getSerializableExtra("SATOSHI");

        mIdText.setText(mAssetId);
        if (mAssetInfo != null) {
            mTickerText.setText(mAssetInfo.getTicker() != null ? mAssetInfo.getTicker() : "");
            mNameText.setText(mAssetInfo.getName() != null ? mAssetInfo.getName() : "");
            mPrecisionText.setText(mAssetInfo.getPrecision() != null ? mAssetInfo.getPrecision().toString() : "");
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

    private void refresh() {
        final CardView assetCardView = UI.find(this, R.id.assetCard);
        final TextView txAssetText = assetCardView.findViewById(R.id.assetName);
        final TextView txAssetValue = assetCardView.findViewById(R.id.assetValue);
        final String label = mAssetInfo != null && mAssetInfo.getName() != null ? mAssetInfo.getName() : mAssetId;
        final String amount = mService.getValueString(mSatoshi, false, false);
        final String ticker = mAssetInfo != null && mAssetInfo.getTicker() != null ? mAssetInfo.getTicker() : "";
        txAssetText.setText("btc".equals(mAssetId) ? "L-BTC" : label);
        txAssetValue.setText(String.format("%s %s", amount, ticker));
    }
}