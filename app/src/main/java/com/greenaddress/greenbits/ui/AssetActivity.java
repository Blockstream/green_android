package com.greenaddress.greenbits.ui;

import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.greenaddress.greenapi.data.AssetInfoData;

import static com.greenaddress.gdk.GDKSession.getSession;

public class AssetActivity extends LoggedActivity {

    private static final String TAG = AssetActivity.class.getSimpleName();
    private static final ObjectMapper mObjectMapper = new ObjectMapper();

    private TextView mIdText;
    private EditText mPrecisionText;
    private EditText mTickerText;
    private EditText mNameText;

    private AssetInfoData mAssetInfo;
    private String mAssetId;
    private Long mSatoshi;
    private String mNeg;

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

        mAssetId = getIntent().getStringExtra("ASSET_ID");
        mAssetInfo = (AssetInfoData) getIntent().getSerializableExtra("ASSET_INFO");
        mSatoshi = getIntent().getLongExtra("SATOSHI", 0L);
        mNeg = getIntent().getBooleanExtra("SATOSHI_NEG", false) ? "-" : "";

        mIdText.setText(mAssetId);
        if (mAssetInfo != null) {
            mIdText.setText(getAssetInfo().getAssetId());
            mTickerText.setText(getAssetInfo().getTicker() == null ? "" : getAssetInfo().getTicker());
            mNameText.setText("btc".equals(mAssetId) ? "L-BTC" : getAssetInfo().getName());
            mPrecisionText.setText(
                getAssetInfo().getPrecision() == null ? "0" : getAssetInfo().getPrecision().toString());
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
        final AssetInfoData assetInfoDefault = new AssetInfoData(mAssetId, mAssetId, 0, "");
        return mAssetInfo == null ? assetInfoDefault : mAssetInfo;
    }

    private void refresh() {
        final CardView assetCardView = UI.find(this, R.id.assetCard);
        final TextView txAssetText = assetCardView.findViewById(R.id.assetName);
        final TextView txAssetValue = assetCardView.findViewById(R.id.assetValue);
        final String ticker = "btc".equals(mAssetId) ? "L-BTC" : getAssetInfo().getTicker() ==
                              null ? "" : getAssetInfo().getTicker();
        try {
            final ObjectNode details = mObjectMapper.createObjectNode();
            details.put("satoshi", mSatoshi);
            details.set("asset_info",  getAssetInfo().toObjectNode());
            final ObjectNode converted = getSession().convert(details);
            final String amount = converted.get(mAssetId).asText();
            txAssetText.setText("btc".equals(mAssetId) ? "L-BTC" :  getAssetInfo().getName());
            txAssetValue.setText(String.format("%s%s %s", mNeg, amount, ticker));
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}