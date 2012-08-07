package com.android.contacts.list;

import java.util.Set;

import com.android.contacts.ContactsSearchManager;
import com.android.contacts.R;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

/**
 * Fragment for the contact list used for browsing contacts.
 * 
 * @author yongan.qiu
 */
public class ContactMultiPickerFragment extends BaseMultiPickerFragment<DefaultContactListAdapter> {

    public ContactMultiPickerFragment(OnPickListener listener, int actionTitle, int actionIcon) {
        super(listener, actionTitle, actionIcon);
    }

    @Override
    protected DefaultContactListAdapter instanceAdapter() {
        return new DefaultContactListAdapter(getActivity());
    }

    @Override
    protected void onCreateListAdapter(DefaultContactListAdapter adapter) {
        adapter.setExcludeUris(getExcludeUris());
    }

    @Override
    protected Uri getUri(int position) {
        return getAdapter().getContactUri(position);
    }
}
