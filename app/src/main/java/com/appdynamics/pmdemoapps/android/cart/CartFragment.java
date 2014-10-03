package com.appdynamics.pmdemoapps.android.cart;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.appdynamics.eumagent.runtime.InfoPoint;
import com.appdynamics.eumagent.runtime.Instrumentation;
import com.appdynamics.pmdemoapps.android.cart.misc.Constants;
import com.appdynamics.pmdemoapps.android.cart.misc.GlobalDataProvider;
import com.appdynamics.pmdemoapps.android.cart.model.Item;
import com.appdynamics.pmdemoapps.android.cart.service.http.DeleteRequestService;
import com.appdynamics.pmdemoapps.android.cart.service.http.GetRequestService;

public class CartFragment extends  SherlockListFragment { 

	
	public static List<Item> currentCartItems = new ArrayList<Item>();
	public static Map<Long,Item> currentCartItemsMap = new ConcurrentHashMap<Long,Item>();
	
	/**
	 * The fragment's current callback object
	 */
	private Callbacks mCallbacks = sDummyCallbacks;
	
	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		public void storeCartFragment(CartFragment cartFragment);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void storeCartFragment(CartFragment cartFragment) {
		}
	};
	
	
	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public CartFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setListAdapter(new ArrayAdapter<Item>(getActivity(),
				android.R.layout.simple_list_item_multiple_choice,
				android.R.id.text1, currentCartItems));
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_cart,
				container, false);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		ListView lview=getListView();
	    lview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);        
	    lview.setTextFilterEnabled(true);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
		mCallbacks.storeCartFragment(this);
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}
	
	
	/*@Override
	public void onListItemClick( ListView l, View v, int position, long id)
	{
	  CheckedTextView textView = (CheckedTextView)v;
	  textView.setChecked(!textView.isChecked());
	}*/

	
	//When called in the context of the same fragment, update the underlying data
	public void convertItemsMaptoList(){
		convertItemsMaptoListStatic();
		((BaseAdapter)this.getListAdapter()).notifyDataSetChanged();
		
	}
	
	//Static Method for calls by non parent activities and other fragments. 
	//Low overhead method. Avoid creating unnecessary items
	public static void convertItemsMaptoListStatic(){
		if (currentCartItemsMap!=null){
			currentCartItems.clear();
			currentCartItems.addAll(currentCartItemsMap.values());
		}
	}
	
/*	public void removeItemFromCart(Item item){
		if(CartFragment.currentCartItemsMap.containsKey(item.getId())){
			CartFragment.currentCartItems.remove(item);
			CartFragment.convertItemsMaptoList();
			
			new DeleteFromCartService().execute(GlobalDataProvider.getInstance().
					getRestServiceUrl()+"cart/"+item.getId());
		}
		
	}
	
	public void removeItemFromCart(List<Item> itemList){
		for (Item item:itemList){
		
			if(CartFragment.currentCartItemsMap.containsKey(item.getId())){
				CartFragment.currentCartItems.remove(item);
				
				new DeleteFromCartService().execute(GlobalDataProvider.getInstance().
						getRestServiceUrl()+"cart/"+item.getId());
			}
		}
		CartFragment.convertItemsMaptoList();
		
	}*/

    @InfoPoint
	public void removeItemFromCart(SparseBooleanArray checkedItems){
		if (checkedItems!=null && currentCartItems!=null && currentCartItems.size()>0){
			boolean atleastOneItemChecked = false;
			for (int i = 0; i < checkedItems.size(); i++) {
			    if(checkedItems.valueAt(i)) {
			    	atleastOneItemChecked = true;
			    	new DeleteFromCartService().execute(GlobalDataProvider.getInstance().
							getRestServiceUrl()+"cart/"+currentCartItems.get(i).getId());
			    	currentCartItemsMap.remove(currentCartItems.get(i).getId());
			     }
			}
			if(atleastOneItemChecked){
				convertItemsMaptoList();//Refresh the list
			}else{
				displayToast("No item was selected for deletion");
			}
		}
		else{
			displayToast("There are no items in the cart");
		}
		
		
	}
	
	private void displayToast(CharSequence text){
		Context context = this.getActivity();
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();

	}
	
	public class DeleteFromCartService extends DeleteRequestService{
		
		@Override
	    protected void onPostExecute(String result) {
			super.onPostExecute(result);
	        //Process the response and populate the list items
	        /*ItemParser parser = new ItemParser();
	        try {
				List<Item> list = parser.parse(result);
				if (list == null){ 
					//TODO - Message - Not able to connect
					list = new ArrayList<Item>();
				}
				if (list!=null && getListAdapter() == null){
					setListAdapter(new ItemListAdapter(parentActivity, list));
				}
				
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
	        
		}
		
	}

    @InfoPoint
	public void checkoutCart(){
		if (currentCartItems!=null && currentCartItems.size()>0){
			CheckoutTask checkoutReq = new CheckoutTask();
            Instrumentation.startTimer("Checkout");
			checkoutReq.execute(getEndpoint() + "cart/co");
			currentCartItemsMap.clear();
			convertItemsMaptoList();
		} else {
			displayToast("There are no items in the cart");
		}
		
		
		
		
	}

public class CheckoutTask extends GetRequestService {
	protected void onPostExecute(String result) {
        Instrumentation.stopTimer("Checkout");
        displayToast(result);
	}
	
}
	
	
public class CartLoginTask extends AsyncTask<Void, Void, String> {
		
		private String error = "";
		
		@Override
		protected String doInBackground(Void... params) {

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(getEndpoint()+"cart/checkout");
			
			 List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			 for(Item item:currentCartItems){
			    nameValuePairs.add(new BasicNameValuePair("itemId", item.getId().toString()));
			    nameValuePairs.add(new BasicNameValuePair("quantity", "1"));
			 }
			 nameValuePairs.add(new BasicNameValuePair("emailId", "harsha.hegde@appdynamics.com"));
			 
			    try {
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				
					HttpResponse response = httpclient.execute(httppost);
					if(response!=null && response.getEntity()!=null){
						String respStr = EntityUtils.toString(response.getEntity());
						return respStr;
					}
					
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace(); 
				}
			    catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			
			return "";
		}

		@Override
		protected void onPostExecute(String success) {
			
			System.out.println(success);
		}

	}


	public String getEndpoint(){
		return GlobalDataProvider.getInstance(). getRestServiceUrl();
		
	}
	
	
}
