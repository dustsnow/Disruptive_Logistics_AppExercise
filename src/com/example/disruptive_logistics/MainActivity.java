/**
 * This project duplicate the Disruptive Logistics App Exercise. 
 */
package com.example.disruptive_logistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;;

/**
 * 
 * 
 * @author Peng Hou
 *
 */
public class MainActivity extends Activity implements 
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener
{
	private LocationClient mLocationClient;//LocationClient used to get current location
	private ProgressBar mActivityIndicator;//ProgressBar shows the progress when get current address
	private Location mCurrentLocation;//store the Location data
	private LatLng currentLatLng;//current latitute and longtitute
	private LatLng destinationLatLng;//destination latitute and longtitute
	private EditText current_addr;//string current address
	private EditText destination_addr;//string destination address
	private Button button_current;//icon before current address EditText
	private Button button_destination;//icon before destination address EditText
	private GoogleMap mMap;//GoogleMap object
	private TextView distance;//Textview shows distance of current and destination
	private LatLngBounds cameraBounds;//bound for map. able to show all markers and route
	private Marker currentMarker;//marker for current location
	private Marker destinationMarker;//marker for destination location
	private Context gContext;//Context of this Activity
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/* Initialization of global variables */
		mActivityIndicator = (ProgressBar) findViewById(R.id.address_progress);
	    current_addr = (EditText) findViewById(R.id.current);
	    destination_addr = (EditText) findViewById(R.id.destination);
		mLocationClient = new LocationClient(this, this, this);
		button_current = (Button) findViewById(R.id.button_current);
		button_destination = (Button) findViewById(R.id.button_destination);
	    distance = (TextView) findViewById(R.id.distance);
	    cameraBounds = null;
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap(); 
		destinationMarker = null;
		gContext = this;
		
		//KeyListener of key "done" for EditText destination_addr
		destination_addr.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction()!=KeyEvent.ACTION_DOWN)
		            return false;
		        if(keyCode == KeyEvent.KEYCODE_ENTER ){
		        	//When user enter destination address and press done button, find route and distance
		            new GetLocTask().execute(destination_addr.getText().toString());
		            return true;
		        }
		        return false;
			}
		});
		//MapClickListener to detect click event on the map
		mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			
			@Override
			public void onMapClick(LatLng clickLatLng) {
				if(!isMapEmpty()){//if map is not clean, clear the map
					Log.d("Location","Remove Destination Marker");
					mMap.clear();
				}
				destinationLatLng = clickLatLng;
				Location destinationLocation  = mCurrentLocation;
				destinationLocation.setLatitude(destinationLatLng.latitude);
				destinationLocation.setLongitude(destinationLatLng.longitude);
				new GetRouteTask().execute();//Get route 
				new GetDestinationAddressTask(gContext).execute(destinationLocation);//get destination address
			}
		});
	}
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		mCurrentLocation = mLocationClient.getLastLocation();//get the current LatLng
		currentLatLng = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
		// Move the camera instantly to hamburg with a zoom of 15.
	    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));//Move camera to current location
		// Zoom in, animating the camera.
	    mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);//Animate the moving
		mActivityIndicator.setVisibility(View.VISIBLE);//Show ProgressBar while getting current location
		new GetAddressTask(this).execute(mCurrentLocation);//get current address from LatLng
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	/*
	 * AsyncTask of getting route
	 */
	@SuppressLint("NewApi")
	private class GetRouteTask extends AsyncTask<Void, Void, String> {
		
		@Override
		protected String doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String route_string="";
			try {
				URL url = new URL("http://maps.googleapis.com/maps/api/directions/json?origin="
						+currentLatLng.latitude+","+currentLatLng.longitude
						+"&destination="+destinationLatLng.latitude+","+destinationLatLng.longitude
						+"&sensor=true");//Construct Google Map direction API
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();//HTTPConnection Object
				conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                InputStream is = conn.getInputStream();
                BufferedReader buff_reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                //transfer inputstream to a string
                while ((line = buff_reader.readLine()) != null) {
                    sb.append(line);
                }
                route_string = sb.toString();
			}catch (Exception ex){
            	ex.printStackTrace();
            }
			return route_string;
		}
		@Override
        protected void onPostExecute(String route) {
			//hide keyboard 
			InputMethodManager imm = (InputMethodManager)getSystemService(
				      Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(destination_addr.getWindowToken(), 0);
			
			button_current.setVisibility(View.VISIBLE);//show icon before current address
			button_destination.setVisibility(View.VISIBLE);//show icon before destination address
			try {
				JSONObject mainObject = new JSONObject(route);
				//get distance from JSON result
				String dist = mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getString("text").toString();
				distance.setText("Distance: "+dist);
				//clear map
				if(!isMapEmpty()){
					mMap.clear();
				}
				//add markers
				currentMarker = mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current").icon(BitmapDescriptorFactory.fromResource(R.drawable.letter_a)));
				destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));
				//determine camera bounds from current and destination LatLng
				determineBounds(currentLatLng,destinationLatLng);
				mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cameraBounds,400));

				//draw route
				for(int i = 0; i < mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").length(); i++){
					//for every step get start and stop LatLng, draw the step
					double step_start_lat = mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(i).getJSONObject("start_location").getDouble("lat");
					double step_start_lng = mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(i).getJSONObject("start_location").getDouble("lng");
					LatLng step_start = new LatLng(step_start_lat,step_start_lng);
					double step_stop_lat = mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(i).getJSONObject("end_location").getDouble("lat");
					double step_stop_lng = mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(i).getJSONObject("end_location").getDouble("lng");
					LatLng step_stop = new LatLng(step_stop_lat,step_stop_lng);
					PolylineOptions rectOptions = new PolylineOptions()
					.color(Color.MAGENTA)
					.add(step_start)
					.add(step_stop);
					Polyline polyline = mMap.addPolyline(rectOptions);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e){
				Log.d("Location","Exception: Current = " + currentLatLng.latitude + ", " + currentLatLng.longitude);
				Log.d("Location","Exception: Destination = " + destinationLatLng.latitude + ", " + destinationLatLng.longitude);
				e.printStackTrace();
			}
        }
		
	}
	@SuppressLint("NewApi")
	private class GetLocTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String addr = params[0];
			try {
				addr = addr.replaceAll(",?\\ +","+");//Regular Expression to replace whitespace with plus(+) sign that the google likes
				URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?address="+addr+"&sensor=true");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d("Location", "The response is: " + response);
                InputStream is = conn.getInputStream();
                BufferedReader buff_reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = buff_reader.readLine()) != null) {
                    sb.append(line);
                }
                Log.d("Location","Response String:"+sb.toString());
                JSONObject mainObject = new JSONObject(sb.toString());
                Log.d("Location",mainObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng").toString());
                Log.d("Location",mainObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lat").toString());
                double dest_lng = Double.parseDouble(mainObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lng").toString());
                double dest_lat = Double.parseDouble(mainObject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location").getString("lat").toString());
                destinationLatLng = new LatLng(dest_lat,dest_lng);
                Log.d("Location","Long Lat:"+dest_lng+" " + dest_lat);
			}catch (Exception ex){
            	ex.printStackTrace();
            }
			return null;
		}
		@Override
        protected void onPostExecute(Void a) {
			new GetRouteTask().execute();
        }
	}
	
	private class GetDestinationAddressTask extends AsyncTask<Location, Void, String> {
		Context mContext;
		public GetDestinationAddressTask(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected String doInBackground(Location... params) {
		    Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
		    // Get the current location from the input parameter list
		    Location loc = params[0];
		    // Create a list to contain the result address
		    List<Address> addresses = null;
		    try {
		        addresses = geocoder.getFromLocation(loc.getLatitude(),loc.getLongitude(), 1);
		        
		    } catch (IOException e1) {
		    	Log.e("LocationSampleActivity","IO Exception in getFromLocation()");
		    	e1.printStackTrace();
		    	return ("IO Exception trying to get address");
		    } catch (IllegalArgumentException e2) {
			    // Error message to post in the log
			    String errorString = "Illegal arguments " + Double.toString(loc.getLatitude()) + " , " +
			            Double.toString(loc.getLongitude()) + " passed to address service";
			    Log.e("LocationSampleActivity", errorString);
			    e2.printStackTrace();
			    return errorString;
		    }
		    // If the reverse geocode returned an address
		    if (addresses != null && addresses.size() > 0) {
		        // Get the first address
		        Address address = addresses.get(0);
		        /*
		         * Format the first line of address (if available),
		         * city, and country name.
		         */
		        String addressText = String.format(
		                "%s, %s, %s",
		                // If there's a street address, add it
		                address.getMaxAddressLineIndex() > 0 ?
		                        address.getAddressLine(0) : "",
		                // Locality is usually a city
		                address.getLocality(),
		                // The country of the address
		                address.getCountryName());
		        // Return the text
		        return addressText;
		    } else {
		        return "No address found";
		    }
		}
		@Override
        protected void onPostExecute(String address) {
            // Set activity indicator visibility to "gone"
            mActivityIndicator.setVisibility(View.GONE);
            // Display the results of the lookup.
            destination_addr.setText(address);
        }
	}
	
	private class GetAddressTask extends AsyncTask<Location, Void, String> {
		Context mContext;
		public GetAddressTask(Context context) {
			super();
			mContext = context;
		}

		/**
		 * Get a Geocoder instance, get the latitude and longitude
		 * look up the address, and return it
		 *
		 * @params params One or more Location objects
		 * @return A string containing the address of the current
		 * location, or an empty string if no address can be found,
		 * or an error message
		 */
		@Override
		protected String doInBackground(Location... params) {
		    Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
		    // Get the current location from the input parameter list
		    Location loc = params[0];
		    // Create a list to contain the result address
		    List<Address> addresses = null;
		    try {
		        addresses = geocoder.getFromLocation(loc.getLatitude(),loc.getLongitude(), 1);
		        
		    } catch (IOException e1) {
		    	Log.e("LocationSampleActivity","IO Exception in getFromLocation()");
		    	e1.printStackTrace();
		    	return ("IO Exception trying to get address");
		    } catch (IllegalArgumentException e2) {
			    // Error message to post in the log
			    String errorString = "Illegal arguments " + Double.toString(loc.getLatitude()) + " , " +
			            Double.toString(loc.getLongitude()) + " passed to address service";
			    Log.e("LocationSampleActivity", errorString);
			    e2.printStackTrace();
			    return errorString;
		    }
		    // If the reverse geocode returned an address
		    if (addresses != null && addresses.size() > 0) {
		        // Get the first address
		        Address address = addresses.get(0);
		        /*
		         * Format the first line of address (if available),
		         * city, and country name.
		         */
		        String addressText = String.format(
		                "%s, %s, %s",
		                // If there's a street address, add it
		                address.getMaxAddressLineIndex() > 0 ?
		                        address.getAddressLine(0) : "",
		                // Locality is usually a city
		                address.getLocality(),
		                // The country of the address
		                address.getCountryName());
		        // Return the text
		        return addressText;
		    } else {
		        return "No address found";
		    }
		}
		@Override
        protected void onPostExecute(String address) {
            // Set activity indicator visibility to "gone"
            mActivityIndicator.setVisibility(View.GONE);
            // Display the results of the lookup.
            current_addr.setText(address);
        }
	}
	
	/*
	 * setup Map object if it's null
	 */
	@SuppressLint("NewApi")
	private void setMap(){
		if(mMap == null){
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap(); 
		}
	}
	
	/*
	 * Determine camera bounds. 
	 * camera bound consist of southeast and northwest point
	 */
	private void determineBounds(LatLng a, LatLng b){
		if(a.longitude < b.longitude && a.latitude < b.latitude){//a: lower-left. b: upper-right
			cameraBounds = new LatLngBounds(a,b);
		}else if(a.longitude > b.longitude && a.latitude > b.latitude){//a: upper-right. b: lower-left
			cameraBounds = new LatLngBounds(b,a);
		}else if(a.longitude < b.longitude && a.latitude > b.latitude){//a: upper-left. b: lower-right
			cameraBounds = new LatLngBounds(new LatLng(b.latitude,a.longitude), new LatLng(a.latitude,b.longitude));
		}else if(a.longitude > b.longitude && a.latitude < b.latitude){//a: lower-right. b: upper-left
			cameraBounds = new LatLngBounds(new LatLng(a.latitude,b.longitude), new LatLng(b.latitude,a.longitude));
		}else{
		}
	}
	
	/**
	 * Determine whether the map is empty.
	 * 
	 * If the map is not empty, it's necessary to clear the map before drawing new route and place new markers on it
	 * @return true if empty; false if not empty
	 */
	private Boolean isMapEmpty(){
		if(destinationMarker != null){
			return false;
		}else{
			return true;
		}
	}
	
	public int pxToDp(int px) {
	    DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
	    int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
	    return dp;
	}
}
