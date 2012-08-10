package com.android.contacts.numberarea;

import com.android.contacts.numberarea.NumberArea.Area;
import com.android.contacts.numberarea.NumberArea.PhoneNumber;
import com.android.contacts.numberarea.NumberArea.PhoneNumberArea;
import com.android.contacts.util.Constants;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Directory;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronously loads area and maintains a cache of areas.
 * @author yongan.qiu
 */
public abstract class NumberAreaManager{
    static final String TAG = "NumberAreaManager";

    static final boolean DEBUG = Constants.TOTAL_DEBUG;

    public static final String NUMBER_AREA_SERVICE = "numberAreas";

    public static synchronized NumberAreaManager createNumberAreaManager(Context context) {
        return new NumberAreaManagerImpl(context);
    }

    /**
     * Load area into the supplied text view.  If the area is already cached,
     * it is displayed immediately.  Otherwise a request is sent to load the area
     * from the database.
     */
    public abstract void loadArea(TextView view, String number);

    /**
     * Remove area from the supplied text view. This also cancels current pending load request
     * inside this area manager.
     */
    public abstract void removeArea(TextView view);

    /**
     * Temporarily stops loading areas from the database.
     */
    public abstract void pause();

    /**
     * Resumes loading areas from the database.
     */
    public abstract void resume();

    /**
     * Initiates a background process that over time will fill up cache with
     * preload areas.
     */
    public abstract void preloadAreasInBackground();

}

class NumberAreaManagerImpl extends NumberAreaManager implements Callback {
    private static final String LOADER_THREAD_NAME = "NumberAreaLoader";

    private static final String DEFAULT_STRING = "-";

    /**
     * Type of message sent by the UI thread to itself to indicate that some areas
     * need to be loaded.
     */
    private static final int MESSAGE_REQUEST_LOADING = 1;

    /**
     * Type of message sent by the loader thread to indicate that some areas have
     * been loaded.
     */
    private static final int MESSAGE_AREAS_LOADED = 2;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String[] COLUMNS = new String[] {PhoneNumber.NUMBER, Area.AREA};

    private final Context mContext;

    private HashMap<String, String> mAreaCache;
    /**
     * A map from TextView to the corresponding number, encapsulated in a request.
     * The request may swapped out before the area loading request is started.
     */
    private final ConcurrentHashMap<TextView, Request> mPendingRequests =
            new ConcurrentHashMap<TextView, Request>();

    /**
     * Handler for messages sent to the UI thread.
     */
    private final Handler mMainThreadHandler = new Handler(this);

    /**
     * Thread responsible for loading areas from the database. Created upon
     * the first request.
     */
    private LoaderThread mLoaderThread;

    /**
     * A gate to make sure we only send one instance of MESSAGE_REQUEST_LOADING at a time.
     */
    private boolean mLoadingRequested;

    /**
     * Flag indicating if the area loading is paused.
     */
    private boolean mPaused;

    public NumberAreaManagerImpl(Context context) {
        mContext = context;
        mAreaCache = new HashMap<String, String>();
    }

    @Override
    public void preloadAreasInBackground() {
        ensureLoaderThread();
        mLoaderThread.requestPreloading();
    }

    @Override
    public void loadArea(TextView view, String number) {
        if (!NumberArea.isValidPhoneNumber(number)) {
            if (DEBUG) Log.d(TAG, "loadArea: number not valid. number is " + number);
            applyDefaultString(view);
            mPendingRequests.remove(view);
        } else {
            number = NumberArea.cropValidPhoneNumber(number);
            if (DEBUG) Log.d(TAG, "loadArea request: " + number);
            loadAreaByNumber(view, Request.createFromNumber(number));
        }
    }

    private void loadAreaByNumber(TextView view, Request request) {
        boolean loaded = loadCachedArea(view, request);
        Log.i(TAG, "loadAreaByNumber: loaded is " + loaded + ", pause is " + mPaused);
        if (loaded) {
            mPendingRequests.remove(view);
        } else {
            mPendingRequests.put(view, request);
            if (!mPaused) {
                // Send a request to start loading areas
                requestLoading();
            }
        }
    }

