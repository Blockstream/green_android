package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockstream.gdk.data.Account
import com.blockstream.green.R
import com.blockstream.green.databinding.AccountCardLayoutBinding
import com.blockstream.green.databinding.ListItemAccountsBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.extensions.bind
import com.blockstream.green.views.AccordionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mu.KLogging


data class AccountsListItem constructor(
    val session: GdkSession,
    val accounts: List<Account>,
    val showArrow : Boolean,
    val show2faActivation : Boolean,
    val showCopy : Boolean,
    val expandedAccount: StateFlow<Account?>? = null,
    val listener: AccordionListener? = null
) : AbstractBindingItem<ListItemAccountsBinding>() {
    override val type: Int
        get() = R.id.fastadapter_accounts_item_id

    init {
        identifier = "AccountsListItem".hashCode().toLong()
    }

    override fun createScope(): CoroutineScope {
        return session.createScope(dispatcher = Dispatchers.Main)
    }

    override fun bindView(binding: ListItemAccountsBinding, payloads: List<Any>) {
        val context = binding.root.context
        val layoutInflater = LayoutInflater.from(context)
        val stack = binding.stack

        stack.accordionListener = object: AccordionListener{
            override fun expandListener(view: View, position: Int) {
                listener?.expandListener(view, position)
            }

            override fun arrowClickListener(view: View, position: Int) { }

            override fun copyClickListener(view: View, position: Int) {}

            override fun warningClickListener(view: View, position: Int) { }

            override fun longClickListener(view: View, position: Int) {
                listener?.longClickListener(view, position)
            }
        }

        while (stack.childCount > accounts.size) {
            stack.removeViewAt(stack.childCount - 1)
        }

        while (stack.childCount < accounts.size) {
            stack.addBinding(AccountCardLayoutBinding.inflate(layoutInflater))
        }

        accounts.forEachIndexed { index, account ->
            stack.getBinding<AccountCardLayoutBinding>(index)?.also { binding ->
                binding.bind(scope = scope, account = account, session = session, showArrow = showArrow, showCopy = showCopy, show2faActivation = show2faActivation)

                binding.arrow.setOnClickListener {
                    listener?.arrowClickListener(it, index)
                }

                binding.buttonCopy.setOnClickListener {
                    listener?.copyClickListener(it, index)
                }

                binding.warningIcon.setOnClickListener {
                    listener?.warningClickListener(it, index)
                }
            }
        }

        expandedAccount?.onEach { accountOrNull ->
            accountOrNull?.also { account ->
                accounts.indexOfFirst { it.id == account.id }.also { position ->
                    if(position != -1 && position != stack.getExpanded()){
                        stack.setExpanded(position, fireListener = false, animate = false)
                    }
                }
            }
        }?.launchIn(scope)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAccountsBinding {
        return ListItemAccountsBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}