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
import android.util.JsonReader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;;

public class MainActivity extends Activity implements 
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener
{
	private LocationClient mLocationClient;
	private ProgressBar mActivityIndicator;
	private Location mCurrentLocation;
	private LatLng currentLatLng;
	private LatLng destinationLatLng;
	private Point destinationPoint;
	private EditText current_addr;
	private EditText destination_addr;
	private Button button_current;
	private Button button_destination;
	private String string_destination;
	private GoogleMap mMap;
	private MapFragment mMapFragment;
	private TextView distance;
	private LatLngBounds cameraBounds;
	private Marker currentMarker;
	private Marker destinationMarker;
	private Projection proj;
	static final LatLng HAMBURG = new LatLng(53.558, 9.927);
	private Context gContext;
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mActivityIndicator = (ProgressBar) findViewById(R.id.address_progress);
	    current_addr = (EditText) findViewById(R.id.current);
	    destination_addr = (EditText) findViewById(R.id.destination);
		mLocationClient = new LocationClient(this, this, this);
		button_current = (Button) findViewById(R.id.button_current);
		button_destination = (Button) findViewById(R.id.button_destination);
	    distance = (TextView) findViewById(R.id.distance);
	    cameraBounds = null;
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap(); 
		destinationPoint = null;
		destinationMarker = null;
		gContext = this;
		
		destination_addr.setOnKeyListener(new OnKeyListener()
		{
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction()!=KeyEvent.ACTION_DOWN)
		            return false;
		        if(keyCode == KeyEvent.KEYCODE_ENTER ){
		        	string_destination = destination_addr.getText().toString();
		            Log.d("Location","Destination Addr: "+string_destination);
		            new GetLocTask().execute(string_destination);
		            return true;
		        }
		        return false;
			}
		});
		mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
			
			@Override
			public void onMapClick(LatLng clickLatLng) {
				//mMap.addMarker(new MarkerOptions().position(clickLatLng).title("Destination"));
				if(!isMapEmpty()){
					Log.d("Location","Remove Destination Marker");
					mMap.clear();
				}
				destinationLatLng = clickLatLng;
				Location destinationLocation  = mCurrentLocation;
				destinationLocation.setLatitude(destinationLatLng.latitude);
				destinationLocation.setLongitude(destinationLatLng.longitude);
				new GetRouteTask().execute();
				new GetDestinationAddressTask(gContext).execute(destinationLocation);
			}
		});
		
//		mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
//			
//			@Override
//			public void onCameraChange(CameraPosition position) {
//				
//				if(destinationLatLng != null){
//					proj = mMap.getProjection();
//					if(destinationPoint == null){
//						destinationPoint = proj.toScreenLocation(destinationMarker.getPosition());
//						Log.d("Location","Point:" + destinationPoint.toString());
//					}
////						proj = mMap.getProjection();
////						//destinationPoint = a.toScreenLocation(destinationLatLng);
//						LatLng newDestinationLatLng = proj.fromScreenLocation(destinationPoint);
////						//Log.d("Location","Point:" + destinationPoint.toString());
////						Log.d("Location","New LatLng: " + newDestinationLatLng.toString());
////						//destination.remove();
//						mMap.addMarker(new MarkerOptions().position(newDestinationLatLng).title("Destination"));
//				}
//			}
//		});
		
		
//		GoogleMapOptions options = new GoogleMapOptions();
//		options.mapType(GoogleMap.MAP_TYPE_NORMAL)
//			.camera(new CameraPosition(new LatLng(37.7750,122.4183),5,0,(float) 0))
//	    	.compassEnabled(false)
//	    	.rotateGesturesEnabled(false)
//	    	.tiltGesturesEnabled(false);
//		
//		mMapFragment = MapFragment.newInstance(options);
//		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
//		fragmentTransaction.add(R.id.map_container,mMapFragment,"map");
//		fragmentTransaction.commit();
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
		// TODO Auto-generated method stub
		mCurrentLocation = mLocationClient.getLastLocation();
		
		currentLatLng = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());
