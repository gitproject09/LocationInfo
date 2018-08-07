package com.spn.location.gps.helper;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.spn.location.gps.util.PermissionUtil;
import com.spn.location.gps.activity.GPSLocationActivity;
import com.spn.location.gps.interfaces.Constants;

public class LocationHelper extends Fragment implements Constants, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static Location sLastLocation;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mCoarseLocationRequest;

    private LocationCallback mCallback;
    private Activity mActivity;
    private static boolean sLoationPermissionDenied;


    public static LocationHelper newInstance() {
        return new LocationHelper();
    }

    public LocationHelper() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof LocationCallback) {
            mCallback = (LocationCallback) activity;
        } else {
            throw new IllegalArgumentException("activity must extend BaseActivity and implement LocationHelper.LocationCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
        mActivity = null;
    }

    public Location getLocation() {
        return sLastLocation;
    }

    public void checkLocationPermissions() {
        if (PermissionUtil.hasSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            initAppAfterCheckingLocation();
        } else {
            // UNCOMMENT TO SUPPORT ANDROID M RUNTIME PERMISSIONS
//      Intent intent = mActivity.getPackageManager().buildRequestPermissionsIntent(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
//      startActivityForResult(intent, REQUEST_LOCATION);
            if (!sLoationPermissionDenied) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
            }
        }
    }

    private void initAppAfterCheckingLocation() {
        if (mGoogleApiClient == null) {
            buildGoogleApiClient();
        } else if (sLastLocation == null) {
            if (mGoogleApiClient.isConnected()) {
                // check for a location.
                checkIfLocationServicesEnabled();
            } //else if (mGoogleApiClient.isConnecting()) {
            //do nothing
            //}
        } else {
            Log.d(GPSLocationActivity.class.getSimpleName(), "GPS Loc : " + sLastLocation.getLatitude() + "," + sLastLocation.getLongitude());
            if (mCallback != null) {
                mCallback.onLocationChanged(sLastLocation);
            }
        }
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    private void checkLocationAndInit() {
        if (sLastLocation == null) {
            sLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if (sLastLocation == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    sLastLocation = location;
                    if (mCallback != null) {
                        mCallback.onLocationChanged(sLastLocation);
                    }
                    initAppAfterCheckingLocation();
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                }
            });

        } else {
            initAppAfterCheckingLocation();
        }
    }

    private LocationRequest createLocationRequest() {
        if (mCoarseLocationRequest == null) {
            mCoarseLocationRequest = new LocationRequest();
            mCoarseLocationRequest.setInterval(5000);
            mCoarseLocationRequest.setFastestInterval(1000);
            mCoarseLocationRequest.setNumUpdates(1);
            mCoarseLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
        return mCoarseLocationRequest;
    }

    private void checkIfLocationServicesEnabled() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(createLocationRequest());

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        checkLocationAndInit();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        checkLocationAndInit();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        if (mCallback != null) {
                            mCallback.onLocationSettingsFailed();
                        }
                        break;
                    default:
                        if (mCallback != null) {
                            mCallback.onLocationSettingsFailed();
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        checkIfLocationServicesEnabled();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void setLoationPermissionDenied(boolean mLoationPermissionDenied) {
        this.sLoationPermissionDenied = mLoationPermissionDenied;
    }

    public static boolean isLoationPermissionDenied() {
        return sLoationPermissionDenied;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissions();
            } else {
                Log.i("BaseActivity", "LOCATION permission was NOT granted.");
                setLoationPermissionDenied(true);
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public interface LocationCallback {
        void onLocationSettingsFailed();
        void onLocationChanged(Location location);
    }

}
