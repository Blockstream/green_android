package com.greenaddress.greenbits.ui;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.greenaddress.greenapi.model.ReceiveAddressObservable;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static android.app.Activity.RESULT_OK;
import static com.greenaddress.greenbits.ui.ScanActivity.INTENT_STRING_TX;
import static com.greenaddress.greenbits.ui.UI.find;

public class AddressDialog extends AppCompatDialogFragment implements Observer {

    private ReceiveAddressObservable mReceiveAddressObservable;
    private View mView;
    private String mAddress;
    private Coin mAmount;

    final Integer[] mChoices =  { R.string.id_generate_new_address, R.string.id_copy_to_clipboard,
                                  R.string.id_share, R.string.id_sweep_from_paper_wallet  };

    final Integer[] mChoicesWatchOnly =  { R.string.id_generate_new_address,
                                           R.string.id_copy_to_clipboard, R.string.id_share };

    final static SparseArray<Integer> mIcons = new SparseArray<>();
    static {
        mIcons.put(R.string.id_generate_new_address, R.drawable.qr_generate);
        mIcons.put(R.string.id_copy_to_clipboard, R.drawable.qr_copy);
        mIcons.put(R.string.id_share, R.drawable.qr_share);
        mIcons.put(R.string.id_sweep_from_paper_wallet, R.drawable.qr_sweep);
    }
    final static SparseArray<Integer> mColors = new SparseArray<>();
    static {
        mColors.put(R.string.id_generate_new_address, R.color.green);
        mColors.put(R.string.id_copy_to_clipboard, R.color.green);
        mColors.put(R.string.id_share, R.color.green);
        mColors.put(R.string.id_sweep_from_paper_wallet, R.color.white);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        final GaService service = ((GreenAddressApplication) getActivity().getApplication()).mService;
        final int subaccount = service.getSession().getCurrentSubaccount();
        mReceiveAddressObservable = service.getModel().getReceiveAddressObservable(subaccount);
        mReceiveAddressObservable.addObserver(this);
        mAddress = mReceiveAddressObservable.getReceiveAddress();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mReceiveAddressObservable.deleteObserver(this);
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        // Get arguments from bundle
        final Bundle b = this.getArguments();
        if (b != null) {
            final long amount = b.getLong("amount");
            mAmount = amount > 0 ? Coin.valueOf(amount) : null;
        }

        // Set style
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.dialog_rounded));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        // Set view
        mView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_address, null);

        // Setup listview of actions

        final List<Integer> ids = new ArrayList<>();
        for (final int resource : isWatchOnly() ? mChoicesWatchOnly : mChoices) {
            ids.add(resource);
        }
        final ChoiceAdapter choiceAdapter = new ChoiceAdapter(getGaActivity(), ids);

        final RecyclerView listView = find(mView, R.id.listView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getGaActivity());
        listView.setLayoutManager(layoutManager);
        listView.setAdapter(choiceAdapter);

        // Set dialog
        return new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialog))
               .setView(mView)
               .setCancelable(true)
               .create();
    }

    private boolean isWatchOnly() {
        final GaService service = ((GreenAddressApplication) getActivity().getApplication()).mService;
        return service != null && service.isWatchOnly();
    }

    @Override
    public void onResume() {
        super.onResume();
        final AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null)
            return;
        onRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void onRefresh() {
        if (mAddress == null)
            return;
        final ImageView qrCode = find(mView, R.id.qrInDialogImageView);
        final TextView addressText = find(mView, R.id.qrInDialogText);
        addressText.setText(mAddress);

        // Set text inside qrCode with amount if defined
        final String qrCodeText;
        if (mAmount != null) {
            final GaService service = ((GreenAddressApplication) getActivity().getApplication()).mService;
            final Address address = Address.fromBase58(service.getNetworkParameters(), mAddress);
            qrCodeText = BitcoinURI.convertToBitcoinURI(address, mAmount, null, null);
        } else
            qrCodeText = mAddress;
        qrCode.setLayoutParams(UI.getScreenLayout(getActivity(), 0.55));
        qrCode.setImageDrawable(UI.getQrBitmapDrawable(getActivity(), qrCodeText));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final String jsonTransaction = data.getStringExtra(INTENT_STRING_TX);
            Log.d("AddressDialog",jsonTransaction);
            final Intent send = new Intent(getActivity(), SendActivity.class);
            send.putExtra(INTENT_STRING_TX, jsonTransaction);
            startActivity(send);
            dismiss();
        }
    }

    @Override
    public void update(final Observable observable, final Object o) {
        if (observable instanceof ReceiveAddressObservable) {
            mAddress = ((ReceiveAddressObservable) observable).getReceiveAddress();
            getGaActivity().runOnUiThread(() -> onRefresh());
        }
    }

    class ChoiceAdapter extends RecyclerView.Adapter<ChoiceAdapter.ViewHolder> {

        private final List<Integer> mChoices;
        private final LayoutInflater mInflater;

        ChoiceAdapter(final Context context, final List<Integer> choices) {
            mInflater = LayoutInflater.from(context);
            mChoices = choices;
        }

        @Override
        public ChoiceAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
            final View view = mInflater.inflate(R.layout.list_element_choice, parent, false);
            return new ChoiceAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ChoiceAdapter.ViewHolder holder, final int position) {
            final Integer resid = mChoices.get(position);
            holder.nameText.setText(resid);
            holder.imageView.setImageResource(mIcons.get(resid));
            holder.nameText.setTextColor(getResources().getColor(mColors.get(resid)));
            holder.itemView.setOnClickListener(new ClickHandler(resid));
        }

        @Override
        public int getItemCount() {
            return mChoices.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView nameText;
            public ImageView imageView;

            ViewHolder(final View itemView) {
                super(itemView);
                nameText = UI.find(itemView, R.id.dialogChoiceText);
                imageView = UI.find(itemView, R.id.dialogChoiceIcon);
            }
        }

        public class ClickHandler implements View.OnClickListener {
            private int res;

            public ClickHandler(int res) {
                this.res = res;
            }

            @Override
            public void onClick(View view) {
                switch (res) {
                case R.string.id_generate_new_address:
                    mReceiveAddressObservable.refresh();
                    break;
                case R.string.id_copy_to_clipboard:
                    final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                        Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("address", mAddress));
                    UI.toast(getGaActivity(), R.string.id_address_copied_to_clipboard, Toast.LENGTH_LONG);
                    break;
                case R.string.id_share:
                    ShareCompat.IntentBuilder
                    .from(getActivity())
                    .setType("text/plain")
                    .setText(mAddress)
                    .setChooserTitle(R.string.id_share_address)
                    .startChooser();
                    break;
                case R.string.id_sweep_from_paper_wallet:
                    final Intent intent = new Intent(getActivity(), ScanActivity.class);
                    intent.setAction("sweep");
                    startActivityForResult(intent, TabbedMainActivity.REQUEST_SEND_QR_SCAN);
                    break;
                }
            }
        }
    }
}
