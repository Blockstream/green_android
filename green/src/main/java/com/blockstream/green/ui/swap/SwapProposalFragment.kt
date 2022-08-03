package com.blockstream.green.ui.swap

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.SwapProposalFragmentBinding
import com.blockstream.green.extensions.copyToClipboard
import com.blockstream.green.extensions.share
import com.blockstream.green.ui.wallet.AbstractWalletFragment
import com.blockstream.green.utils.toAmountLook
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SwapProposalFragment : AbstractWalletFragment<SwapProposalFragmentBinding>(
    layout = R.layout.swap_proposal_fragment,
    menuRes = 0
) {
    override val isAdjustResize = false

    val args: SwapProposalFragmentArgs by navArgs()
    var link: String? = null

    override val walletOrNull by lazy { args.wallet }

    override val screenName = "SwapProposal"

    @Inject
    lateinit var viewModelFactory: SwapProposalViewModel.AssistedFactory
    val viewModel: SwapProposalViewModel by viewModels {
        SwapProposalViewModel.provideFactory(
            viewModelFactory,
            args.wallet,
            args.proposal
        )
    }

    override fun getWalletViewModel() = viewModel

    override fun onViewCreatedGuarded(view: View, savedInstanceState: Bundle?) {
        binding.vm = viewModel

        viewModel.link.observe(viewLifecycleOwner) {
            link = it
        }

        binding.buttonCopy.setOnClickListener {
            copyToClipboard(label = "Swap Proposal", content = link ?: "", animateView = it, showCopyNotification = true)
        }

        binding.buttonShare.setOnClickListener {
            share(link ?: "")
        }

        val from = args.proposal.inputs[0].let {
            it.amount.toAmountLook(session = session, assetId = it.assetId, withUnit = true)
        }

        val to = args.proposal.outputs[0].let {
            it.amount.toAmountLook(session = session, assetId = it.assetId, withUnit = true)
        }

        binding.swapDescriptionTextView.text = buildSpannedString {

            color(ContextCompat.getColor(requireContext(), R.color.color_on_surface_emphasis_medium)) {
                append("You want to swap ")
            }

            scale(1.1f) {
                bold {
                    color(
                        ContextCompat.getColor(requireContext(), R.color.color_on_surface_emphasis_high)
                    ) {
                        append("$from")
                    }
                }
            }

            color(ContextCompat.getColor(requireContext(), R.color.color_on_surface_emphasis_medium)) {
                append(" to ")
            }

            scale(1.1f) {
                color(ContextCompat.getColor(requireContext(), R.color.color_on_surface_emphasis_high)) {
                    bold {
                        append( "$to")
                    }
                }
            }
            color(ContextCompat.getColor(requireContext(), R.color.color_on_surface_emphasis_medium)) {
                append(".\nYou can share this proposal with peers.")
            }
        }

        viewModel.qrBitmap.observe(viewLifecycleOwner) {
            binding.qrImageView.setImageDrawable(BitmapDrawable(resources, it).also { bitmap ->
                bitmap.isFilterBitmap = false
            })
        }
    }
}
