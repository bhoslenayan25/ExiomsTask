package task.nayan.exiomstask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getName();

    TextView textCurrentLocation;
    AutoCompleteTextView autoDestination;
    Button btnStart;

    private String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private String TYPE_AUTOCOMPLETE = "/autocomplete";
    private String OUT_JSON = "/json";
    GPSTracker locationService;

    LocationManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textCurrentLocation = (TextView) findViewById(R.id.textCurrentLocation);
        autoDestination = (AutoCompleteTextView) findViewById(R.id.autoDestination);
        btnStart = (Button) findViewById(R.id.btnStart);

        autoDestination.setAdapter(new DestinationAdapter(this, R.layout.list_item));

        locationService = new GPSTracker(MainActivity.this);

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Intent servIntent = new Intent("task.nayan.exiomstask.GPSTracker");
        servIntent.putExtra("lat", 0.0);
        servIntent.putExtra("lng", 0.0);
        startService(servIntent);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showSettingsAlert();
        } else {
            Location location = locationService.getLocation();
            if(location != null) {
                String adderss = getAddressFromLocation(location);
                textCurrentLocation.setText(adderss);
            }
        }


        autoDestination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = (String) parent.getItemAtPosition(position);
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });


        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String desti = autoDestination.getText().toString().trim();
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if(desti.length() > 0){
                        LatLng latLng = getLatLongFromAddress(desti);
                        Intent servIntent = new Intent("task.nayan.exiomstask.GPSTracker");
                        servIntent.putExtra("lat", latLng.latitude);
                        servIntent.putExtra("lng", latLng.longitude);
                        startService(servIntent);
                    }else{
                        Toast.makeText(MainActivity.this,"Select Destination",Toast.LENGTH_LONG).show();
                    }
                } else {
                    showSettingsAlert();
                }
            }
        });

    }

    private String getAddressFromLocation(Location location) {

        String address = "";

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            Address add = addresses.get(0);
            for (int i = 0; i < add.getMaxAddressLineIndex(); i++) { //Since it return only four value we declare this as static.
                address = address + add.getAddressLine(i).replace(",", "") + ", ";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void locationChanged(Location location) {
        String adderss = getAddressFromLocation(location);
        textCurrentLocation.setText(adderss);
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    private LatLng getLatLongFromAddress(String address) {
        double lat = 0.0, lng = 0.0;
        LatLng latLng = null;
        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocationName(address, 1);
            if (addresses.size() > 0) {
                lat = addresses.get(0).getLatitude();
                lng = addresses.get(0).getLongitude();

                latLng = new LatLng(lat, lng);

                Log.d("Latitude", "" + lat);
                Log.d("Longitude", "" + lng);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return latLng;
    }


    class DestinationAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public DestinationAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }


    public ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + getString(R.string.api_key));
            sb.append("&components=country:in");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());

            System.out.println("URL: " + url);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {

            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

}
