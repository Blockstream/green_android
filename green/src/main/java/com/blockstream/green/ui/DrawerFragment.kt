package com.blockstream.green.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import com.blockstream.green.*
import com.blockstream.green.databinding.DrawerFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.migration.OptionalInject
import dagger.hilt.android.migration.OptionalInjectCheck.wasInjectedByHilt

@OptionalInject
@AndroidEntryPoint
class DrawerFragment : WalletListCommonFragment<DrawerFragmentBinding>(R.layout.drawer_fragment, menuRes = 0) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vm = viewModel

        init(binding.common)

        binding.buttonConnectionSettings.setOnClickListener {
            navigate(NavGraphDirections.actionGlobalConnectionSettingsDialogFragment())
            closeDrawer()
        }
    }
}