    @Override
    public void removeArea(TextView view) {
        view.setText(DEFAULT_STRING);
        mPendingRequests.remove(view);
    }

    private void applyDefaultString(TextView view) {
        view.setText(DEFAULT_STRING);
    }

    /**
     * Checks if the area is present in cache.  If so, sets the area on the view.
     *
     * @return false if the area needs to be (re)loaded from the provider.
     */
    private boolean loadCachedArea(TextView view, Request request) {
        String area = mAreaCache.get(request.getKey());
        if (TextUtils.isEmpty(area)) {
            // The area has not been loaded - should display the default string.
            request.applyDefaultString(view);
            return false;
        }
        view.setText(area);
        return true;
    }

    public void clear() {
        if (DEBUG) Log.d(TAG, "clear");
        mPendingRequests.clear();
        mAreaCache.clear();
    }

    @Override
    public void pause() {
        mPaused = true;
    }

    @Override
    public void resume() {
        mPaused = false;
        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * Sends a message to this thread itself to start loading areas.  If the current
     * view contains multiple area views, all of those area views will get a chance
     * to request their respective areas before any of those requests are executed.
     * This allows us to load images in bulk.
     */
    private void requestLoading() {
        if (!mLoadingRequested) {
            mLoadingRequested = true;
            mMainThreadHandler.sendEmptyMessage(MESSAGE_REQUEST_LOADING);
        }
    }

    /**
     * Processes requests on the main thread.
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_REQUEST_LOADING: {
                mLoadingRequested = false;
                if (!mPaused) {
                    ensureLoaderThread();
                    mLoaderThread.requestLoading();
                }
                return true;
            }

            case MESSAGE_AREAS_LOADED: {
                if (!mPaused) {
                    processLoadedareas();
                }
                return true;
            }
        }
        return false;
    }

    public void ensureLoaderThread() {
        if (mLoaderThread == null) {
            mLoaderThread = new LoaderThread(mContext.getContentResolver());
            mLoaderThread.start();
        }
    }

    /**
     * Goes over pending loading requests and displays loaded areas.  If some of the
     * areas still haven't been loaded, sends another request for area loading.
     */
    private void processLoadedareas() {
        Iterator<TextView> iterator = mPendingRequests.keySet().iterator();
        while (iterator.hasNext()) {
            TextView view = iterator.next();
            Request key = mPendingRequests.get(view);
            boolean loaded = loadCachedArea(view, key);
            if (loaded) {
                iterator.remove();
            }
        }

        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * Stores the supplied area in cache.
     */
    private void cacheArea(String number, String area) {
        if (DEBUG) Log.d(TAG, "cache : number = " + number + ", area = " + area);
        mAreaCache.put(number, area);
    }

    /**
     * Populates an array of numbers that need to be loaded just from requests.
     */
    private void obtainNumbersToLoad(Set<String> numbers) {
        numbers.clear();

        Iterator<Request> iterator = mPendingRequests.values().iterator();
        while (iterator.hasNext()) {
            Request request = iterator.next();
            String area = mAreaCache.get(request.mNumber);
            if (TextUtils.isEmpty(area)) {
                numbers.add(request.mNumber);
            }
        }
    }

    /**
     * The thread that performs loading of areas from the database.
     */
    private class LoaderThread extends HandlerThread implements Callback {
        private static final int MESSAGE_PRELOAD_NUMBERS = 0;
        private static final int MESSAGE_LOAD_AREAS = 1;

        /**
         * A pause between preload batches that yields to the UI thread.
         */
        private static final int NUMBER_PRELOAD_DELAY = 1000;

        /**
         * Number of areas to preload per batch.
         */
        private static final int PRELOAD_BATCH = 25;

        private static final int MAX_AREAS_TO_PRELOAD = 100;

        private static final int STRING_CACHE_RED_ZONE_SIZE = 200;

        private final ContentResolver mResolver;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private final Set<String> mNumbers = Sets.newHashSet();
        private final List<String> mPreloadNumbers = Lists.newArrayList();

        private Handler mLoaderThreadHandler;

        private static final int PRELOAD_STATUS_NOT_STARTED = 0;
        private static final int PRELOAD_STATUS_IN_PROGRESS = 1;
        private static final int PRELOAD_STATUS_DONE = 2;

        private int mPreloadStatus = PRELOAD_STATUS_NOT_STARTED;

        public LoaderThread(ContentResolver resolver) {
            super(LOADER_THREAD_NAME);
            mResolver = resolver;
        }

        public void ensureHandler() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
        }

        /**
         * Kicks off preloading of the next batch of areas on the background thread.
         * Preloading will happen after a delay: we want to yield to the UI thread
         * as much as possible.
         * <p>
         * If preloading is already complete, does nothing.
         */
        public void requestPreloading() {
            if (mPreloadStatus == PRELOAD_STATUS_DONE) {
                return;
            }

            ensureHandler();
            if (mLoaderThreadHandler.hasMessages(MESSAGE_LOAD_AREAS)) {
                return;
            }

            mLoaderThreadHandler.sendEmptyMessageDelayed(
                    MESSAGE_PRELOAD_NUMBERS, NUMBER_PRELOAD_DELAY);
        }

        /**
         * Sends a message to this thread to load requested areas.  Cancels a preloading
         * request, if any: we don't want preloading to impede loading of the areas
         * we need to display now.
         */
        public void requestLoading() {
            ensureHandler();
            mLoaderThreadHandler.removeMessages(MESSAGE_PRELOAD_NUMBERS);
            mLoaderThreadHandler.sendEmptyMessage(MESSAGE_LOAD_AREAS);
        }

        /**
         * Receives the above message, loads areas and then sends a message
         * to the main thread to process them.
         */
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_PRELOAD_NUMBERS:
                    preloadAreasInBackground();
                    break;
                case MESSAGE_LOAD_AREAS:
                    loadAreasInBackground();
                    break;
            }
            return true;
        }

