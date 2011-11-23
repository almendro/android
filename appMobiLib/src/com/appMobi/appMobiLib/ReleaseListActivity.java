package com.appMobi.appMobiLib;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ReleaseListActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		Intent intent = getIntent();
		String[] releaseList = intent.getStringArrayExtra("releases");
		
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, releaseList));
		ListView lv = getListView();
    	lv.setTextFilterEnabled(true);

    	lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		// When clicked, show a toast with the TextView text
        		Intent intent = getIntent();
        		intent.putExtra("RELEASE", ((TextView) view).getText());
        		ReleaseListActivity.this.setResult(RESULT_OK, intent);
        		finish();
        	}
    	});
	}
}
