package com.androidessence.cashcaretaker.activities;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.androidessence.cashcaretaker.R;
import com.androidessence.cashcaretaker.adapters.TransactionAdapter;
import com.androidessence.cashcaretaker.core.CoreActivity;
import com.androidessence.cashcaretaker.data.CCContract;
import com.androidessence.cashcaretaker.dataTransferObjects.Account;
import com.androidessence.cashcaretaker.dataTransferObjects.Transaction;
import com.androidessence.cashcaretaker.dataTransferObjects.TransactionDetails;
import com.androidessence.cashcaretaker.fragments.AccountTransactionsFragment;
import com.androidessence.cashcaretaker.fragments.TransactionFragment;

/**
 * Activity that allows the user to add, edit, and view transactions.
 *
 * Created by adam.mcneilly on 9/5/16.
 */
public class TransactionsActivityR extends CoreActivity implements AccountTransactionsFragment.OnAddTransactionFABClickedListener, TransactionFragment.OnTransactionSubmittedListener, TransactionAdapter.OnTransactionLongClickListener{
    private AppBarLayout appBar;
    private ActionMode actionMode;

    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.transaction_context_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete_transaction:
                    // The transaction that was selected is passed as the tag
                    // for the action mode.
                    showDeleteAlertDialog((Transaction) actionMode.getTag());
                    mode.finish(); // Action picked, so close the CAB
                    return true;
                case R.id.action_edit_transaction:
                    // Edit the transaction selected. Close CAB when done.
                    showEditTransactionFragment((TransactionDetails) actionMode.getTag());
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }
    };

    public static final String ARG_ACCOUNT = "account";
    private static final String ARG_VIEW_STATE = "viewState";

    private Account account;
    private ViewStates viewState;

    private TransactionFragment transactionFragment;
    private AccountTransactionsFragment accountTransactionsFragment;

    private enum ViewStates {
        VIEW,
        ADD,
        EDIT
    }

    private void showDeleteAlertDialog(final Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete " + transaction.getDescription() + "?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getContentResolver().delete(
                                CCContract.TransactionEntry.CONTENT_URI,
                                CCContract.TransactionEntry._ID + " = ?",
                                new String[] { String.valueOf(transaction.getIdentifier()) }
                        );

                        accountTransactionsFragment.restartAccountBalanceLoader();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        appBar = (AppBarLayout) findViewById(R.id.appbar);

        setupToolbar(true);

        // Read arguments
        viewState = (savedInstanceState != null && savedInstanceState.containsKey(ARG_VIEW_STATE))
                ? (ViewStates) savedInstanceState.getSerializable(ARG_VIEW_STATE)
                : ViewStates.VIEW;
        account = getIntent().getParcelableExtra(ARG_ACCOUNT); //TODO: Bad assumption?

        // Setup fragment depending on view state
        switch(viewState) {
            case VIEW:
                // When showing transactions we also show account balance, so remove elevation
                // on app bar. Requires API 21
                setAppBarElevation(false);
                AccountTransactionsFragment accountTransactionsFragment = (AccountTransactionsFragment) getSupportFragmentManager().findFragmentByTag(AccountTransactionsFragment.FRAGMENT_TAG);
                if(accountTransactionsFragment == null) {
                    showTransactionsFragment();
                }
                break;
            case ADD:
                setAppBarElevation(true);
                TransactionFragment transactionFragment = (TransactionFragment) getSupportFragmentManager().findFragmentByTag(TransactionFragment.FRAGMENT_TAG_ADD);
                if(transactionFragment == null) {
                    showAddTransactionFragment();
                }
                break;
            case EDIT:
                setAppBarElevation(true);
                TransactionFragment editTransactionFragment = (TransactionFragment) getSupportFragmentManager().findFragmentByTag(TransactionFragment.FRAGMENT_TAG_EDIT);
                if(editTransactionFragment == null) {
                    showAddTransactionFragment();
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown transaction state: " + viewState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_VIEW_STATE, viewState);
    }

    private void showAddTransactionFragment() {
        transactionFragment = TransactionFragment.NewInstance(account.getIdentifier(), TransactionFragment.MODE_ADD, null);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_transactions, transactionFragment, TransactionFragment.FRAGMENT_TAG_ADD).commit();

        setToolbarTitle(getString(R.string.add_transaction));

        viewState = ViewStates.ADD;

        setAppBarElevation(true);
    }

    private void showEditTransactionFragment(TransactionDetails transactionDetails) {
        transactionFragment = TransactionFragment.NewInstance(account.getIdentifier(), TransactionFragment.MODE_EDIT, transactionDetails);

        setToolbarTitle(getString(R.string.edit_transaction));

        viewState = ViewStates.EDIT;

        setAppBarElevation(true);
    }

    private void showTransactionsFragment() {
        accountTransactionsFragment = AccountTransactionsFragment.NewInstance(account.getIdentifier());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_transactions, accountTransactionsFragment, AccountTransactionsFragment.FRAGMENT_TAG).commit();

        setToolbarTitle(account.getName() + " Transactions");

        viewState = ViewStates.VIEW;

        setAppBarElevation(false);
    }

    @Override
    public void addTransactionFABClicked() {
        // Close action mode if necessary
        if(actionMode != null) {
            actionMode.finish();
        }
        showAddTransactionFragment();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // If we are in view state, finish to go back to accounts
                if (viewState == ViewStates.VIEW) {
                    finish();
                } else {
                    // If we are in ADD or EDIT, show the transactions fragment again
                    showTransactionsFragment();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTransactionSubmitted() {
        showTransactionsFragment();
    }

    @Override
    public void onTransactionLongClick(TransactionDetails transaction) {
        startActionMode(transaction);
    }

    private void startActionMode(TransactionDetails transactionDetails) {
        // Don't fire if already being used
        if(actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
            actionMode.setTitle(transactionDetails.getDescription());
            actionMode.setTag(transactionDetails);
        }
    }

    private void setAppBarElevation(boolean showElevation) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBar.setElevation(showElevation
                    ? getResources().getDimension(R.dimen.appbar_elevation)
                    : 0);
        }
    }
}