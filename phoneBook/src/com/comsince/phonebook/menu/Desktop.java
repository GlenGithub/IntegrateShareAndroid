package com.comsince.phonebook.menu;


import com.comsince.phonebook.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

public class Desktop {
	private Context mContext;
	private Activity mActivity;
	/**
	 * ��ǰ�����View
	 */
	private View mDesktop;
	
	public Desktop(Context context, Activity activity) {
		mContext = context;
		mActivity = activity;
		// �󶨲��ֵ���ǰView
		mDesktop = LayoutInflater.from(context).inflate(R.layout.desktop, null);
		findViewById();
		setListener();
		init();
	}
	
	public void findViewById(){
		
	}
	
	public void setListener(){
		
	}
	
	public void init(){
		
	}
	
	/**
	 * ��ȡ�˵�����
	 * 
	 * @return �˵������View
	 */
	public View getView() {
		return mDesktop;
	}

}
