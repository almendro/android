package fm.flycast;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.appMobi.appMobiLib.R;

public class SleepTimerActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		String[] releaseList = { "15 minutes", "30 minutes", "45 minutes" };
		
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, releaseList));
		ListView lv = getListView();
    	lv.setTextFilterEnabled(true);

    	lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        		// When clicked, show a toast with the TextView text
        		Intent intent = getIntent();
        		String choice = ((TextView) view).getText().toString();
        		intent.putExtra("TIMER", choice.substring(0, 2));
        		SleepTimerActivity.this.setResult(RESULT_OK, intent);
        		finish();
        	}
    	});
	}
}
