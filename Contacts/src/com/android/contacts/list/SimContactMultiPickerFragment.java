package com.android.contacts.list;

import com.android.contacts.sim.SIMContactsListAdapter;
import com.android.contacts.sim.SIMCursorLoader;

import android.content.CursorLoader;
import android.net.Uri;

public class SimContactMultiPickerFragment extends BaseMultiPickerFragment<SIMContactsListAdapter> {

    public SimContactMultiPickerFragment(
            com.android.contacts.list.BaseMultiPickerFragment.OnPickListener listener,
            int actionTitle, int actionIcon) {
        super(listener, actionTitle, actionIcon);
    }

    @Override
    protected SIMContactsListAdapter instanceAdapter() {
        return new SIMContactsListAdapter(getActivity());
    }

    @Override
    protected Uri getUri(int position) {
        return getAdapter().getDataUri(position);
    }

    @Override
    public CursorLoader createCursorLoader() {
        return new SIMCursorLoader(getActivity());
    }
}
