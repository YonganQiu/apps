/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.model;

import com.android.contacts.R;
import com.android.contacts.model.AccountType.DefinitionException;
import com.android.contacts.model.AccountType.EditField;
import com.android.contacts.model.AccountType.EditType;
import com.android.contacts.model.BaseAccountType.EmailActionInflater;
import com.android.contacts.model.BaseAccountType.PhoneActionAltInflater;
import com.android.contacts.model.BaseAccountType.PhoneActionInflater;
import com.android.contacts.model.BaseAccountType.SimpleInflater;
import com.google.android.collect.Lists;

import android.content.ContentValues;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

/**
 * 
 * @author yongan.qiu
 *
 */
public class SimAccountType extends AccountType {
    private static final String TAG = "SimAccountType";

    protected static final int FLAGS_PHONE = EditorInfo.TYPE_CLASS_PHONE;
    protected static final int FLAGS_PERSON_NAME = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS | EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME;

    private SimAccountType(Context context, String resPackageName) {
        this.accountType = AccountTypeManagerImpl.ACCOUNT_TYPE_SIM;
        this.dataSet = null;
        this.titleRes = R.string.account_type_sim;
        this.iconRes = R.mipmap.ic_internal_sim;

        this.resPackageName = resPackageName;
        this.summaryResPackageName = resPackageName;

        try {
            addDataKindStructuredName(context);
            addDataKindPhone(context);
            addDataKindDisplayName(context);
            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    public SimAccountType(Context context) {
        this(context, null);
    }

    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE,
                R.string.nameLabelsGroup, -1, true, R.layout.structured_name_editor_view));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME,
                R.string.full_name, FLAGS_PERSON_NAME));
        return kind;
    }

    protected DataKind addDataKindDisplayName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME,
                R.string.nameLabelsGroup, -1, true, R.layout.text_fields_editor_view));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME,
                R.string.full_name, FLAGS_PERSON_NAME));
        return kind;
    }

    protected DataKind addDataKindPhone(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(Phone.CONTENT_ITEM_TYPE, R.string.phoneLabelsGroup,
                -1, true, R.layout.text_fields_editor_view));
        kind.iconAltRes = R.drawable.ic_text_holo_light;
        kind.iconAltDescriptionRes = R.string.sms;
        kind.typeOverallMax = 1;
        kind.actionHeader = new PhoneActionInflater();
        kind.actionAltHeader = new PhoneActionAltInflater();
        kind.actionBody = new SimpleInflater(Phone.NUMBER);
        /*kind.typeColumn = Phone.TYPE;
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildPhoneType(Phone.TYPE_OTHER));*/

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE));

        return kind;
    }

    /**
     * Used to compare with an {@link ExternalAccountType} built from a test contacts.xml.
     * In order to build {@link DataKind}s with the same resource package name,
     * {@code resPackageName} is injectable.
     */
    static AccountType createForTest(Context context, String resPackageName) {
        return new SimAccountType(context, resPackageName);
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return false;
    }
}
