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

package com.android.contacts.util;

import android.text.TextUtils;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 
 * @author yongan.qiu
 *
 */
public class SimpleHanziToPinyin {
    private static final String TAG = "HanziToPinyin";

    // Turn on this flag when we want to check internal data structure.
    private static final boolean DEBUG = false;

    /**
     * Unihans array. Each unihans is the first one within same pinyin. Use it to determine pinyin
     * for all ~20k unihans.
     */
    public static final char[] UNIHANS = {
            '\u5475', '\u54ce', '\u5b89', '\u80ae', '\u51f9',
            '\u516b', '\u6300', '\u6273', '\u90a6', '\u5305', '\u5351', '\u5954', '\u4f3b',
            '\u5c44', '\u8fb9', '\u6807', '\u618b', '\u90a0', '\u69df', '\u7676', '\u5cec',
            '\u5693', '\u5a47', '\u98e1', '\u4ed3', '\u64cd', '\u518a', '\u5d7e', '\u564c',
            '\u53c9', '\u9497', '\u8fbf', '\u4f25', '\u6284', '\u8f66', '\u62bb', '\u67fd',
            '\u5403', '\u5145', '\u62bd', '\u51fa', '\u6b3b', '\u63e3', '\u5ddd', '\u75ae',
            '\u5439', '\u6776', '\u9034', '\u75b5', '\u5306', '\u51d1', '\u7c97', '\u6c46',
            '\u5d14', '\u90a8', '\u6413', '\u5491', '\u5927', '\u75b8', '\u5f53', '\u5200',
            '\u6dc2', '\u5f97', '\u6265', '\u706f', '\u6c10', '\u55f2', '\u7538', '\u5201',
            '\u7239', '\u4ec3', '\u4e1f', '\u4e1c', '\u5517', '\u561f', '\u5073', '\u5806',
            '\u9413', '\u591a', '\u5a40', '\u8bf6', '\u5940', '\u97a5', '\u800c', '\u53d1',
            '\u5e06', '\u65b9', '\u98de', '\u5206', '\u4e30', '\u8985', '\u4ecf', '\u7d11',
            '\u4f15', '\u65ee', '\u8be5', '\u7518', '\u5188', '\u768b', '\u6208', '\u7d66',
            '\u6839', '\u5e9a', '\u5de5', '\u52fe', '\u4f30', '\u74dc', '\u7f6b', '\u5173',
            '\u5149', '\u5f52', '\u886e', '\u5459', '\u54c8', '\u54b3', '\u9878', '\u82c0',
            '\u84bf', '\u8bc3', '\u9ed2', '\u62eb', '\u4ea8', '\u5677', '\u543d', '\u9f41',
            '\u5322', '\u82b1', '\u6000', '\u72bf', '\u5ddf', '\u7070', '\u660f', '\u5419',
            '\u4e0c', '\u52a0', '\u620b', '\u6c5f', '\u827d', '\u9636', '\u5dfe', '\u52a4',
            '\u5182', '\u52fc', '\u530a', '\u5a1f', '\u5658', '\u519b', '\u5494', '\u5f00',
            '\u520a', '\u95f6', '\u5c3b', '\u533c', '\u524b', '\u80af', '\u962c', '\u7a7a',
            '\u62a0', '\u5233', '\u5938', '\u84af', '\u5bbd', '\u5321', '\u4e8f', '\u5764',
            '\u6269', '\u5783', '\u6765', '\u5170', '\u5577', '\u635e', '\u4ec2', '\u52d2',
            '\u5844', '\u5215', '\u5006', '\u5941', '\u826f', '\u64a9', '\u5217', '\u62ce',
            '\u3007', '\u6e9c', '\u9f99', '\u779c', '\u565c', '\u5a08', '\u7567', '\u62a1',
            '\u7f57', '\u5463', '\u5988', '\u973e', '\u5ada', '\u9099', '\u732b', '\u9ebc',
            '\u6c92', '\u95e8', '\u753f', '\u54aa', '\u7720', '\u55b5', '\u54a9', '\u6c11',
            '\u540d', '\u8c2c', '\u6478', '\u54de', '\u6bea', '\u62cf', '\u5b7b', '\u56e1',
            '\u56ca', '\u5b6c', '\u8bb7', '\u9981', '\u6041', '\u80fd', '\u59ae', '\u62c8',
            '\u5b22', '\u9e1f', '\u634f', '\u60a8', '\u5b81', '\u599e', '\u519c', '\u7fba',
            '\u5974', '\u597b', '\u8650', '\u632a', '\u5594', '\u8bb4', '\u8db4', '\u62cd',
            '\u7705', '\u4e53', '\u629b', '\u5478', '\u55b7', '\u5309', '\u4e15', '\u504f',
            '\u527d', '\u6c15', '\u59d8', '\u4e52', '\u948b', '\u5256', '\u4ec6', '\u4e03',
            '\u6390', '\u5343', '\u545b', '\u6084', '\u767f', '\u4fb5', '\u9751', '\u909b',
            '\u4e18', '\u66f2', '\u5f2e', '\u7f3a', '\u590b', '\u5465', '\u7a63', '\u5a06',
            '\u60f9', '\u4eba', '\u6254', '\u65e5', '\u8338', '\u53b9', '\u5982', '\u5827',
            '\u6875', '\u95f0', '\u82e5', '\u4ee8', '\u6be2', '\u4e09', '\u6852', '\u63bb',
            '\u8272', '\u68ee', '\u50e7', '\u6740', '\u7b5b', '\u5c71', '\u4f24', '\u5f30',
            '\u5962', '\u7533', '\u5347', '\u5c38', '\u53ce', '\u4e66', '\u5237', '\u6454',
            '\u95e9', '\u53cc', '\u8c01', '\u542e', '\u5981', '\u53b6', '\u5fea', '\u635c',
            '\u82cf', '\u72fb', '\u590a', '\u5b59', '\u5506', '\u4ed6', '\u82d4', '\u574d',
            '\u94f4', '\u5932', '\u5fd1', '\u71a5', '\u5254', '\u5929', '\u4f7b', '\u5e16',
            '\u5385', '\u56f2', '\u5077', '\u92c0', '\u6e4d', '\u63a8', '\u541e', '\u6258',
            '\u6316', '\u6b6a', '\u5f2f', '\u5c2a', '\u5371', '\u586d', '\u7fc1', '\u631d',
            '\u5140', '\u5915', '\u867e', '\u4eda', '\u4e61', '\u7071', '\u4e9b', '\u5fc3',
            '\u661f', '\u51f6', '\u4f11', '\u65f4', '\u8f69', '\u75b6', '\u52cb', '\u4e2b',
            '\u6079', '\u592e', '\u5e7a', '\u8036', '\u4e00', '\u6b2d', '\u5e94', '\u54df',
            '\u4f63', '\u4f18', '\u625c', '\u9e22', '\u66f0', '\u6655', '\u531d', '\u707d',
            '\u7ccc', '\u7242', '\u50ae', '\u5219', '\u8d3c', '\u600e', '\u5897', '\u5412',
            '\u635a', '\u6cbe', '\u5f20', '\u948a', '\u8707', '\u8d1e', '\u4e89', '\u4e4b',
            '\u4e2d', '\u5dde', '\u6731', '\u6293', '\u8de9', '\u4e13', '\u5986', '\u96b9',
            '\u5b92', '\u5353', '\u5b5c', '\u5b97', '\u90b9', '\u79df', '\u94bb', '\u539c',
            '\u5c0a', '\u6628', };

