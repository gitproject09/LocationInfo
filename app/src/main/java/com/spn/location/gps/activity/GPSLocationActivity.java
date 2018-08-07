package com.spn.location.gps.activity;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.spn.location.gps.helper.LocationHelper;
import com.spn.location.gps.R;
import com.spn.location.gps.interfaces.Constants;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GPSLocationActivity extends AppCompatActivity implements Constants, LocationHelper.LocationCallback {

    private LocationHelper mLocationHelper;
    private Location mLastLocation = null;
    // private TextView tvLatitude, tvLongitude;
    TextView tv_latitude, tv_longitude, tv_address, tv_division, tv_thana, tv_country, tv_district;
    Double latitude, longitude;
    Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gpslocation);

        //  tvLatitude = findViewById(R.id.latitude_textview);
        //tvLongitude = findViewById(R.id.longitude_textview);

        tv_address = findViewById(R.id.tv_address);
        tv_latitude = findViewById(R.id.tv_latitude);
        tv_longitude = findViewById(R.id.tv_longitude);
        tv_division = findViewById(R.id.tv_division);
        tv_thana = findViewById(R.id.tv_thana);
        tv_district = findViewById(R.id.tv_district);
        tv_country = findViewById(R.id.tv_country);
        geocoder = new Geocoder(this, Locale.getDefault());

        mLocationHelper = (LocationHelper) getFragmentManager().findFragmentByTag(LOCATION_FRAGMENT);
        if (mLocationHelper == null) {
            mLocationHelper = LocationHelper.newInstance();
            getFragmentManager().beginTransaction().add(mLocationHelper, LOCATION_FRAGMENT).commit();
        }

    }

    @Override
    public void onLocationSettingsFailed() {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Log.d(GPSLocationActivity.class.getSimpleName(), "GPSLocation : " + mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());
        //String.valueOf(mLastLocation.getLatitude())
        // tvLatitude.setText(" "+mLastLocation.getLatitude());
        // tvLongitude.setText(" "+mLastLocation.getLongitude());

        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();

        //  latitude = 21.431991;
        // longitude = 92.16067;

       /* latitude = 24.0372165;
        longitude = 90.9815163;*/

        tv_latitude.setText(latitude + "");
        tv_longitude.setText(longitude + "");

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Log.e(GPSLocationActivity.class.getSimpleName(), "Thana : " + addresses.get(0).getLocality());
                Log.e(GPSLocationActivity.class.getSimpleName(), "District : " + addresses.get(0).getSubAdminArea());
                Log.e(GPSLocationActivity.class.getSimpleName(), "Division : " + addresses.get(0).getAdminArea());
                Log.e(GPSLocationActivity.class.getSimpleName(), "Address : " + addresses.get(0).getAddressLine(0));
                Log.e(GPSLocationActivity.class.getSimpleName(), "Country : " + addresses.get(0).getCountryName());

                /*String cityName = addresses.get(0).getAddressLine(0);
                String stateName = addresses.get(0).getAddressLine(1);
                String countryName = addresses.get(0).getAddressLine(2);*/

                String divisionName = addresses.get(0).getAdminArea(); // Division
                String districtName = addresses.get(0).getSubAdminArea(); // District
                String thanaName = addresses.get(0).getLocality(); // Thana
                String addressName = addresses.get(0).getAddressLine(0); // Address
                String countryName = addresses.get(0).getCountryName(); // Country

                tv_division.setText(divisionName);
                tv_district.setText(districtName);
                tv_thana.setText(thanaName);
                tv_address.setText(addressName);
                tv_country.setText(countryName);

            } else {
                Log.e(GPSLocationActivity.class.getSimpleName(), "LocationOfYou : Not found Data");
            }

        } catch (IOException e1) {
            e1.printStackTrace();
            Log.e(GPSLocationActivity.class.getSimpleName(), "Error occured : " + e1.getMessage());
        }

    }

    private void fetchLocation() {
        if (mLocationHelper != null) {
            mLocationHelper.checkLocationPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mLastLocation == null) {
            fetchLocation();
        }
    }
}
