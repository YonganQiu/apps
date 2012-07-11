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

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

/**
 * 
 * @author yongan.qiu
 *
 */
public class LocalAccountType extends BaseAccountType {
    private static final String TAG = "LocalAccountType";

    private LocalAccountType(Context context, String resPackageName) {
        this.accountType = AccountTypeManagerImpl.ACCOUNT_TYPE_LOCAL;
        this.dataSet = null;
        this.titleRes = R.string.account_type_local;
        this.iconRes = R.mipmap.ic_internal_local;

        this.resPackageName = resPackageName;
        this.summaryResPackageName = resPackageName;

        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
            addDataKindPhoneticName(context);
            addDataKindNickname(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindStructuredPostal(context);
            addDataKindIm(context);
            addDataKindOrganization(context);
            addDataKindPhoto(context);
            addDataKindNote(context);
            addDataKindWebsite(context);
            //{Added by yongan.qiu on 2012-7-5 begin.
            addDataKindGroupMembership(context);
            //}Added by yongan.qiu end.
            //begin: added by yunzhou.song for authentication
            if(!"1".equals(SystemProperties.get("ro.identification.version"))) {
            //end: added by by yunzhou.song for authentication
            	addDataKindSipAddress(context);
            //begin: added by yunzhou.song for authentication
            }
            //end: added by by yunzhou.song for authentication

            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    public LocalAccountType(Context context) {
        this(context, null);
    }

    /**
     * Used to compare with an {@link ExternalAccountType} built from a test contacts.xml.
     * In order to build {@link DataKind}s with the same resource package name,
     * {@code resPackageName} is injectable.
     */
    static AccountType createForTest(Context context, String resPackageName) {
        return new LocalAccountType(context, resPackageName);
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

    //{Added by yongan.qiu on 2012-7-6 begin.
    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }
    //}Added by yongan.qiu end.

}