    /**
     * Pinyin array. Each pinyin is corresponding to unihans of same offset in the unihans array.
     */
    public static final byte[][] PINYINS = {
        { 97, 0, 0, 0, 0, 0 }, { 97, 105, 0, 0, 0, 0 }, { 97, 110, 0, 0, 0, 0 },
        { 97, 110, 103, 0, 0, 0 }, { 97, 111, 0, 0, 0, 0 }, { 98, 97, 0, 0, 0, 0 },
        { 98, 97, 105, 0, 0, 0 }, { 98, 97, 110, 0, 0, 0 }, { 98, 97, 110, 103, 0, 0 },
        { 98, 97, 111, 0, 0, 0 }, { 98, 101, 105, 0, 0, 0 }, { 98, 101, 110, 0, 0, 0 },
        { 98, 101, 110, 103, 0, 0 }, { 98, 105, 0, 0, 0, 0 }, { 98, 105, 97, 110, 0, 0 },
        { 98, 105, 97, 111, 0, 0 }, { 98, 105, 101, 0, 0, 0 }, { 98, 105, 110, 0, 0, 0 },
        { 98, 105, 110, 103, 0, 0 }, { 98, 111, 0, 0, 0, 0 }, { 98, 117, 0, 0, 0, 0 },
        { 99, 97, 0, 0, 0, 0 }, { 99, 97, 105, 0, 0, 0 },
        { 99, 97, 110, 0, 0, 0 }, { 99, 97, 110, 103, 0, 0 }, { 99, 97, 111, 0, 0, 0 },
        { 99, 101, 0, 0, 0, 0 }, { 99, 101, 110, 0, 0, 0 }, { 99, 101, 110, 103, 0, 0 },
        { 99, 104, 97, 0, 0, 0 }, { 99, 104, 97, 105, 0, 0 }, { 99, 104, 97, 110, 0, 0 },
        { 99, 104, 97, 110, 103, 0 }, { 99, 104, 97, 111, 0, 0 }, { 99, 104, 101, 0, 0, 0 },
        { 99, 104, 101, 110, 0, 0 }, { 99, 104, 101, 110, 103, 0 }, { 99, 104, 105, 0, 0, 0 },
        { 99, 104, 111, 110, 103, 0 }, { 99, 104, 111, 117, 0, 0 }, { 99, 104, 117, 0, 0, 0 },
        { 99, 104, 117, 97, 0, 0 }, { 99, 104, 117, 97, 105, 0 }, { 99, 104, 117, 97, 110, 0 },
        { 99, 104, 117, 97, 110, 103 }, { 99, 104, 117, 105, 0, 0 }, { 99, 104, 117, 110, 0, 0 },
        { 99, 104, 117, 111, 0, 0 }, { 99, 105, 0, 0, 0, 0 }, { 99, 111, 110, 103, 0, 0 },
        { 99, 111, 117, 0, 0, 0 }, { 99, 117, 0, 0, 0, 0 }, { 99, 117, 97, 110, 0, 0 },
        { 99, 117, 105, 0, 0, 0 }, { 99, 117, 110, 0, 0, 0 }, { 99, 117, 111, 0, 0, 0 },
        { 100, 97, 0, 0, 0, 0 }, { 100, 97, 105, 0, 0, 0 }, { 100, 97, 110, 0, 0, 0 },
        { 100, 97, 110, 103, 0, 0 }, { 100, 97, 111, 0, 0, 0 }, { 100, 101, 0, 0, 0, 0 },
        { 100, 101, 105, 0, 0, 0 }, { 100, 101, 110, 0, 0, 0 }, { 100, 101, 110, 103, 0, 0 },
        { 100, 105, 0, 0, 0, 0 }, { 100, 105, 97, 0, 0, 0 }, { 100, 105, 97, 110, 0, 0 },
        { 100, 105, 97, 111, 0, 0 }, { 100, 105, 101, 0, 0, 0 }, { 100, 105, 110, 103, 0, 0 },
        { 100, 105, 117, 0, 0, 0 }, { 100, 111, 110, 103, 0, 0 }, { 100, 111, 117, 0, 0, 0 },
        { 100, 117, 0, 0, 0, 0 }, { 100, 117, 97, 110, 0, 0 }, { 100, 117, 105, 0, 0, 0 },
        { 100, 117, 110, 0, 0, 0 }, { 100, 117, 111, 0, 0, 0 }, { 101, 0, 0, 0, 0, 0 },
        { 101, 105, 0, 0, 0, 0 }, { 101, 110, 0, 0, 0, 0 }, { 101, 110, 103, 0, 0, 0 },
        { 101, 114, 0, 0, 0, 0 }, { 102, 97, 0, 0, 0, 0 }, { 102, 97, 110, 0, 0, 0 },
        { 102, 97, 110, 103, 0, 0 }, { 102, 101, 105, 0, 0, 0 }, { 102, 101, 110, 0, 0, 0 },
        { 102, 101, 110, 103, 0, 0 }, { 102, 105, 97, 111, 0, 0 }, { 102, 111, 0, 0, 0, 0 },
        { 102, 111, 117, 0, 0, 0 }, { 102, 117, 0, 0, 0, 0 }, { 103, 97, 0, 0, 0, 0 },
        { 103, 97, 105, 0, 0, 0 }, { 103, 97, 110, 0, 0, 0 }, { 103, 97, 110, 103, 0, 0 },
        { 103, 97, 111, 0, 0, 0 }, { 103, 101, 0, 0, 0, 0 }, { 103, 101, 105, 0, 0, 0 },
        { 103, 101, 110, 0, 0, 0 }, { 103, 101, 110, 103, 0, 0 }, { 103, 111, 110, 103, 0, 0 },
        { 103, 111, 117, 0, 0, 0 }, { 103, 117, 0, 0, 0, 0 }, { 103, 117, 97, 0, 0, 0 },
        { 103, 117, 97, 105, 0, 0 }, { 103, 117, 97, 110, 0, 0 }, { 103, 117, 97, 110, 103, 0 },
        { 103, 117, 105, 0, 0, 0 }, { 103, 117, 110, 0, 0, 0 }, { 103, 117, 111, 0, 0, 0 },
        { 104, 97, 0, 0, 0, 0 }, { 104, 97, 105, 0, 0, 0 }, { 104, 97, 110, 0, 0, 0 },
        { 104, 97, 110, 103, 0, 0 }, { 104, 97, 111, 0, 0, 0 }, { 104, 101, 0, 0, 0, 0 },
        { 104, 101, 105, 0, 0, 0 }, { 104, 101, 110, 0, 0, 0 }, { 104, 101, 110, 103, 0, 0 },
        { 104, 109, 0, 0, 0, 0 }, { 104, 111, 110, 103, 0, 0 }, { 104, 111, 117, 0, 0, 0 },
        { 104, 117, 0, 0, 0, 0 }, { 104, 117, 97, 0, 0, 0 }, { 104, 117, 97, 105, 0, 0 },
        { 104, 117, 97, 110, 0, 0 }, { 104, 117, 97, 110, 103, 0 }, { 104, 117, 105, 0, 0, 0 },
        { 104, 117, 110, 0, 0, 0 }, { 104, 117, 111, 0, 0, 0 }, { 106, 105, 0, 0, 0, 0 },
        { 106, 105, 97, 0, 0, 0 }, { 106, 105, 97, 110, 0, 0 }, { 106, 105, 97, 110, 103, 0 },
        { 106, 105, 97, 111, 0, 0 }, { 106, 105, 101, 0, 0, 0 }, { 106, 105, 110, 0, 0, 0 },
        { 106, 105, 110, 103, 0, 0 }, { 106, 105, 111, 110, 103, 0 }, { 106, 105, 117, 0, 0, 0 },
        { 106, 117, 0, 0, 0, 0 }, { 106, 117, 97, 110, 0, 0 }, { 106, 117, 101, 0, 0, 0 },
        { 106, 117, 110, 0, 0, 0 }, { 107, 97, 0, 0, 0, 0 }, { 107, 97, 105, 0, 0, 0 },
        { 107, 97, 110, 0, 0, 0 }, { 107, 97, 110, 103, 0, 0 }, { 107, 97, 111, 0, 0, 0 },
        { 107, 101, 0, 0, 0, 0 }, { 107, 101, 105, 0, 0, 0 }, { 107, 101, 110, 0, 0, 0 },
        { 107, 101, 110, 103, 0, 0 }, { 107, 111, 110, 103, 0, 0 }, { 107, 111, 117, 0, 0, 0 },
        { 107, 117, 0, 0, 0, 0 }, { 107, 117, 97, 0, 0, 0 }, { 107, 117, 97, 105, 0, 0 },
        { 107, 117, 97, 110, 0, 0 }, { 107, 117, 97, 110, 103, 0 }, { 107, 117, 105, 0, 0, 0 },
        { 107, 117, 110, 0, 0, 0 }, { 107, 117, 111, 0, 0, 0 }, { 108, 97, 0, 0, 0, 0 },
        { 108, 97, 105, 0, 0, 0 }, { 108, 97, 110, 0, 0, 0 }, { 108, 97, 110, 103, 0, 0 },
        { 108, 97, 111, 0, 0, 0 }, { 108, 101, 0, 0, 0, 0 }, { 108, 101, 105, 0, 0, 0 },
        { 108, 101, 110, 103, 0, 0 }, { 108, 105, 0, 0, 0, 0 }, { 108, 105, 97, 0, 0, 0 },
        { 108, 105, 97, 110, 0, 0 }, { 108, 105, 97, 110, 103, 0 }, { 108, 105, 97, 111, 0, 0 },
        { 108, 105, 101, 0, 0, 0 }, { 108, 105, 110, 0, 0, 0 }, { 108, 105, 110, 103, 0, 0 },
        { 108, 105, 117, 0, 0, 0 }, { 108, 111, 110, 103, 0, 0 }, { 108, 111, 117, 0, 0, 0 },
        { 108, 117, 0, 0, 0, 0 }, { 108, 117, 97, 110, 0, 0 }, { 108, 117, 101, 0, 0, 0 },
        { 108, 117, 110, 0, 0, 0 }, { 108, 117, 111, 0, 0, 0 }, { 109, 0, 0, 0, 0, 0 },
        { 109, 97, 0, 0, 0, 0 }, { 109, 97, 105, 0, 0, 0 }, { 109, 97, 110, 0, 0, 0 },
        { 109, 97, 110, 103, 0, 0 }, { 109, 97, 111, 0, 0, 0 }, { 109, 101, 0, 0, 0, 0 },
        { 109, 101, 105, 0, 0, 0 }, { 109, 101, 110, 0, 0, 0 }, { 109, 101, 110, 103, 0, 0 },
        { 109, 105, 0, 0, 0, 0 }, { 109, 105, 97, 110, 0, 0 }, { 109, 105, 97, 111, 0, 0 },
        { 109, 105, 101, 0, 0, 0 }, { 109, 105, 110, 0, 0, 0 }, { 109, 105, 110, 103, 0, 0 },
        { 109, 105, 117, 0, 0, 0 }, { 109, 111, 0, 0, 0, 0 }, { 109, 111, 117, 0, 0, 0 },
        { 109, 117, 0, 0, 0, 0 }, { 110, 97, 0, 0, 0, 0 }, { 110, 97, 105, 0, 0, 0 },
        { 110, 97, 110, 0, 0, 0 }, { 110, 97, 110, 103, 0, 0 }, { 110, 97, 111, 0, 0, 0 },
        { 110, 101, 0, 0, 0, 0 }, { 110, 101, 105, 0, 0, 0 }, { 110, 101, 110, 0, 0, 0 },
        { 110, 101, 110, 103, 0, 0 }, { 110, 105, 0, 0, 0, 0 }, { 110, 105, 97, 110, 0, 0 },
        { 110, 105, 97, 110, 103, 0 }, { 110, 105, 97, 111, 0, 0 }, { 110, 105, 101, 0, 0, 0 },
        { 110, 105, 110, 0, 0, 0 }, { 110, 105, 110, 103, 0, 0 }, { 110, 105, 117, 0, 0, 0 },
        { 110, 111, 110, 103, 0, 0 }, { 110, 111, 117, 0, 0, 0 }, { 110, 117, 0, 0, 0, 0 },
        { 110, 117, 97, 110, 0, 0 }, { 110, 117, 101, 0, 0, 0 }, { 110, 117, 111, 0, 0, 0 },
        { 111, 0, 0, 0, 0, 0 }, { 111, 117, 0, 0, 0, 0 }, { 112, 97, 0, 0, 0, 0 },
        { 112, 97, 105, 0, 0, 0 }, { 112, 97, 110, 0, 0, 0 }, { 112, 97, 110, 103, 0, 0 },
        { 112, 97, 111, 0, 0, 0 }, { 112, 101, 105, 0, 0, 0 }, { 112, 101, 110, 0, 0, 0 },
        { 112, 101, 110, 103, 0, 0 }, { 112, 105, 0, 0, 0, 0 }, { 112, 105, 97, 110, 0, 0 },
        { 112, 105, 97, 111, 0, 0 }, { 112, 105, 101, 0, 0, 0 }, { 112, 105, 110, 0, 0, 0 },
        { 112, 105, 110, 103, 0, 0 }, { 112, 111, 0, 0, 0, 0 }, { 112, 111, 117, 0, 0, 0 },
        { 112, 117, 0, 0, 0, 0 }, { 113, 105, 0, 0, 0, 0 }, { 113, 105, 97, 0, 0, 0 },
        { 113, 105, 97, 110, 0, 0 }, { 113, 105, 97, 110, 103, 0 }, { 113, 105, 97, 111, 0, 0 },
        { 113, 105, 101, 0, 0, 0 }, { 113, 105, 110, 0, 0, 0 }, { 113, 105, 110, 103, 0, 0 },
        { 113, 105, 111, 110, 103, 0 }, { 113, 105, 117, 0, 0, 0 }, { 113, 117, 0, 0, 0, 0 },
        { 113, 117, 97, 110, 0, 0 }, { 113, 117, 101, 0, 0, 0 }, { 113, 117, 110, 0, 0, 0 },
        { 114, 97, 110, 0, 0, 0 }, { 114, 97, 110, 103, 0, 0 }, { 114, 97, 111, 0, 0, 0 },
        { 114, 101, 0, 0, 0, 0 }, { 114, 101, 110, 0, 0, 0 }, { 114, 101, 110, 103, 0, 0 },
        { 114, 105, 0, 0, 0, 0 }, { 114, 111, 110, 103, 0, 0 }, { 114, 111, 117, 0, 0, 0 },
        { 114, 117, 0, 0, 0, 0 }, { 114, 117, 97, 110, 0, 0 }, { 114, 117, 105, 0, 0, 0 },
        { 114, 117, 110, 0, 0, 0 }, { 114, 117, 111, 0, 0, 0 }, { 115, 97, 0, 0, 0, 0 },
        { 115, 97, 105, 0, 0, 0 }, { 115, 97, 110, 0, 0, 0 }, { 115, 97, 110, 103, 0, 0 },
        { 115, 97, 111, 0, 0, 0 }, { 115, 101, 0, 0, 0, 0 }, { 115, 101, 110, 0, 0, 0 },
        { 115, 101, 110, 103, 0, 0 }, { 115, 104, 97, 0, 0, 0 }, { 115, 104, 97, 105, 0, 0 },
        { 115, 104, 97, 110, 0, 0 }, { 115, 104, 97, 110, 103, 0 }, { 115, 104, 97, 111, 0, 0 },
        { 115, 104, 101, 0, 0, 0 }, { 115, 104, 101, 110, 0, 0 }, { 115, 104, 101, 110, 103, 0 },
        { 115, 104, 105, 0, 0, 0 }, { 115, 104, 111, 117, 0, 0 }, { 115, 104, 117, 0, 0, 0 },
        { 115, 104, 117, 97, 0, 0 }, { 115, 104, 117, 97, 105, 0 }, { 115, 104, 117, 97, 110, 0 },
        { 115, 104, 117, 97, 110, 103 }, { 115, 104, 117, 105, 0, 0 }, { 115, 104, 117, 110, 0, 0 },
        { 115, 104, 117, 111, 0, 0 }, { 115, 105, 0, 0, 0, 0 }, { 115, 111, 110, 103, 0, 0 },
        { 115, 111, 117, 0, 0, 0 }, { 115, 117, 0, 0, 0, 0 }, { 115, 117, 97, 110, 0, 0 },
        { 115, 117, 105, 0, 0, 0 }, { 115, 117, 110, 0, 0, 0 }, { 115, 117, 111, 0, 0, 0 },
        { 116, 97, 0, 0, 0, 0 }, { 116, 97, 105, 0, 0, 0 }, { 116, 97, 110, 0, 0, 0 },
        { 116, 97, 110, 103, 0, 0 }, { 116, 97, 111, 0, 0, 0 }, { 116, 101, 0, 0, 0, 0 },
        { 116, 101, 110, 103, 0, 0 }, { 116, 105, 0, 0, 0, 0 }, { 116, 105, 97, 110, 0, 0 },
        { 116, 105, 97, 111, 0, 0 }, { 116, 105, 101, 0, 0, 0 }, { 116, 105, 110, 103, 0, 0 },
        { 116, 111, 110, 103, 0, 0 }, { 116, 111, 117, 0, 0, 0 }, { 116, 117, 0, 0, 0, 0 },
        { 116, 117, 97, 110, 0, 0 }, { 116, 117, 105, 0, 0, 0 }, { 116, 117, 110, 0, 0, 0 },
        { 116, 117, 111, 0, 0, 0 }, { 119, 97, 0, 0, 0, 0 }, { 119, 97, 105, 0, 0, 0 },
        { 119, 97, 110, 0, 0, 0 }, { 119, 97, 110, 103, 0, 0 }, { 119, 101, 105, 0, 0, 0 },
        { 119, 101, 110, 0, 0, 0 }, { 119, 101, 110, 103, 0, 0 }, { 119, 111, 0, 0, 0, 0 },
        { 119, 117, 0, 0, 0, 0 }, { 120, 105, 0, 0, 0, 0 }, { 120, 105, 97, 0, 0, 0 },
        { 120, 105, 97, 110, 0, 0 }, { 120, 105, 97, 110, 103, 0 }, { 120, 105, 97, 111, 0, 0 },
        { 120, 105, 101, 0, 0, 0 }, { 120, 105, 110, 0, 0, 0 }, { 120, 105, 110, 103, 0, 0 },
        { 120, 105, 111, 110, 103, 0 }, { 120, 105, 117, 0, 0, 0 }, { 120, 117, 0, 0, 0, 0 },
        { 120, 117, 97, 110, 0, 0 }, { 120, 117, 101, 0, 0, 0 }, { 120, 117, 110, 0, 0, 0 },
        { 121, 97, 0, 0, 0, 0 }, { 121, 97, 110, 0, 0, 0 }, { 121, 97, 110, 103, 0, 0 },
        { 121, 97, 111, 0, 0, 0 }, { 121, 101, 0, 0, 0, 0 }, { 121, 105, 0, 0, 0, 0 },
        { 121, 105, 110, 0, 0, 0 }, { 121, 105, 110, 103, 0, 0 }, { 121, 111, 0, 0, 0, 0 },
        { 121, 111, 110, 103, 0, 0 }, { 121, 111, 117, 0, 0, 0 }, { 121, 117, 0, 0, 0, 0 },
        { 121, 117, 97, 110, 0, 0 }, { 121, 117, 101, 0, 0, 0 }, { 121, 117, 110, 0, 0, 0 },
        { 90, 97, 0, 0, 0, 0 }, { 90, 97, 105, 0, 0, 0 }, { 90, 97, 110, 0, 0, 0 },
        { 90, 97, 110, 103, 0, 0 }, { 90, 97, 111, 0, 0, 0 }, { 90, 101, 0, 0, 0, 0 },
        { 90, 101, 105, 0, 0, 0 }, { 90, 101, 110, 0, 0, 0 }, { 90, 101, 110, 103, 0, 0 },
        { 90, 104, 97, 0, 0, 0 }, { 90, 104, 97, 105, 0, 0 }, { 90, 104, 97, 110, 0, 0 },
        { 90, 104, 97, 110, 103, 0 }, { 90, 104, 97, 111, 0, 0 }, { 90, 104, 101, 0, 0, 0 },
        { 90, 104, 101, 110, 0, 0 }, { 90, 104, 101, 110, 103, 0 }, { 90, 104, 105, 0, 0, 0 },
        { 90, 104, 111, 110, 103, 0 }, { 90, 104, 111, 117, 0, 0 }, { 90, 104, 117, 0, 0, 0 },
        { 90, 104, 117, 97, 0, 0 }, { 90, 104, 117, 97, 105, 0 }, { 90, 104, 117, 97, 110, 0 },
        { 90, 104, 117, 97, 110, 103 }, { 90, 104, 117, 105, 0, 0 }, { 90, 104, 117, 110, 0, 0 },
        { 90, 104, 117, 111, 0, 0 }, { 90, 105, 0, 0, 0, 0 }, { 90, 111, 110, 103, 0, 0 },
        { 90, 111, 117, 0, 0, 0 }, { 90, 117, 0, 0, 0, 0 }, { 90, 117, 97, 110, 0, 0 },
        { 90, 117, 105, 0, 0, 0 }, { 90, 117, 110, 0, 0, 0 }, { 90, 117, 111, 0, 0, 0 }, };

