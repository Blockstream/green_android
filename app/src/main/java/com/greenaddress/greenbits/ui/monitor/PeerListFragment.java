package com.greenaddress.greenbits.ui.monitor;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.greenaddress.greenbits.ui.R;

public class PeerListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_peerlist, container, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {

    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {

    }
}