        /**
         * The first time it is called, figures out which areas need to be preloaded.
         * Each subsequent call preloads the next batch of areas and requests
         * another cycle of preloading after a delay.  The whole process ends when
         * we either run out of areas to preload or fill up cache.
         */
        private void preloadAreasInBackground() {
            if (mPreloadStatus == PRELOAD_STATUS_DONE) {
                return;
            }

            if (mPreloadStatus == PRELOAD_STATUS_NOT_STARTED) {
                queryAreasForPreload();
                if (mPreloadNumbers.isEmpty()) {
                    mPreloadStatus = PRELOAD_STATUS_DONE;
                } else {
                    mPreloadStatus = PRELOAD_STATUS_IN_PROGRESS;
                }
                requestPreloading();
                return;
            }

            if (mAreaCache.size() > STRING_CACHE_RED_ZONE_SIZE) {
                mPreloadStatus = PRELOAD_STATUS_DONE;
                return;
            }

            mNumbers.clear();

            int count = 0;
            int preloadSize = mPreloadNumbers.size();
            while(preloadSize > 0 && mNumbers.size() < PRELOAD_BATCH) {
                preloadSize--;
                count++;
                String number = mPreloadNumbers.get(preloadSize);
                mNumbers.add(number);
                mPreloadNumbers.remove(preloadSize);
            }

            loadAreasFromDatabase(true);

            if (preloadSize == 0) {
                mPreloadStatus = PRELOAD_STATUS_DONE;
            }

            Log.v(TAG, "Preloaded " + count + " areas.  Cached size: "
                    + mAreaCache.size());

            requestPreloading();
        }

