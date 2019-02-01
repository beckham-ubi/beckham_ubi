package com.example.skoka.mapapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMyLocationButtonClickListener, LocationSource {


    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private Marker mMarker = null;

    private TextView textView;
    private DialogFragment dialogFragment;
    private FragmentManager flagmentManager;

    private OnLocationChangedListener onLocationChangedListener = null;

    private int priority[] = {LocationRequest.PRIORITY_HIGH_ACCURACY, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
            LocationRequest.PRIORITY_LOW_POWER, LocationRequest.PRIORITY_NO_POWER};


    int locationPriority = priority[0];

    int flag=0;
    double lastlat=0;
    double lastlng=0;
    String lasttime;

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */



    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 0x01;
    private static String[] mPermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("debug", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        // LocationRequest を生成して精度、インターバルを設定
        locationRequest = LocationRequest.create();



        // 測位の精度、消費電力の優先度

        if (locationPriority == priority[0]) {
            // 位置情報の精度を優先する場合
            locationRequest.setPriority(locationPriority);
            locationRequest.setInterval(5000);
            locationRequest.setFastestInterval(16);
            locationRequest.setSmallestDisplacement(10);
        } else {
            // 消費電力を考慮する場合
            locationRequest.setPriority(locationPriority);
            locationRequest.setInterval(60000);
            locationRequest.setFastestInterval(16);
            locationRequest.setSmallestDisplacement(100);
        }


        //マップへの接続準備
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d("debug", "onCreated");
    }


    //ファイルへの書き込み、読み取り権限
    private static void verifyStoragePermissions(Activity activity) {
        int readPermission = ContextCompat.checkSelfPermission(activity, mPermissions[0]);
        int writePermission = ContextCompat.checkSelfPermission(activity, mPermissions[1]);

        if (writePermission != PackageManager.PERMISSION_GRANTED ||
                readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    mPermissions,
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }
    }






    // onResumeフェーズに入ったら接続
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    // onPauseで切断
    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }




    //初期設定
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d("debug", "onMapReady");

        // 権限確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("debug", "permission granted");

            mMap = googleMap;
            mMap.setLocationSource(this);
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
        } else {
            Log.d("debug", "permission error");
        }
        mMap = googleMap;


        //東京駅にズームイン
        CameraUpdate cUpdate = CameraUpdateFactory.newLatLngZoom( new LatLng(35.68, 139.76),12);
        mMap.moveCamera(cUpdate);


        //マップがタップされた時の処理
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng tapLocation) {
                // タップされた位置の緯度経度
                LatLng tLocation = new LatLng(tapLocation.latitude, tapLocation.longitude);
                String str = String.format(Locale.US, "%f, %f", tapLocation.latitude, tapLocation.longitude);
                mMarker = mMap.addMarker(new MarkerOptions().position(tLocation).title(str));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tLocation, 14));
            }

        });


        // マーカーがタップされた時の処理
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(getApplicationContext(), "マーカータップ", Toast.LENGTH_SHORT).show();

                // アラートダイアログを表示させる
                flagmentManager = getSupportFragmentManager();
                dialogFragment = new MainActivity.AlertDialogFragment();
                dialogFragment.show(flagmentManager, "test alert dialog");

                return false;
            }

        });


        //マップが長押しされた時の処理
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng longpushLocation) {
                mMarker.remove();
                Toast.makeText(getApplicationContext(), "マーカー削除!", Toast.LENGTH_SHORT).show();
            }
        });

        Log.d("debug", "onMapReadied");
    }



    //移動して位置が変わったことを認識した時の処理
    @Override
    public void onLocationChanged(Location location) {
        Log.d("debug","onLocationChanged");
        if (onLocationChangedListener != null) {
            onLocationChangedListener.onLocationChanged(location);

            //緯度経度を取得、表示
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            LatLng newLocation = new LatLng(lat, lng);
            LatLng pastLocation = new LatLng(lastlat, lastlng);
            Log.d("debug","location="+lat+","+lng);
            Toast.makeText(this, "location="+lat+","+lng, Toast.LENGTH_SHORT).show();

            // 現在日時の取得、フォーマット
            Date now = new Date(System.currentTimeMillis());
            DateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH時mm分ss秒");
            String nowText = formatter.format(now);


            //マーカーのアイコンを丸に変える



            //マーカーの個別設定
            MarkerOptions options = new MarkerOptions();
            options.position(newLocation);
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
            options.icon(icon);

            //マーカー設置、カメラ移動
           // mMap.addMarker(options.position(newLocation).title(nowText));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newLocation));

            //直前の位置から直線で結ぶ
            if(flag>0) {           //初期状態では直線で結べないため例外扱い
                PolylineOptions straight = new PolylineOptions()
                        .add(pastLocation, newLocation)
                        .geodesic(false)        // 直線
                        .color(Color.GREEN)
                        .width(20);
                mMap.addPolyline(straight);
            }

            //現在地を仮保存
            lastlat = lat;
            lastlng = lng;
            lasttime = nowText;
            flag=1;        //直線で結べるかを判別するフラグ

        }
        Log.d("debug", "LocationChanged");
    }




    //マップ接続時の処理
    @Override
    public void onConnected(Bundle bundle) {
        Log.d("debug", "onConnected");
        // 権限確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("debug", "permission granted!!　");

            // FusedLocationApi
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, locationRequest, this);
        }
        else{
            Log.d("debug", "permission error");
        }
        Log.d("debug", "Connected");
    }



    // 何らかの理由で接続が無くなった時の処理
    @Override
    public void onConnectionSuspended(int i) {
        Log.d("debug", "onConnectionSuspended");
    }



    // 接続が失敗した時の処理
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("debug", "onConnectionFailed");
    }



    //ボタンをタップされた時の処理
    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "onMyLocationButtonClick", Toast.LENGTH_SHORT).show();

        return false;
    }





    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        this.onLocationChangedListener = onLocationChangedListener;
    }




    @Override
    public void deactivate() {
        this.onLocationChangedListener = null;
    }



}