    /** First and last Chinese character with known Pinyin according to zh collation */
    private static final String FIRST_PINYIN_UNIHAN = "\u963F";
    private static final String LAST_PINYIN_UNIHAN = "\u84D9";
    /** The first Chinese character in Unicode block */
    private static final char FIRST_UNIHAN = '\u3400';
    private static final Collator COLLATOR = Collator.getInstance(Locale.CHINA);

    private static SimpleHanziToPinyin sInstance;
    private final boolean mHasChinaCollator;

    public static class Token {
        /**
         * Separator between target string for each source char
         */
        public static final String SEPARATOR = " ";

        public static final int LATIN = 1;
        public static final int PINYIN = 2;
        public static final int UNKNOWN = 3;

        public Token() {
        }

        public Token(int type, String source, String target) {
            this(type, source, target, -1, -1);
        }

        public Token(int type, String source, String target, int first, int last) {
            this.type = type;
            this.source = source;
            this.target = target;
            this.firstIndexOfSourceInOriInput = first;
            this.lastIndexOfSourceInOriInput = last;
        }

        /**
         * Type of this token, ASCII, PINYIN or UNKNOWN.
         */
        public int type;
        /**
         * Original string before translation.
         */
        public String source;
        /**
         * Translated string of source. For Han, target is corresponding Pinyin. Otherwise target is
         * original string in source.
         */
        public String target;

