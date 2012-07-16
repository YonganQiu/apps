/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License.
 */
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
 */
public class EmailAddressPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter> {
    private OnEmailAddressPickerActionListener mListener;

    //{Added by yongan.qiu on 2012-7-16 begin.
    private ContactListFilter mFilter;

    public void setFilter(ContactListFilter filter) {
        mFilter = filter;
    }
    //}Added by yongan.qiu end.

    public EmailAddressPickerFragment() {
        setQuickContactEnabled(false);
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_DATA_SHORTCUT);
    }

    public void setOnEmailAddressPickerActionListener(OnEmailAddressPickerActionListener listener) {
        mListener = listener;
    }

    @Override
    protected void onItemClick(int position, long id) {
        EmailAddressListAdapter adapter = (EmailAddressListAdapter)getAdapter();
        //{Added by yongan.qiu on 2012-7-16 begin.
        Uri dataUri = adapter.getDataUri(position);
        if (dataUri != null) {
            ListView listView = getListView();
            if(listView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
                int adjPosition = position + listView.getHeaderViewsCount();
                boolean isChecked = listView.isItemChecked(adjPosition);

                Set<Uri> selectedUriSet = getSelectedUriSet();
                if(isChecked) {
                    selectedUriSet.add(dataUri);
                } else {
                    selectedUriSet.remove(dataUri);
                }

                View buttonsLayout = getActivity().findViewById(R.id.layout_bottom);
                if(buttonsLayout != null) {
                    Button okBtn = (Button)buttonsLayout.findViewById(R.id.btn_ok);
                    if(okBtn != null) {
                        okBtn.setEnabled(!selectedUriSet.isEmpty());
                    }
                }
            }
        }
        //}Added by yongan.qiu end.
        pickEmailAddress(adapter.getDataUri(position));
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        EmailAddressListAdapter adapter = new EmailAddressListAdapter(getActivity());
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        //{Added by yongan.qiu on 2012-7-16 begin.
        if (mFilter != null) {
            adapter.setFilter(mFilter);
        }
        //}Added by yongan.qiu end.
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    private void pickEmailAddress(Uri uri) {
        mListener.onPickEmailAddressAction(uri);
    }
}
