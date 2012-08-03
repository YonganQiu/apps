package com.android.contacts.activities;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.list.BaseMultiPickerFragment;
import com.android.contacts.list.BaseMultiPickerFragment.OnDoneListener;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactEntryListFragment;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactMultiPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.EmailAddressMultiPickerFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.list.PhoneNumberMultiPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.android.contacts.util.Constants;
import com.android.contacts.widget.ContextMenuAdapter;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Set;

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 * 
 * @author yongan.qiu
 */
public class MultiActionSelectionActivity extends ContactsActivity
        implements View.OnCreateContextMenuListener {
    private static final String TAG = MultiActionSelectionActivity.class.getSimpleName();
    private static boolean DEBUG = Constants.TOTAL_DEBUG;

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private ContactsIntentResolver mIntentResolver;
    private ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;

    private ContactsRequest mRequest;

    private Parcelable[] mExcludeUris;
    private ContactListFilter mContactListFilter;
    private String mExtraSelection;
    private int mActionTitle;
    private int mActionIcon;

    private static final int DEFAULT_TITLE_RES_ID = R.string.action;
    private static final int DEFAULT_ICON_RES_ID = R.drawable.ic_menu_action_holo_dark;

    public MultiActionSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
        }

        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        configureActivityTitle();

        setContentView(R.layout.multi_action_selection_activity);
        
        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();

            Intent intent = getIntent();
            String extraName = null;
            switch(mActionCode) {
            case ContactsRequest.ACTION_PICK_CONTACT:
                extraName = Intents.EXTRA_CONTACT_URIS;
                break;
            case ContactsRequest.ACTION_PICK_PHONE:
                extraName = Intents.EXTRA_PHONE_URIS;
                break;
            case ContactsRequest.ACTION_PICK_EMAIL:
                extraName = Constants.EXTRA_EMAIL_URIS;
                break;
            }
            if (extraName != null) {
                mExcludeUris = intent.getParcelableArrayExtra(extraName);
            }
            mContactListFilter = (ContactListFilter) intent.getParcelableExtra(Constants.EXTRA_CONTACT_LIST_FILTER);
            mExtraSelection = intent.getStringExtra(Constants.EXTRA_SELECTION);
            mActionTitle = intent.getIntExtra(Constants.EXTRA_ACTION_TITLE, DEFAULT_TITLE_RES_ID);
            mActionIcon = intent.getIntExtra(Constants.EXTRA_ACTION_ICON, DEFAULT_ICON_RES_ID);

            configureListFragment();
        }

        prepareActionBar();

    }

    private void prepareActionBar() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    OnDoneListener mOnDoneListener = new OnDoneListener() {
        @Override
        public void onDone() {
            Set<Uri> selectedUriSet = mListFragment.getSelectedUriSet();
            if (DEBUG) {
                Log.i(TAG, "Selection done. SelectedUriSet = " + selectedUriSet);
            }
            if(selectedUriSet != null && !selectedUriSet.isEmpty()) {
                Uri[] uris = new Uri[selectedUriSet.size()];
                selectedUriSet.toArray(uris);
                String extraName = null;
                switch(mActionCode) {
                case ContactsRequest.ACTION_PICK_CONTACT:
                    extraName = Intents.EXTRA_CONTACT_URIS;
                    break;
                case ContactsRequest.ACTION_PICK_PHONE:
                    extraName = Intents.EXTRA_PHONE_URIS;
                    break;
                case ContactsRequest.ACTION_PICK_EMAIL:
                    extraName = Constants.EXTRA_EMAIL_URIS;
                    break;
                }
                if (extraName != null) {
                    Intent intent = new Intent();
                    intent.putExtra(extraName, uris);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                } else {
                    Log.e(TAG, "Illegal state!");
                }
            } else {
                Log.w(TAG, "No item selected, action canceled!");
            }
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to previous screen, intending "cancel"
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
    }

    private void configureActivityTitle() {
        if (mRequest.getActivityTitle() != null) {
            setTitle(mRequest.getActivityTitle());
            return;
        }

        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_PICK_CONTACT: {
                setTitle(R.string.multi_action_selection_activity_title);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                setTitle(R.string.multi_action_selection_activity_title);
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                setTitle(R.string.multi_action_selection_activity_title);
                break;
            }
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        BaseMultiPickerFragment<? extends ContactEntryListAdapter> fragment;
        switch (mActionCode) {
            case ContactsRequest.ACTION_PICK_CONTACT: {
                fragment = new ContactMultiPickerFragment(
                        mOnDoneListener, mActionTitle, mActionIcon);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                fragment = new PhoneNumberMultiPickerFragment(
                        mOnDoneListener, mActionTitle, mActionIcon);
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                fragment = new EmailAddressMultiPickerFragment(
                        mOnDoneListener, mActionTitle, mActionIcon);
                break;
            }

            default:
                throw new IllegalStateException("Invalid action code: " + mActionCode);
        }

        fragment.setExcludeUris(mExcludeUris);
        if (mContactListFilter != null) {
            fragment.setFilter(mContactListFilter);
        }

        fragment.setExtraSelection(mExtraSelection);
        fragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        fragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getFragmentManager().beginTransaction()
                .replace(R.id.list_container, fragment)
                .commitAllowingStateLoss();

        mListFragment = fragment;
    }
}
