package com.android.contacts.list;

import java.util.Set;

import com.android.contacts.R;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

/**
 * Fragment containing an email list for picking.
 * 
 * @author yongan.qiu
 */
public class EmailAddressMultiPickerFragment extends BaseMultiPickerFragment<EmailAddressListAdapter> {

    public EmailAddressMultiPickerFragment(OnDoneListener listener, int actionTitle, int actionIcon) {
        super(listener, actionTitle, actionIcon);
    }

    @Override
    protected EmailAddressListAdapter instanceAdapter() {
        return new EmailAddressListAdapter(getActivity());
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected Uri getUri(int position) {
        return getAdapter().getDataUri(position);
    }
}
