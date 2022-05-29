package ch.ruefenacht.sandro.relaisfinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import ch.ruefenacht.sandro.relaisfinder.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private static Map<Integer, RadioRepeaterModel> radioRepeaterModels = new HashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        if(!checkConnection()) {
            Log.i("Network-Connection", "No Connection");

            Intent panelIntent = new Intent( Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
            startActivity(panelIntent);
        }

        new Thread() {
            public void run() {
                try {
                    URL url = new URL(BuildConfig.RELAIS_FINDER_API_URL + "relais");
                    URLConnection urlConnection = url.openConnection();
                    InputStream in = urlConnection.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                    StringBuilder resultContent = new StringBuilder();
                    String inputLine;
                    while ((inputLine = reader.readLine()) != null) {
                        resultContent.append(inputLine);
                    }
                    in.close();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            deliverResult(resultContent.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void deliverResult(String result) {
        try {
            JSONArray jsonArray = new JSONArray(result);

            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if(!jsonObject.has("ID"))
                    throw new Exception();

                Marker marker = this.mMap.addMarker(new MarkerOptions().position(new LatLng(jsonObject.getDouble("coordinateX"), jsonObject.getDouble("coordinateY"))).title(jsonObject.getString("title")));
                if(marker != null) {
                    RadioRepeaterModel radioRepeaterModel = new RadioRepeaterModel(jsonObject);
                    radioRepeaterModels.put(radioRepeaterModel.ID, radioRepeaterModel);
                    marker.setTag(radioRepeaterModel.ID);
                }
            }
            this.mMap.setOnMarkerClickListener(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng centerSwitzerland = new LatLng(46.7985, 8.2318);
        this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerSwitzerland, 6.5f));
    }

    private boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getActiveNetworkInfo() != null) {
            if(connectivityManager.getActiveNetworkInfo().isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        Integer clickCount = (Integer) marker.getTag();

        // Check if a click count was set, then display the click count.
        if (clickCount != null) {
            if(!radioRepeaterModels.containsKey(marker.getTag())) {
                Log.e("Relais Finder", "Radio Repeater '" + marker.getTag() + "' not found!");
            }
            String markerInfoTable = this.getInfoTable(radioRepeaterModels.get(marker.getTag()));
            ((TextView)findViewById(R.id.markerOutput)).setText(markerInfoTable);
        }

        return false;
    }

    private String getInfoTable(RadioRepeaterModel radioRepeater) {
        return getResources().getString(R.string.info_table_title) + ":                     " + radioRepeater.title + "\n" +
                getResources().getString(R.string.info_table_callsign) + ":               " + radioRepeater.callsign + "\n" +
                getResources().getString(R.string.info_table_frequency_in) + ":      " + radioRepeater.frequencyIn + " Mhz \n" +
                getResources().getString(R.string.info_table_frequency_out) + ":   " + radioRepeater.frequencyOut + " Mhz";
    }
}