
package com.stj.soundrecorder;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.stj.soundrecorder.emoji.Emojicon;
import com.stj.soundrecorder.emoji.Nature;
import com.stj.soundrecorder.emoji.Objects;
import com.stj.soundrecorder.emoji.People;
import com.stj.soundrecorder.emoji.Places;
import com.stj.soundrecorder.emoji.Symbols;


import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class RecordNameEditText extends EditText {

    private Context mContext;

    private InputMethodManager mInputMethodManager;

    private OnNameChangeListener mNameChangeListener;

    private String mDir;

    private String mExtension;

    private String mOriginalName;

    private LocalFileNameFilter mFileNameFilter;

    private boolean mIsEnglishOnly = false;

    private static final boolean USE_ENGLIST_FILE_NAME = true;

    private Emojicon[][] mEmojis = {
            Nature.DATA, Objects.DATA, People.DATA, Places.DATA, Symbols.DATA
    };

    public interface OnNameChangeListener {

        void onNameChanged(String name);
    }

    public RecordNameEditText(Context context) {
        super(context, null);
        mContext = context;
        mInputMethodManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mNameChangeListener = null;
        mFileNameFilter = new LocalFileNameFilter();
    }

    public RecordNameEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mInputMethodManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mNameChangeListener = null;
        mFileNameFilter = new LocalFileNameFilter();
    }

    public RecordNameEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mInputMethodManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mNameChangeListener = null;
        mFileNameFilter = new LocalFileNameFilter();
    }

    public void setNameChangeListener(OnNameChangeListener listener) {
        mNameChangeListener = listener;
    }

    public void initFileName(String dir, String extension, boolean englishOnly) {
        mDir = dir;
        mExtension = extension;
        mIsEnglishOnly = englishOnly;
        // initialize the default name
        if (!USE_ENGLIST_FILE_NAME && !englishOnly) {
            setText(getProperFileName(mContext.getString(R.string.default_record_name)));
        } else {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            //setText(getProperFileName("rec_" + dataFormat.format(new Date())));
            setText(getProperFileName(dataFormat.format(new Date())));
        }
    }

    private String getProperFileName(String name) {
        String uniqueName = name;

        if (isFileExisted(uniqueName)) {
            int i = 2;
            while (true) {
                String temp = uniqueName + "(" + i + ")";
                if (!isFileExisted(temp)) {
                    uniqueName = temp;
                    break;
                }
                i++;
            }
        }
        return uniqueName;
    }

    private boolean isFileExisted(String name) {
        String fullName = mDir + "/" + name.trim() + mExtension;
        File file = new File(fullName);
        return file.exists();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                if (mNameChangeListener != null) {
                    String name = getText().toString().trim();
                    if (!TextUtils.isEmpty(name)) {
                        // use new name
                        setText(name);
                        mNameChangeListener.onNameChanged(name);

                    } else {
                        // use original name
                        setText(mOriginalName);
                    }
                    clearFocus();

                    // hide the keyboard
                    mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (!focused && mNameChangeListener != null) {
            String name = getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                // use new name
                setText(name);
                mNameChangeListener.onNameChanged(name);

            } else {
                // use original name
                setText(mOriginalName);
            }

            // hide the keyboard
            mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        } else if (focused) {
            mOriginalName = getText().toString();
        }
    }

    private class LocalFileNameFilter implements FilenameFilter {
        Pattern pattern;
        String regex = "[^\\\\/:*?\"<>|]+";

        public LocalFileNameFilter() {
            pattern = Pattern.compile(regex);
        }

        @Override
        public boolean accept(File dir, String filename) {
            return pattern.matcher(filename).matches();
        }

    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        String name = text.toString();
        if (mFileNameFilter != null && !TextUtils.isEmpty(name)) {
            boolean isAccept = mFileNameFilter.accept(null, text.toString());
            String addStr = name.substring(start, start + lengthAfter);
            for (int i = 0; i < mEmojis.length; i++) {
                for (int j = 0; j < mEmojis[i].length; j++) {
                    String emoji = mEmojis[i][j].getEmoji();
                    if (TextUtils.equals(emoji, addStr)) {
                        isAccept = false;
                        break;
                    }
                }
            }

            if (!isAccept) {
                Toast.makeText(mContext, R.string.not_access_file_name, Toast.LENGTH_LONG).show();
                name = name.replace(name.subSequence(start, start + lengthAfter), "");
                if(TextUtils.isEmpty(name)){
                    initFileName(mDir, mExtension, mIsEnglishOnly);
                    setSelection(getText().toString().length());
                }else{
                    setText(name);
                    setSelection(start);
                }
            }
        }
    }
}