        public int firstIndexOfSourceInOriInput;
        public int lastIndexOfSourceInOriInput;
    }

    protected SimpleHanziToPinyin(boolean hasChinaCollator) {
        mHasChinaCollator = hasChinaCollator;
    }

    public static SimpleHanziToPinyin getInstance() {
        synchronized (SimpleHanziToPinyin.class) {
            if (sInstance != null) {
                return sInstance;
            }
            // Check if zh_CN collation data is available
            final Locale locale[] = Collator.getAvailableLocales();
            for (int i = 0; i < locale.length; i++) {
                if (locale[i].equals(Locale.CHINA)) {
                    // Do self validation just once.
                    if (DEBUG) {
                        Log.d(TAG, "Self validation. Result: " + doSelfValidation());
                    }
                    sInstance = new SimpleHanziToPinyin(true);
                    return sInstance;
                }
            }
            Log.w(TAG, "There is no Chinese collator, HanziToPinyin is disabled");
            sInstance = new SimpleHanziToPinyin(false);
            return sInstance;
        }
    }

    /**
     * Validate if our internal table has some wrong value.
     *
     * @return true when the table looks correct.
     */
    private static boolean doSelfValidation() {
        char lastChar = UNIHANS[0];
        String lastString = Character.toString(lastChar);
        for (char c : UNIHANS) {
            if (lastChar == c) {
                continue;
            }
            final String curString = Character.toString(c);
            int cmp = COLLATOR.compare(lastString, curString);
            if (cmp >= 0) {
                Log.e(TAG, "Internal error in Unihan table. " + "The last string \"" + lastString
                        + "\" is greater than current string \"" + curString + "\".");
                return false;
            }
            lastString = curString;
        }
        return true;
    }

