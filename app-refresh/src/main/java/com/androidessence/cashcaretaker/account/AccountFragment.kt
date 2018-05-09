package com.androidessence.cashcaretaker.account

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.androidessence.cashcaretaker.R
import com.androidessence.cashcaretaker.addaccount.AddAccountDialog
import com.androidessence.cashcaretaker.addtransaction.AddTransactionDialog
import com.androidessence.cashcaretaker.data.CCDatabase
import com.androidessence.cashcaretaker.data.CCRepository
import com.androidessence.cashcaretaker.main.MainController
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_account.*

/**
 * Fragment for displaying a list of acocunts to the user.
 */
class AccountFragment : Fragment() {
    //region Properties
    private val adapter = AccountAdapter()
    private val compositeDisposable = CompositeDisposable()
    private lateinit var viewModel: AccountViewModel

    private val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val database = CCDatabase.getInMemoryDatabase(context!!)
                val repository = CCRepository(database)

                @Suppress("UNCHECKED_CAST")
                return AccountViewModel(repository) as T
            }
        }
    }
    //endregion

    //region Lifecycle Methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(AccountViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_account, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeRecyclerView()

        add_account.setOnClickListener { showAddAccountView() }

        subscribeToAdapterClicks()
        subscribeToAccounts()
        viewModel.fetchAccounts()

        fragmentManager?.addOnBackStackChangedListener { viewModel.clearActionMode() }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.dispose()
    }
    //endregion

    //region Initializations
    private fun initializeRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        accountsRecyclerView.adapter = adapter
        accountsRecyclerView.layoutManager = layoutManager
        accountsRecyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    private fun subscribeToAccounts() {
        val subscription = viewModel.accountList
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { adapter.items = it }

        compositeDisposable.add(subscription)
    }

    private fun subscribeToAdapterClicks() {
        compositeDisposable.addAll(
                adapter.accountClickSubject.subscribe(this::onAccountSelected),
                adapter.accountLongClickSubject.subscribe(this::onAccountLongClicked),
                adapter.withdrawalClickSubject.subscribe(this::onWithdrawalButtonClicked),
                adapter.depositClickSubject.subscribe(this::onDepositButtonClicked)
        )
    }
    //endregion

    //region UI Events
    private fun onWithdrawalButtonClicked(account: Account) {
        val dialog = AddTransactionDialog.newInstance(account.name, true)
        dialog.show(fragmentManager, AddTransactionDialog.FRAGMENT_NAME)
    }

    private fun onDepositButtonClicked(account: Account) {
        val dialog = AddTransactionDialog.newInstance(account.name, false)
        dialog.show(fragmentManager, AddTransactionDialog.FRAGMENT_NAME)
    }

    private fun onAccountSelected(account: Account) {
        (activity as? MainController)?.showTransactions(account.name)
    }

    private fun onAccountLongClicked(account: Account) {
        viewModel.startActionModeForAccount(account, activity as AppCompatActivity)
    }

    private fun showAddAccountView() {
        val dialog = AddAccountDialog()
        dialog.show(fragmentManager, AddAccountDialog.FRAGMENT_NAME)
    }
    //endregion

    companion object {
        val FRAGMENT_NAME: String = AccountFragment::class.java.simpleName

        fun newInstance(): AccountFragment = AccountFragment()
    }
}