
package com.stj.soundrecorder;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.Arrays;

public class RecordOperateList extends BaseActivity implements BaseActivity.BottomKeyClickListener {

    private View mRootView;
    private BaseMenuItemAdapter mAdapter;
    private ListView mListView;
    private Uri mSelectedAudioFileUri ;
    
    private enum RecordOperate{
        PLAY,DELETE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMiddleViewStub.setLayoutResource(R.layout.record_operate_main);
        mRootView = mMiddleViewStub.inflate();

        setBottomKeyClickListener(this);
        setActivityBgResource(0);

        String[] menuItems = getResources().getStringArray(R.array.record_operate_menu);
        mAdapter = new BaseMenuItemAdapter(this, Arrays.asList(menuItems));
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new MenuItemClickListener());
    }

    private class MenuItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(position == RecordOperate.PLAY.ordinal()){
                setResult(RecordFileListActivity.RECORD_PLAY_RESULTCODE,getIntent());
            }else if(position == RecordOperate.DELETE.ordinal()){
                setResult(RecordFileListActivity.RECORD_DELETE_RESULTCODE,getIntent());
            }
            finish();
        }
    }

    @Override
    public Button BuildLeftBtn(Button v) {
        return null;
    }

    @Override
    public Button BuildMiddleBtn(ImageButton v) {
        return null;
    }

    @Override
    public Button BuildRightBtn(Button v) {
        v.setText(R.string.back);
        return v;
    }

    @Override
    public TextView BuildTopTitle(TextView v) {
        v.setText(R.string.soundrecorder_title);
        return v;
    }

    @Override
    public void onLeftKeyPress() {

    }

    @Override
    public void onMiddleKeyPress() {

    }

    @Override
    public void onRightKeyPress() {
        finish();
    }

}
