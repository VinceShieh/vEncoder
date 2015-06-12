package com.mkyong.android;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import android.widget.Toast;

public class CustomOnItemSelectedListener implements OnItemSelectedListener {

	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		/*
		Toast.makeText(parent.getContext(), 
				"OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString()+",pos:"+parent.getSelectedItemPosition(),
				Toast.LENGTH_SHORT).show();
		*/
		if(parent.getSelectedItemPosition()==1){
			Toast.makeText(parent.getContext(), 
					"Encoding output data will be written to /sdcard/Movies/output_xxx.h264",
					Toast.LENGTH_SHORT).show();
		}
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

}