    private Token getToken(char character, int index) {
        Token token = new Token();
        final String letter = Character.toString(character);
        token.source = letter;
        token.firstIndexOfSourceInOriInput = index;
        token.lastIndexOfSourceInOriInput = index;
        int offset = -1;
        int cmp;
        if (character < 256) {
            token.type = Token.LATIN;
            token.target = letter;
            return token;
        } else if (character < FIRST_UNIHAN) {
            token.type = Token.UNKNOWN;
            token.target = letter;
            return token;
        } else {
            cmp = COLLATOR.compare(letter, FIRST_PINYIN_UNIHAN);
            if (cmp < 0) {
                token.type = Token.UNKNOWN;
                token.target = letter;
                return token;
            } else if (cmp == 0) {
                token.type = Token.PINYIN;
                offset = 0;
            } else {
                cmp = COLLATOR.compare(letter, LAST_PINYIN_UNIHAN);
                if (cmp > 0) {
                    token.type = Token.UNKNOWN;
                    token.target = letter;
                    return token;
                } else if (cmp == 0) {
                    token.type = Token.PINYIN;
                    offset = UNIHANS.length - 1;
                }
            }
        }

        token.type = Token.PINYIN;
        if (offset < 0) {
            int begin = 0;
            int end = UNIHANS.length - 1;
            while (begin <= end) {
                offset = (begin + end) / 2;
                final String unihan = Character.toString(UNIHANS[offset]);
                cmp = COLLATOR.compare(letter, unihan);
                if (cmp == 0) {
                    break;
                } else if (cmp > 0) {
                    begin = offset + 1;
                } else {
                    end = offset - 1;
                }
            }
        }
        if (cmp < 0) {
            offset--;
        }
        StringBuilder pinyin = new StringBuilder();
        for (int j = 0; j < PINYINS[offset].length && PINYINS[offset][j] != 0; j++) {
            pinyin.append((char) PINYINS[offset][j]);
        }
        token.target = pinyin.toString();
        return token;
    }

