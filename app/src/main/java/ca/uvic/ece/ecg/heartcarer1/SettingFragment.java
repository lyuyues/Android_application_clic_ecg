package ca.uvic.ece.ecg.heartcarer1;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class SettingFragment extends Fragment {
	private static android.app.FragmentManager fMgr; 
	private View view;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		fMgr = getChildFragmentManager();

		view = inflater.inflate(R.layout.activity_main1, container, false);	
		Fragment ProfileSettingFragment=new ProfileSettingFragment();
		getFragmentManager().beginTransaction().replace(R.id.fragmentRoot, ProfileSettingFragment, "ProfileSettingFragment").commit();					

			dealBottomButtonsClickEvent();
		return view;
		
	}
	
	private void dealBottomButtonsClickEvent() { 
		view.findViewById(R.id.ProfileSet).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Fragment ProfileSettingFragment=new ProfileSettingFragment();
				getFragmentManager().beginTransaction().replace(R.id.fragmentRoot, ProfileSettingFragment, "ProfileSettingFragment").commit();					

			}
		});
		view.findViewById(R.id.GeneralSet).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				popAllFragmentsExceptTheBottomOne();
				//AddressFragment sf = new AddressFragment();
				Fragment GeneralSettingFragment=new GeneralSettingFragment();
				getFragmentManager().beginTransaction().replace(R.id.fragmentRoot, GeneralSettingFragment, "GeneralSettingFragment").commit();	

				/*FragmentTransaction ft = fMgr.beginTransaction();
				ft.hide(fMgr.findFragmentByTag("weiXinFragment"));
				AddressFragment sf = new AddressFragment();
				ft.add(R.id.fragmentRoot, sf, "AddressFragment");
				ft.addToBackStack("AddressFragment");
				ft.commit();*/
				
			}
		});
		view.findViewById(R.id.NotificationSet).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				popAllFragmentsExceptTheBottomOne();
				Fragment NotificationSettingFragment = new NotificationSettingFragment();
				getFragmentManager().beginTransaction().replace(R.id.fragmentRoot, NotificationSettingFragment, "NotificationSettingFragment").commit();	

				/*FragmentTransaction ft = fMgr.beginTransaction();
				ft.hide(fMgr.findFragmentByTag("weiXinFragment"));
				FindFragment sf = new FindFragment();
				ft.add(R.id.fragmentRoot, sf, "AddressFragment");
				ft.addToBackStack("FindFragment");
				ft.commit();*/
			}
		});
	
	}
	 
	/**
	 * ��back stack�������е�fragment��������ҳ���Ǹ�
	 */
	public static void popAllFragmentsExceptTheBottomOne() {
		for (int i = 0, count = fMgr.getBackStackEntryCount() - 1; i < count; i++) {
			fMgr.popBackStack();
		}
	}
	//������ذ�ť
	public void onBackPressed() {
		if(fMgr.findFragmentByTag("weiXinFragment")!=null && fMgr.findFragmentByTag("weiXinFragment").isVisible()) {
			getActivity().finish();
		} 
	}
}