        private void queryAreasForPreload() {
            Cursor cursor = null;
            try {
                Uri uri = Constants.CALL_LOG_GROUP_BY_NUMBER.buildUpon().appendQueryParameter(
                        ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT))
                        .appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                                String.valueOf(MAX_AREAS_TO_PRELOAD))
                        .build();
                cursor = mResolver.query(uri, new String[] { Calls.NUMBER },
                        "(" + Calls.CACHED_NUMBER_TYPE + " IS NULL OR " + Calls.CACHED_NUMBER_TYPE
                                + " = 0) AND " + Calls.DATE + " > " + Constants.NEW_SECTION_TIME_WINDOW,
                        null,
                        Calls.DEFAULT_SORT_ORDER);

                if (cursor != null) {
                    String number;
                    while (cursor.moveToNext()) {
                        // Insert them in reverse order, because we will be taking
                        // them from the end of the list for loading.
                        number = cursor.getString(0);
                        if (NumberArea.isValidPhoneNumber(number)) {
                            mPreloadNumbers.add(0, NumberArea.cropValidPhoneNumber(number));
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        /**
         * 
         */
        private void loadAreasInBackground() {
            obtainNumbersToLoad(mNumbers);
            loadAreasFromDatabase(false);
            requestPreloading();
        }

        private void loadAreasFromDatabase(boolean preloading) {
            if (mNumbers.isEmpty()) {
                return;
            }

            // Remove loaded areas from the preload queue: we don't want
            // the preloading process to load them again.
            if (!preloading && mPreloadStatus == PRELOAD_STATUS_IN_PROGRESS) {
                for (String number : mNumbers) {
                    mPreloadNumbers.remove(number);
                }
                if (mPreloadNumbers.isEmpty()) {
                    mPreloadStatus = PRELOAD_STATUS_DONE;
                }
            }

            mStringBuilder.setLength(0);
            mStringBuilder.append(PhoneNumber.NUMBER + " IN(");
            for (int i = 0; i < mNumbers.size(); i++) {
                if (i != 0) {
                    mStringBuilder.append(',');
                }
                mStringBuilder.append('?');
            }
            mStringBuilder.append(")");

            Cursor cursor = null;
            try {
                if (DEBUG) Log.d(TAG, "Loading " + TextUtils.join(",", mNumbers));
                cursor = mResolver.query(PhoneNumberArea.CONTENT_URI,
                        COLUMNS,
                        mStringBuilder.toString(),
                        mNumbers.toArray(EMPTY_STRING_ARRAY),
                        null);

                if (cursor != null) {
                    if (DEBUG) Log.d(TAG, "something loaded. count " + cursor.getCount());
                    while (cursor.moveToNext()) {
                        String number = cursor.getString(0);
                        String area = cursor.getString(1);
                        cacheArea(number, area);
                        mNumbers.remove(number);
                    }
                }
                // Cache null when some number cannot find their areas.
                if (!mNumbers.isEmpty()) {
                    if (DEBUG) Log.d(TAG, "These numbers can not find their areas: " + TextUtils.join(",", mNumbers));
                    for (String number : mNumbers) {
                        cacheArea(number, DEFAULT_STRING);
                    }
                    mNumbers.clear();
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Remaining areas were not found in the contacts database.
            for (String number : mNumbers) {
                cacheArea(number, DEFAULT_STRING);
            }

            mMainThreadHandler.sendEmptyMessage(MESSAGE_AREAS_LOADED);
        }
    }

    /**
     * A holder for number.
     */
    private static final class Request {
        private final String mNumber;

        private Request(String number) {
            mNumber = number;
        }

        public static Request createFromNumber(String number) {
            return new Request(number);
        }

        @Override
        public int hashCode() {
            return mNumber.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Request)) return false;
            return mNumber.equals(((Request) o).mNumber);
        }

        public String getKey() {
            return mNumber;
        }

        public void applyDefaultString(TextView view) {
            view.setText(DEFAULT_STRING);
        }
    }
}