    /**
     * Convert the input to a array of tokens. The sequence of ASCII or Unknown characters without
     * space will be put into a Token, One Hanzi character which has pinyin will be treated as a
     * Token. If these is no China collator, the empty token array is returned.
     */
    public ArrayList<Token> get(final String input) {
        ArrayList<Token> tokens = new ArrayList<Token>();
        if (!mHasChinaCollator || TextUtils.isEmpty(input)) {
            // return empty tokens.
            return tokens;
        }
        final int inputLength = input.length();
        final StringBuilder sb = new StringBuilder();
        Integer tokenFirstIndex = -1;
        Integer tokenLastIndex = -1;
        int tokenType = Token.LATIN;
        // Go through the input, create a new token when
        // a. Token type changed
        // b. Get the Pinyin of current charater.
        // c. current character is space.
        for (int i = 0; i < inputLength; i++) {
            final char character = input.charAt(i);
            if (character == ' ') {
                if (sb.length() > 0) {
                    addToken(sb, tokens, tokenFirstIndex, tokenLastIndex, tokenType);
                }
            } else if (character < 256) {
                if (tokenType != Token.LATIN && sb.length() > 0) {
                    addToken(sb, tokens, tokenFirstIndex, tokenLastIndex, tokenType);
                }
                tokenType = Token.LATIN;
                if (sb.length() == 0) {
                    tokenFirstIndex = i;
                }
                sb.append(character);
                tokenLastIndex = i;
            } else if (character < FIRST_UNIHAN) {
                if (tokenType != Token.UNKNOWN && sb.length() > 0) {
                    addToken(sb, tokens, tokenFirstIndex, tokenLastIndex, tokenType);
                }
                tokenType = Token.UNKNOWN;
                if (sb.length() == 0) {
                    tokenFirstIndex = i;
                }
                sb.append(character);
                tokenLastIndex = i;
            } else {
                Token t = getToken(character, i);
                if (t.type == Token.PINYIN) {
                    if (sb.length() > 0) {
                        addToken(sb, tokens, tokenFirstIndex, tokenLastIndex, tokenType);
                    }
                    tokens.add(t);
                    tokenFirstIndex = -1;
                    tokenLastIndex = -1;
                    tokenType = Token.PINYIN;
                } else {
                    if (tokenType != t.type && sb.length() > 0) {
                        addToken(sb, tokens, tokenFirstIndex, tokenLastIndex, tokenType);
                    }
                    tokenType = t.type;
                    if (sb.length() == 0) {
                        tokenFirstIndex = i;
                    }
                    sb.append(character);
                    tokenLastIndex = i;
                }
            }
        }
        if (sb.length() > 0) {
            addToken(sb, tokens, tokenFirstIndex, tokenLastIndex, tokenType);
        }
        return tokens;
    }

    private void addToken(
            final StringBuilder sb, final ArrayList<Token> tokens, final int tokenType) {
        String str = sb.toString();
        tokens.add(new Token(tokenType, str, str));
        sb.setLength(0);
    }
    
    private void addToken(
            final StringBuilder sb, final ArrayList<Token> tokens, Integer first, Integer last, final int tokenType) {
        String str = sb.toString();
        tokens.add(new Token(tokenType, str, str, first, last));
        first = -1;
        last = -1;
        sb.setLength(0);
    }
}
