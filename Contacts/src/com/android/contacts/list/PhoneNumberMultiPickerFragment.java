package com.android.contacts.list;

import java.util.Set;

import com.android.contacts.R;
import com.android.contacts.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.util.AccountFilterUtil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

/**
 * Fragment containing a phone number list for picking.
 * 
 * @author yongan.qiu
 */
public class PhoneNumberMultiPickerFragment extends BaseMultiPickerFragment<PhoneNumberListAdapter> {

    public PhoneNumberMultiPickerFragment(OnPickListener listener, int actionTitle, int actionIcon) {
        super(listener, actionTitle, actionIcon);
    }

    @Override
    protected PhoneNumberListAdapter instanceAdapter() {
        return new PhoneNumberListAdapter(getActivity());
    }

    @Override
    protected void onCreateListAdapter(PhoneNumberListAdapter adapter) {
        adapter.setExcludeUris(getExcludeUris());
    }

    @Override
    protected Uri getUri(int position) {
        return getAdapter().getDataUri(position);
    }
}
