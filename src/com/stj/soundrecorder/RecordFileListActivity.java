
package com.stj.soundrecorder;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import com.stj.soundrecorder.StorageHelper.Storage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecordFileListActivity extends BaseActivity implements
        BaseActivity.BottomKeyClickListener {

    private static final int RECORD_REQUESETCODE = 1 ;
    public static final int RECORD_PLAY_RESULTCODE = 2 ;
    public static final int RECORD_DELETE_RESULTCODE = 3 ;
    private static final String KEY_SELECTED_RECORD = "key_selected_record";
    private View mRootView;
    private static Uri mSelectedAudioFileUri ;
    private static RecordFileListFragment mRecordFileListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setContentView(R.layout.file_list_activity);

        mMiddleViewStub.setLayoutResource(R.layout.file_list_activity);
        mRootView = mMiddleViewStub.inflate();
        setBottomKeyClickListener(this);
        setActivityBgResource(0);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentById(R.id.file_list_content) == null) {
            RecordFileListFragment listFragment = new RecordFileListFragment();
            fm.beginTransaction().replace(R.id.file_list_content, listFragment).commit();
        }
    }

    public static class RecordFileListFragment extends ListFragment implements
            LoaderManager.LoaderCallbacks<ArrayList<RecordFileInfo>>, OnClickListener,
            OnQueryTextListener, OnCloseListener {

        private static String TAG = RecordFileListActivity.class.getSimpleName();

        private FileListAdapter mListAdapter;
        private FileListLoader mLoader;
        private ListView mFileListView;
        private View mBottomView;
        private FileSearchView mFileSearchView;
        private static String mCurFilter;
        private TextView mEmptyText;

        private boolean mIsMultiSimCard;

        private final int DELETE_ITEM = 0;
        private final int SELECT_AS_RINGTONE = 1;
        private final int SHARE_ITEM = 2;
        private final int ITEM_DETAIL = 3;

        private final int MENU_SELECT_ALL_ID = 0;
        private final int MENU_SEARCH_FILE_ID = 1;

        private ArrayList<RecordFileInfo> mSelectItemList = new ArrayList<RecordFileInfo>();

        public RecordFileListFragment() {
            super();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setHasOptionsMenu(true);
            mIsMultiSimCard = getActivity().getResources().getBoolean(R.bool.is_multi_sim_card);

            mBottomView = getActivity().findViewById(R.id.botoom_layout);
            Button deleteBt = (Button) mBottomView.findViewById(R.id.delete_sure);
            Button cancelBt = (Button) mBottomView.findViewById(R.id.delete_cancel);
            deleteBt.setOnClickListener(this);
            cancelBt.setOnClickListener(this);

            mEmptyText = (TextView) getActivity().findViewById(R.id.empty_tv);

            mFileListView = getListView();
            // mFileListView.setOnCreateContextMenuListener(this);

            mListAdapter = new FileListAdapter();
            setListAdapter(mListAdapter);

            mFileListView.setOnItemSelectedListener(new ItemSelectedListener());

            // setListShown(false);
            getLoaderManager().initLoader(0, null, this);
        }

        private class ItemSelectedListener implements OnItemSelectedListener {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                RecordFileInfo info = mListAdapter.getFilesInfo().get(position);
                mSelectItemList.clear();
                mSelectItemList.add(info);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            if (mListAdapter != null) {
                ArrayList<RecordFileInfo> infos = mListAdapter.getFilesInfo();
                if (infos == null || infos != null && (infos.size() == 0)) {
                    menu.setGroupVisible(0, false);
                } else {
                    menu.setGroupVisible(0, true);
                }
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.add(0, MENU_SELECT_ALL_ID, 0, R.string.action_bar_menu_select_all);
            // Place an action bar item for searching.
            MenuItem item = menu.add(0, MENU_SEARCH_FILE_ID, 0, R.string.search);
            item.setIcon(android.R.drawable.ic_menu_search);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
                    | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            mFileSearchView = new FileSearchView(getActivity());
            mFileSearchView.setOnQueryTextListener(this);
            mFileSearchView.setOnCloseListener(this);
            mFileSearchView.setIconifiedByDefault(true);
            item.setActionView(mFileSearchView);
        }

        public class FileSearchView extends SearchView {
            public FileSearchView(Context context) {
                super(context);
            }

            @Override
            public void onActionViewCollapsed() {
                mLoader.forceLoad();
                super.onActionViewCollapsed();
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            switch (item.getItemId()) {
                case MENU_SELECT_ALL_ID:
                    mSelectItemList.clear();
                    mSelectItemList.addAll(mListAdapter.getFilesInfo());
                    mBottomView.setVisibility(View.VISIBLE);
                    break;

                default:
                    break;
            }

            mListAdapter.notifyDataSetChanged();

            if (item.getItemId() == android.R.id.home) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }

            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            RecordFileInfo info = mListAdapter.getFilesInfo().get(position);
            launchAudioPreview(info);
        }
        
        public void launchAudioPreview(RecordFileInfo info){
            File audioFile = new File(info.position);
            Uri fileUri = Uri.fromFile(audioFile);
            Intent intent = new Intent();
            intent.setData(fileUri);
            intent.setClass(getActivity().getApplicationContext(), AudioPreview.class);
            startActivity(intent);
        }
        
        @Override
        public void onDestroyView() {
            super.onDestroyView();
        }

        private String formatFileSizeString(long size) {
            String ret = "";
            if (size >= 1024) {
                ret = convertStorage(size);
                ret += (" (" + getResources().getString(R.string.file_size, size) + ")");
            } else {
                ret = getResources().getString(R.string.file_size, size);
            }
            return ret;
        }

        public static String convertStorage(long size) {
            long kb = 1024;
            long mb = kb * 1024;
            long gb = mb * 1024;

            if (size >= gb) {
                return String.format("%.1f GB", (float) size / gb);
            } else if (size >= mb) {
                float f = (float) size / mb;
                return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
            } else if (size >= kb) {
                float f = (float) size / kb;
                return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
            } else
                return String.format("%d B", size);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            menu.add(0, DELETE_ITEM, 0, R.string.delete_record_file);
            if (!mIsMultiSimCard) {
                menu.add(0, SELECT_AS_RINGTONE, 0, R.string.set_as_ringtone);
            }
            menu.add(0, SHARE_ITEM, 0, R.string.share);
            menu.add(0, ITEM_DETAIL, 0, R.string.item_detail);
        }

        @Override
        public boolean onContextItemSelected(MenuItem item) {
            AdapterView.AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            int listItemPosition = info.position;
            ArrayList<RecordFileInfo> filesInfo = mListAdapter.getFilesInfo();
            RecordFileInfo fileInfo = filesInfo.get(listItemPosition);
            long fileId = fileInfo.id;

            switch (item.getItemId()) {
                case DELETE_ITEM:
                    long[] list = new long[1];
                    list[0] = fileId;
                    deleteRecordFiles(list);
                    break;
                case SELECT_AS_RINGTONE:
                    setRingtone(getActivity(), fileId);
                    break;
                case SHARE_ITEM:
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("audio/*");
                    Uri uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fileInfo.id);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(shareIntent);
                    break;
                case ITEM_DETAIL:
                    showInformationDialog(fileInfo);
                    break;

                default:
                    break;
            }
            return super.onContextItemSelected(item);
        }

        private void showInformationDialog(RecordFileInfo info) {

            if (info == null) {
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.information_dialog, null);
            TextView locationView = (TextView) view.findViewById(R.id.information_location);
            TextView sizeView = (TextView) view.findViewById(R.id.information_size);
            TextView dateView = (TextView) view.findViewById(R.id.information_date);
            TextView typeView = (TextView) view.findViewById(R.id.information_type);

            locationView.setText(convertDisplayPath(info.position));
            sizeView.setText(formatFileSizeString(info.size));
            dateView.setText(timeFormart(getActivity(), info.date));
            typeView.setText(guessExtensionFromMimeType(info.type));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                    AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            builder.setTitle(info.name);
            builder.setView(view);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.create().show();
        }

        public String guessExtensionFromMimeType(String mimeType) {

            if (mimeType == null || mimeType.isEmpty()) {
                return "";
            }

            if (TextUtils.equals("audio/3gpp", mimeType)) {
                return "3gpp";
            } else if (TextUtils.equals("audio/ogg", mimeType)) {
                return "ogg";
            } else if (TextUtils.equals("audio/aac", mimeType)) {
                return "aac";
            } else if (TextUtils.equals("audio/amr", mimeType)) {
                return "amr";
            } else if (TextUtils.equals("audio/midi", mimeType)) {
                return "mid";
            } else if (TextUtils.equals("audio/x-wav", mimeType)) {
                return "wav";
            } else {
                return getString(R.string.file_info_type_unknow);
            }
        }

        public String convertDisplayPath(String path) {
            StorageHelper helper = StorageHelper.getInstance(getActivity());
            ArrayList<Storage> storageList = helper.getStorageList();
            for (Storage storage : storageList) {
                if (path.contains(storage.mountPoint)) {
                    CharSequence subSequence = path.subSequence(storage.mountPoint.length(),
                            path.length());
                    String storageName = getString(storage.descriptionId);
                    return storageName + subSequence;
                }
            }
            return "";
        }

        public void setRingtone(Context context, long id) {
            ContentResolver resolver = context.getContentResolver();
            // Set the flag in the database to mark this as a ringtone
            Uri ringUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id);
            try {
                ContentValues values = new ContentValues(2);
                values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
                values.put(MediaStore.Audio.Media.IS_ALARM, "1");
                resolver.update(ringUri, values, null, null);
            } catch (UnsupportedOperationException ex) {
                // most likely the card just got unmounted
                Log.e(TAG, "couldn't set ringtone flag for id " + id);
                return;
            }

            String[] cols = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE
            };

            String where = MediaStore.Audio.Media._ID + "=" + id;
            Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    cols, where, null, null);
            try {
                if (cursor != null && cursor.getCount() == 1) {
                    // Set the system setting to make this the current ringtone
                    cursor.moveToFirst();
                    String message = context.getString(R.string.ringtone_set, cursor.getString(2));

                    android.media.RingtoneManager.setActualDefaultRingtoneUri(context,
                            android.media.RingtoneManager.TYPE_RINGTONE, ringUri);

                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        private void deleteRecordFiles(final long[] items) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                    AlertDialog.THEME_DEVICE_DEFAULT_DARK);
            builder.setMessage(R.string.delete_file_warn);
            builder.setTitle(android.R.string.dialog_alert_title);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            deleteTracks(getActivity(), items);
                        }
                    }).start();

                    if (mFileSearchView != null && mFileSearchView.isShown()
                            && !TextUtils.isEmpty(mFileSearchView.getQuery())) {
                        mListAdapter.deleteItemsFromOriginal();
                    } else {
                        mLoader.forceLoad();
                    }
                    mSelectItemList.clear();
                    mBottomView.setVisibility(View.GONE);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        }

        private void deleteTracks(Context context, long[] items) {
            String[] cols = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID
            };
            StringBuilder where = new StringBuilder();
            where.append(MediaStore.Audio.Media._ID + " IN (");
            for (int i = 0; i < items.length; i++) {
                where.append(items[i]);
                if (i < items.length - 1) {
                    where.append(",");
                }
            }
            where.append(")");

            Cursor c = query(context, MediaStore.Files.getContentUri("external"), cols,
                    where.toString(), null, null);

            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    where.toString(), null);

            c.moveToFirst();
            int failedDeleteCount = 0;
            while (!c.isAfterLast()) {
                String name = c.getString(1);
                File f = new File(name);
                try { // File.delete can throw a security exception
                    if (!f.delete()) {
                        failedDeleteCount++;
                        // I'm not sure if we'd ever get here (deletion would
                        // have to fail, but no exception thrown)
                        Log.e(TAG, "Failed to delete file " + name);
                    } else {
                        context.getContentResolver().delete(
                                MediaStore.Files.getContentUri("external"),
                                MediaStore.Audio.Media.DATA + " =? ", new String[] {
                                    name
                                });
                    }
                    c.moveToNext();
                } catch (SecurityException ex) {
                    c.moveToNext();
                }
            }
            c.close();

            int deleteSucc = items.length - failedDeleteCount;
            String deleteWarnStr = "";
            if (deleteSucc > 0) {
                deleteWarnStr = String.format(context.getString(R.string.delete_succ_warn),
                        deleteSucc);
            }
            if (failedDeleteCount > 0) {
                deleteWarnStr = deleteWarnStr
                        + String.format(context.getString(R.string.delete_fail_warn),
                                failedDeleteCount);
            }
            final String warning = deleteWarnStr;
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getActivity(), warning, Toast.LENGTH_SHORT).show();
                    mLoader.forceLoad();
                }
            });

            // We deleted a number of tracks, which could affect any number of
            // things
            // in the media content domain, so update everything.
            context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        }

        private Cursor query(Context context, Uri uri, String[] projection,
                String selection, String[] selectionArgs, String sortOrder, int limit) {
            try {
                ContentResolver resolver = context.getContentResolver();
                if (resolver == null) {
                    return null;
                }
                if (limit > 0) {
                    uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
                }
                return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
            } catch (UnsupportedOperationException ex) {
                return null;
            }

        }

        private Cursor query(Context context, Uri uri, String[] projection,
                String selection, String[] selectionArgs, String sortOrder) {
            return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
        }

        private class FileListAdapter extends BaseAdapter implements Filterable {
            private LayoutInflater inflater;
            private ArrayList<RecordFileInfo> infoData;
            private final Object mLock = new Object();
            private final Object mLock2 = new Object();
            FileFilter mFilter;
            private ArrayList<RecordFileInfo> mOriginalValues;

            public FileListAdapter() {
                inflater = LayoutInflater.from(getActivity());
            }

            public void setData(ArrayList<RecordFileInfo> data) {
                synchronized (mLock2) {
                    infoData = data;
                    notifyDataSetChanged();
                }
            }

            public ArrayList<RecordFileInfo> getFilesInfo() {
                synchronized (mLock2) {
                    return infoData;
                }
            }

            @Override
            public int getCount() {
                return infoData == null ? 0 : infoData.size();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                FileListViewHolder holder = FileListViewHolder.createOrRecycle(inflater,
                        convertView, parent);
                holder.fileName.setText(infoData.get(position).name);
                String dateStr = timeFormart(getActivity(), infoData.get(position).date);
                holder.date.setText(dateStr);
                holder.duration.setText(infoData.get(position).duration);
                // holder.selectCb.setChecked(mSelectItemList.contains(infoData.get(position)));
                // holder.selectCb.setOnCheckedChangeListener(null);
                // holder.selectCb.setOnCheckedChangeListener(new
                // MyOnCheckedChangeListener(position));
                return holder.rootView;
            }

            public void deleteItemsFromOriginal() {
                if (mOriginalValues != null && mOriginalValues.size() > 0) {
                    for (RecordFileInfo file : mSelectItemList) {
                        mOriginalValues.remove(file);
                    }
                    String queryString = mFileSearchView.getQuery().toString();
                    mFileSearchView.setQuery(null, false);
                    mFileSearchView.setQuery(queryString, true);
                }
            }

            @Override
            public Filter getFilter() {
                if (mFilter == null) {
                    mFilter = new FileFilter();
                }
                return mFilter;
            }

            private class FileFilter extends Filter {

                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (mOriginalValues == null) {
                        mOriginalValues = new ArrayList<RecordFileInfo>(infoData);
                    }
                    if (constraint == null || constraint.length() == 0) {
                        results.values = mOriginalValues;
                        results.count = mOriginalValues.size();
                    } else {
                        String prefixString = constraint.toString();
                        final int count = mOriginalValues.size();
                        synchronized (mLock) {
                            final ArrayList<RecordFileInfo> newInfoData = new ArrayList<RecordFileInfo>();
                            for (int i = 0; i < count; i++) {
                                if (mOriginalValues.get(i).name.contains(prefixString)) {
                                    newInfoData.add(mOriginalValues.get(i));
                                }
                            }
                            results.values = newInfoData;
                            results.count = newInfoData.size();
                        }
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    infoData = (ArrayList<RecordFileInfo>) results.values;
                    setData(infoData);
                }
            }
        }

        private class MyOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
            private int position;
            private Button deleteBt;

            public MyOnCheckedChangeListener(int position) {
                this.position = position;
            }

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                RecordFileInfo info = mListAdapter.getFilesInfo().get(position);
                if (isChecked) {
                    if (!mSelectItemList.contains(info)) {
                        mSelectItemList.add(info);
                    }
                } else {
                    if (mSelectItemList.contains(info)) {
                        mSelectItemList.remove(info);
                    }
                }
                if (mSelectItemList.size() > 0) {
                    mBottomView.setVisibility(View.VISIBLE);
                } else {
                    mBottomView.setVisibility(View.GONE);
                }
            }

        }

        public static class FileListLoader extends AsyncTaskLoader<ArrayList<RecordFileInfo>> {
            private Context context;
            String[] cols = new String[] {
                    MediaStore.Audio.Playlists._ID,
                    MediaStore.Audio.Playlists.NAME
            };

            String[] mFileCols = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ARTIST_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.IS_MUSIC,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.DATE_ADDED
            };

            public FileListLoader(Context context) {
                super(context);
                this.context = context;
            }

            @Override
            protected void onStartLoading() {
                forceLoad();
            }

            @Override
            protected void onStopLoading() {

            }

            @Override
            public ArrayList<RecordFileInfo> loadInBackground() {
                ArrayList<RecordFileInfo> infos = new ArrayList<RecordFileInfo>();
                StorageHelper storageHelp = StorageHelper.getInstance(getContext());
                ArrayList<Storage> storgeList = storageHelp.getMountedStorageList();
                for (Storage storage : storgeList) {
                    File sampleDir = new File(storage.mountPoint + Recorder.SAMPLE_DEFAULT_DIR);
                    if (sampleDir != null && sampleDir.exists()) {
                        ContentResolver resolver = context.getContentResolver();
                        Uri uri = MediaStore.Files.getContentUri("external");
                        File[] files = sampleDir.listFiles();
                        for (int i = 0; i < files.length; i++) {
                            String fileName = files[i].getPath();
                            Cursor cursor = resolver.query(uri, mFileCols, "_data =?",
                                    new String[] {
                                        fileName
                                    }, null);
                            if (cursor != null && cursor.getCount() > 0) {
                                constructFileInfo(infos, cursor);
                            }
                            if (!cursor.isClosed()) {
                                cursor.close();
                            }
                        }
                    }
                }
                return infos;
            }

            private void constructFileInfo(ArrayList<RecordFileInfo> infos, Cursor filesCursor) {
                int fileIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int durationIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dateIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
                int typeIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
                int sizeIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
                int dataIdx = filesCursor
                        .getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                while (filesCursor.moveToNext()) {
                    RecordFileInfo fileInfo = new RecordFileInfo();
                    fileInfo.name = filesCursor.getString(titleIdx);
                    long duration = filesCursor.getLong(durationIdx);
                    fileInfo.duration = formatDuring(duration);
                    long date = filesCursor.getLong(dateIdx);
                    fileInfo.date = date;
                    fileInfo.type = filesCursor.getString(typeIdx);
                    fileInfo.id = filesCursor.getLong(fileIdx);
                    fileInfo.size = filesCursor.getLong(sizeIdx);
                    fileInfo.position = filesCursor.getString(dataIdx);
                    infos.add(fileInfo);
                }
            }

            private String formatDuring(long mss) {
                long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
                long seconds = (mss % (1000 * 60)) / 1000;
                StringBuilder builder = new StringBuilder();
                if (hours > 0) {
                    builder.append(hours).append(context.getString(R.string.hour));
                }
                if (minutes > 0) {
                    builder.append(minutes).append(context.getString(R.string.minute));
                }
                if (seconds > 0) {
                    builder.append(seconds).append(context.getString(R.string.second));
                }
                return builder.toString();
            }

        }

        private String timeFormart(Context context, long time) {
            Date date = new Date(time * 1000);
            SimpleDateFormat formatter = new SimpleDateFormat(
                    context.getString(R.string.audio_db_title_format));
            return formatter.format(date);
        }

        static class FileListViewHolder {
            TextView fileName;
            TextView date;
            TextView duration;
            TextView type;
            View rootView;

            // CheckBox selectCb;

            public static FileListViewHolder createOrRecycle(LayoutInflater inflater,
                    View convertView,
                    ViewGroup parent) {
                if (convertView == null) {
                    FileListViewHolder holder = new FileListViewHolder();
                    convertView = inflater.inflate(R.layout.navigation_adapter, null);
                    holder.rootView = convertView;
                    holder.fileName = (TextView) convertView.findViewById(R.id.file_name);
                    holder.date = (TextView) convertView.findViewById(R.id.record_date);
                    holder.duration = (TextView) convertView.findViewById(R.id.record_duration);
                    // holder.selectCb = (CheckBox)
                    // convertView.findViewById(R.id.cb_select);
                    convertView.setTag(holder);
                    return holder;
                } else {
                    return (FileListViewHolder) convertView.getTag();
                }
            }
        }

        @Override
        public Loader<ArrayList<RecordFileInfo>> onCreateLoader(int id, Bundle args) {
            if (mLoader == null) {
                mLoader = new FileListLoader(getActivity());
            }
            return mLoader;
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<RecordFileInfo>> loader,
                ArrayList<RecordFileInfo> data) {
            if (mSelectItemList.size() == 0) {
                mBottomView.setVisibility(View.GONE);
            }

            mEmptyText.setVisibility((data != null && data.size() > 0) ? View.GONE : View.VISIBLE);

            String optionStr = getResources().getString(R.string.option);
            ((BaseActivity) getActivity()).setLeftBtnText((data != null && data.size() > 0) ? optionStr : null);

            if (mFileSearchView != null && !mFileSearchView.isShown()) {
                getActivity().invalidateOptionsMenu();
            }
            mListAdapter.setData(data);
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<ArrayList<RecordFileInfo>> loader) {

        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.delete_sure:
                    long[] items = new long[mSelectItemList.size()];
                    for (int i = 0; i < items.length; i++) {
                        items[i] = mSelectItemList.get(i).id;
                    }
                    deleteRecordFiles(items);
                    break;
                case R.id.delete_cancel:
                    mSelectItemList.clear();
                    mListAdapter.notifyDataSetChanged();
                    mBottomView.setVisibility(View.GONE);
                    break;

                default:
                    break;
            }
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
            // Don't do anything if the filter hasn't actually changed.
            // Prevents restarting the loader when restoring state.
            if (mCurFilter == null && newFilter == null) {
                return true;
            }
            if (mCurFilter != null && mCurFilter.equals(newFilter)) {
                return true;
            }
            mCurFilter = newFilter;
            mListAdapter.getFilter().filter(mCurFilter);
            return true;
        }

        @Override
        public boolean onClose() {
            if (!TextUtils.isEmpty(mFileSearchView.getQuery())) {
                mFileSearchView.setQuery(null, true);
            }
            return true;
        }

        public ArrayList<RecordFileInfo> getmSelectItemList() {
            return mSelectItemList;
        }
        
    }

    public static class RecordFileInfo implements Parcelable{
        private String name;
        private long date;
        private String duration;
        private String type;
        private long id;
        private long size;
        private String position;

        @Override
        public boolean equals(Object object) {
            RecordFileInfo info = null;
            if (object instanceof RecordFileInfo) {
                info = (RecordFileInfo) object;
            }
            if (info == null) {
                return false;
            }
            return TextUtils.equals(position, info.position)
                    && TextUtils.equals(type, info.type);
        }

        public RecordFileInfo(){
        
        }
        public RecordFileInfo(Parcel in){
            this.id = in.readLong() ;
            this.name = in.readString();
            this.date = in.readLong() ;
            this.duration = in.readString();
            this.type = in.readString();
            this.size = in.readLong();
            this.position = in.readString();
        }

        public static Parcelable.Creator<RecordFileInfo> CREATOR = new Creator<RecordFileInfo>() {
            @Override
            public RecordFileInfo createFromParcel(Parcel source) {
                return new RecordFileInfo(source);
            }

            @Override
            public RecordFileInfo[] newArray(int size) {
                return new RecordFileInfo[size];
            }
        };
        
        @Override
        public int describeContents() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(id) ;
            dest.writeString(name);
            dest.writeLong(date);
            dest.writeString(duration);
            dest.writeString(type);
            dest.writeLong(size);
            dest.writeString(position);
        }
    }

    @Override
    public Button BuildLeftBtn(Button v) {
        v.setText(R.string.option);
        return v;
    }

    @Override
    public Button BuildMiddleBtn(ImageButton v) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Button BuildRightBtn(Button v) {
        v.setText(R.string.back);
        return v;
    }

    @Override
    public TextView BuildTopTitle(TextView v) {
        v.setText(R.string.recording_file_list);
        return v;
    }

    @Override
    public void onLeftKeyPress() {
        mRecordFileListFragment = (RecordFileListFragment) getFragmentManager().findFragmentById(R.id.file_list_content) ;
        Intent intent = new Intent();
        intent.putExtra(KEY_SELECTED_RECORD, mRecordFileListFragment.getmSelectItemList());
        intent.setClass(this, RecordOperateList.class);
//        startActivity(intent);
        startActivityForResult(intent, RECORD_REQUESETCODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RECORD_REQUESETCODE:
            	if(data != null){
	                ArrayList<RecordFileInfo> mSelectItemList = data.getParcelableArrayListExtra(KEY_SELECTED_RECORD);
	                if(mRecordFileListFragment != null &&  mSelectItemList != null && mSelectItemList.size() > 0){
	                    if(RECORD_PLAY_RESULTCODE == resultCode){
	                        mRecordFileListFragment.launchAudioPreview(mSelectItemList.get(0));
	                    }else{
	                        long[] items = new long[mSelectItemList.size()];
	                        for (int i = 0; i < items.length; i++) {
	                            items[i] = mSelectItemList.get(i).id;
	                        }
	                        mRecordFileListFragment.deleteRecordFiles(items);
	                    }
	                }
                }
                break;

            default:
                break;
        }
    }
    
    @Override
    public void onMiddleKeyPress() {

    }

    @Override
    public void onRightKeyPress() {
        finish();
    }
}
