/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.contacts.editor;

import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.util.InternalsAndAccountsMergeAdapter;
import com.android.contacts.util.InternalsListAdapter;
import com.android.contacts.util.InternalsListAdapter.InternalListFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Shows a dialog asking the user which account to chose.
 *
 * The result is passed to {@code targetFragment} passed to {@link #show}.
 */
public final class SelectAccountDialogFragment extends DialogFragment {
    public static final String TAG = "SelectAccountDialogFragment";

    private static final String KEY_TITLE_RES_ID = "title_res_id";
    //{Modified by yongan.qiu on 2012-7-6 begin.
    //old:
    /*private static final String KEY_LIST_FILTER = "list_filter";*/
    //new:
    private static final String KEY_INTERNAL_LIST_FILTER = "internal_list_filter";
    private static final String KEY_ACCOUNT_LIST_FILTER = "account_list_filter";
    //}Modified by yongan.qiu end.
    private static final String KEY_EXTRA_ARGS = "extra_args";

    public SelectAccountDialogFragment() { // All fragments must have a public default constructor.
    }

    /**
     * Show the dialog.
     *
     * @param fragmentManager {@link FragmentManager}.
     * @param targetFragment {@link Fragment} that implements {@link Listener}.
     * @param titleResourceId resource ID to use as the title.
     * @param accountListFilter account filter.
     * @param extraArgs Extra arguments, which will later be passed to
     *     {@link Listener#onAccountChosen}.  {@code null} will be converted to
     *     {@link Bundle#EMPTY}.
     */
    public static <F extends Fragment & Listener> void show(FragmentManager fragmentManager,
            F targetFragment, int titleResourceId,
            //{Added by yongan.qiu on 2012-7-6 begin.
            InternalListFilter internalListFilter,
            //}Added by yongan.qiu end.
            AccountListFilter accountListFilter, Bundle extraArgs) {
        final Bundle args = new Bundle();
        args.putInt(KEY_TITLE_RES_ID, titleResourceId);
        //{Modified by yongan.qiu on 2012-7-6 begin.
        //old:
        /*args.putSerializable(KEY_LIST_FILTER, accountListFilter);*/
        //new:
        args.putSerializable(KEY_INTERNAL_LIST_FILTER, internalListFilter);
        args.putSerializable(KEY_ACCOUNT_LIST_FILTER, accountListFilter);
        //}Modified by yongan.qiu end.

        args.putBundle(KEY_EXTRA_ARGS, (extraArgs == null) ? Bundle.EMPTY : extraArgs);

        final SelectAccountDialogFragment instance = new SelectAccountDialogFragment();
        instance.setArguments(args);
        instance.setTargetFragment(targetFragment, 0);
        instance.show(fragmentManager, null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Bundle args = getArguments();

        //{Modified by yongan.qiu on 2012-7-6 begin.
        //old:
        /*final AccountListFilter filter = (AccountListFilter) args.getSerializable(KEY_LIST_FILTER);
        final AccountsListAdapter accountAdapter = new AccountsListAdapter(builder.getContext(),
                filter);*/
        //new:
        final InternalListFilter internalFilter = (InternalListFilter) args.getSerializable(KEY_INTERNAL_LIST_FILTER);
        final AccountListFilter accountFilter = (AccountListFilter) args.getSerializable(KEY_ACCOUNT_LIST_FILTER);
        final InternalsListAdapter internalAdapter = new InternalsListAdapter(builder.getContext(), internalFilter);
        final AccountsListAdapter accountAdapter = new AccountsListAdapter(builder.getContext(),
                accountFilter);
        final InternalsAndAccountsMergeAdapter mergeAdapter = new InternalsAndAccountsMergeAdapter(internalAdapter, accountAdapter);
        //}Modified by yongan.qiu end.

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                //{Modified by yongan.qiu on 2012-7-6 begin.
                //old:
                /*onAccountSelected(accountAdapter.getItem(which));*/
                //new:
                onAccountSelected(mergeAdapter.getItem(which));
                //}Modified by yongan.qiu end.
            }
        };

        builder.setTitle(args.getInt(KEY_TITLE_RES_ID));
        //{Modified by yongan.qiu on 2012-7-6 begin.
        //old:
       /*builder.setSingleChoiceItems(accountAdapter, 0, clickListener);*/
        //new:
        builder.setSingleChoiceItems(mergeAdapter, 0, clickListener);
        //}Modified by yongan.qiu end.
        final AlertDialog result = builder.create();
        return result;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null && targetFragment instanceof Listener) {
            final Listener target = (Listener) targetFragment;
            target.onAccountSelectorCancelled();
        }
    }

    /**
     * Calls {@link Listener#onAccountChosen} of {@code targetFragment}.
     */
    private void onAccountSelected(AccountWithDataSet account) {
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null && targetFragment instanceof Listener) {
            final Listener target = (Listener) targetFragment;
            target.onAccountChosen(account, getArguments().getBundle(KEY_EXTRA_ARGS));
        }
    }

    public interface Listener {
        void onAccountChosen(AccountWithDataSet account, Bundle extraArgs);
        void onAccountSelectorCancelled();
    }
}
