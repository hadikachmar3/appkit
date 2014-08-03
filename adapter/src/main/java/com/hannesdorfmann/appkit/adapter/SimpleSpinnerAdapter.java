package com.hannesdorfmann.appkit.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import com.hannesdorfmann.appkit.dagger.Injector;

/**
 * The adapter for spinners
 * 
 * @author Hannes Dorfmann
 * 
 */
public abstract class SimpleSpinnerAdapter<D> extends SimpleAdapter<D>
    implements SpinnerAdapter {


	public SimpleSpinnerAdapter(Context context, Injector injector) {
		super(context, injector);
	}


	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = newDropDownView(parent);
		}

		bindDropDownView(position, convertView);
		return convertView;

	}

	protected abstract View newDropDownView(ViewGroup parent);

	protected abstract void bindDropDownView(int position, View view);



}