//		GroundOverlayOptions newarkMap = new GroundOverlayOptions()
//        	.image(BitmapDescriptorFactory.fromResource(R.drawable.a))
//        	.position(currentLatLng, 8600f, 6500f);
//		mMap.addGroundOverlay(newarkMap);
		//Marker current = mMap.addMarker(new MarkerOptions().position(current_latlng).title("Current"));
		// Move the camera instantly to hamburg with a zoom of 15.
	    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
		// Zoom in, animating the camera.
	    mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
	    
		//LatLngBounds cameraBound = new LatLngBounds(new LatLng(-44, 113), new LatLng(-10, 154));
		//LatLngBounds cameraBound = new LatLngBounds(new LatLng(33, -96), new LatLng(39, -84));
		//mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cameraBound, 0));
	    
		Log.d("Location","Current Location:"+mCurrentLocation.toString());
		mActivityIndicator.setVisibility(View.VISIBLE);
		new GetAddressTask(this).execute(mCurrentLocation);
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
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
						+"&sensor=true");
				Log.d("Location",url.toString());
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
                route_string = sb.toString();
			}catch (Exception ex){
            	ex.printStackTrace();
            }
			return route_string;
		}
		@Override
        protected void onPostExecute(String route) {
			button_current.setVisibility(View.VISIBLE);
			button_destination.setVisibility(View.VISIBLE);
			try {
				JSONObject mainObject = new JSONObject(route);
				Log.d("Location",mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getString("text").toString());
				String dist = mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance").getString("text").toString();
				distance.setText("Distance: "+dist);
 
				currentMarker = mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current").icon(BitmapDescriptorFactory.fromResource(R.drawable.letter_a)));
				destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));
				
				determineBounds(currentLatLng,destinationLatLng);
				mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(cameraBounds,200));
				
				Log.d("Location",mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").length()+"");

				for(int i = 0; i < mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").length(); i++){
					//Log.d("Location",mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(i).getJSONObject("start_location").getDouble("lat")+"");
					//Log.d("Location",mainObject.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps").getJSONObject(i).getJSONObject("start_location").getDouble("lng")+"");
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
				//URL url = new URL("http://maps.googleapis.com/maps/api/geocode/json?address=591+10th+St+NW+Atlanta+GA+30318&sensor=true");
				//URL url = new URL("http://www.penghou.net/huayang-codeigniter/index.php/order/allproduct/orderid/92/format/json");
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
		        /*
		         * Return 1 address.
		         */
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
		        /*
		         * Return 1 address.
		         */
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
	@SuppressLint("NewApi")
	private void setMap(){
		if(mMap == null){
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap(); 
		}
	}
	
	private void determineBounds(LatLng a, LatLng b){
		if(a.longitude < b.longitude && a.latitude < b.latitude){//a: lower-left. b: upper-right
			Log.d("Location","Bounds Case 1");
			cameraBounds = new LatLngBounds(a,b);
		}else if(a.longitude > b.longitude && a.latitude > b.latitude){
			Log.d("Location","Bounds Case 2");
			cameraBounds = new LatLngBounds(b,a);
		}else if(a.longitude < b.longitude && a.latitude > b.latitude){//a: upper-left. b: lower-right
			Log.d("Location","Bounds Case 3");
			cameraBounds = new LatLngBounds(new LatLng(b.latitude,a.longitude), new LatLng(a.latitude,b.longitude));
		}else if(a.longitude > b.longitude && a.latitude < b.latitude){//a: lower-right. b: upper-left
			Log.d("Location","Bounds Case 4");
			cameraBounds = new LatLngBounds(new LatLng(a.latitude,b.longitude), new LatLng(b.latitude,a.longitude));
		}else{
			Log.d("Location","Bounds Case 5");
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
}
