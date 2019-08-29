package com.greenaddress.greenbits.ui;

import android.content.Context;
import androidx.fragment.app.Fragment;

import com.greenaddress.greenapi.model.Model;
import com.greenaddress.greenbits.GaService;
import com.greenaddress.greenbits.GreenAddressApplication;

public abstract class GAFragment extends Fragment {
    private GreenAddressApplication mApp;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        mApp = (GreenAddressApplication) getActivity().getApplication();

        try {
            context.getTheme().applyStyle(ThemeUtils.getThemeFromNetworkId(mApp.mService.getNetwork(), context, null),
                                          true);
        } catch (final Exception e) {
            // Some reports show NullPointer Exception in applying style
            // Applying theme is not mandatory, doing nothing here
        }
    }

    protected boolean isDisconnected() {
        return getGAService() != null && getGAService().isDisconnected();
    }

    protected GaActivity getGaActivity() {
        return (GaActivity) getActivity();
    }

    protected GaService getGAService() {
        return mApp.mService;
    }

    protected Model getModel() {
        return mApp.mService.getModel();
    }
}
