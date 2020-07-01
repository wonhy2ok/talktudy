package jeong_won_hyeok.inhatc.talktudy;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef;

    FloatingActionButton add, mode;
    private GoogleMap mMap;
    private BackPressHandler backPressHandler;

    private Marker currentMarker = null;
    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};  // 외부 저장소
    Location mCurrentLocation;
    LatLng currentPosition;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;
    private View mLayout;

    Boolean recur;

    private ImageView search;
    private Marker addMarker;
    String addMarkerId;

    SimpleDateFormat dateFormat = new SimpleDateFormat ( "yyyy-MM-dd");
    Date today = new Date();

    CircleOptions circleOptions;
    Circle circle;

    static ArrayList<String> placeList = new ArrayList<String>();
    static ArrayList<LatLng> alarmList = new ArrayList<LatLng>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        backPressHandler = new BackPressHandler(this);
        myRef = database.getReference("list");
        recur=false;

        add=(FloatingActionButton)findViewById(R.id.add);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, AlertList.class);
                startActivity(intent);
            }
        });
        /*
        // 집중모드는 역사속으로
        mode=(FloatingActionButton)findViewById(R.id.mode);
        mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mode1 = new Intent(MapsActivity.this, Mode1.class);
                startActivity(mode1);
            }
        });
        */

        // 추가
        // https://webnautes.tistory.com/1249
        mLayout = findViewById(R.id.layout_main);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        search = (ImageView)findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, DaumWebViewActivity.class);
                startActivityForResult(intent, 100);
            }
        });

        // 저장한 알람 리스트 가져오기
        placeList = getStringArrayList("place");
        setStringArrayListToLatLngArrayList(getStringArrayList("alarmm"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setDefaultLocation();
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {
            startLocationUpdates(); // 3. 위치 업데이트 시작
        }else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions( MapsActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();
            } else {
                ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot c : dataSnapshot.getChildren()) {
                    Double lat = Double.parseDouble(c.child("lat").getValue().toString());
                    Double lng = Double.parseDouble(c.child("long").getValue().toString());
                    Date curtDate = null;
                    try{
                        curtDate = dateFormat.parse(c.child("date").getValue().toString());
                    }catch(ParseException e){
                        e.getStackTrace();
                    }
                    if(today.compareTo(curtDate) <= 0) {
                        int[] icons = {R.drawable.icon0, R.drawable.icon1, R.drawable.icon2, R.drawable.icon3, R.drawable.icon4, R.drawable.icon5, R.drawable.icon6, R.drawable.icon7};
                        String title = c.child("title").getValue().toString();
                        int number = Integer.parseInt(c.child("icon").getValue().toString());
                        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(icons[number]);
                        Bitmap b = bitmapdraw.getBitmap();
                        Bitmap smallMarker;
                        smallMarker = Bitmap.createScaledBitmap(b, 72, 96, false);
                        LatLng m = new LatLng(lat, lng);
                        mMap.addMarker(new MarkerOptions().position(m).title(title).icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).snippet("상세 보기"));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mMap.setOnInfoWindowClickListener(infoWindowClickListener);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (addMarker == null) {
                    BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.green);
                    Bitmap b=bitmapdraw.getBitmap();
                    Bitmap smallMarker;
                    smallMarker = Bitmap.createScaledBitmap(b, 96, 96, false);
                    MarkerOptions addedMarker = new MarkerOptions()
                            .position(latLng)
                            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
                            .title("등록")
                            .snippet("장소를 등록하실래요?");
                    addMarker = mMap.addMarker(addedMarker);
                    addMarkerId = addMarker.getId();

                } else {
                    addMarker.setPosition(latLng);
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.getId().equals(addMarkerId)) { // 등록 마커일 경우
                    location.setLatitude(marker.getPosition().latitude);
                    location.setLongitude(marker.getPosition().longitude);
                    AddDialog(location);
                }else {
                    onInfoWindowClick(marker);
                }
                return true;
            }
        });
    }

    public void onInfoWindowClick(final Marker marker) {
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String tit = child.child("title").getValue().toString();
                    if (marker.getTitle().equals(tit)) {
                        String con = child.child("content").getValue().toString();
                        String p = child.child("place").getValue().toString();
                        String nm = child.child("name").getValue().toString();
                        String dt = child.child("date").getValue().toString();
                        String lin = child.child("link").getValue().toString();
                        String dt2 = child.child("date2").getValue().toString();
                        LatLng latlng = marker.getPosition();
                        InfoDialog(tit, con, p, nm, lin, dt, dt2, latlng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    GoogleMap.OnInfoWindowClickListener infoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(final Marker marker) {
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        String tit = child.child("title").getValue().toString();
                        if (marker.getTitle().equals(tit)) {
                            String con = child.child("content").getValue().toString();
                            String p = child.child("place").getValue().toString();
                            String nm = child.child("name").getValue().toString();
                            String dt = child.child("date").getValue().toString();
                            String lin = child.child("link").getValue().toString();
                            String dt2 = child.child("date2").getValue().toString();
                            LatLng latlng = marker.getPosition();
                            InfoDialog(tit, con, p, nm, lin, dt, dt2, latlng);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    };

    @Override public void onBackPressed() {
        backPressHandler.onBackPressed();
    }

    public void AddDialog(final Location addLocation) {
        View dlgView = View.inflate(this,R.layout.dialog_add,null);
        final Dialog dlg = new Dialog(this);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dlg.setContentView(dlgView);
        TextView ok;
        final TextView title, content, place, link, name, date2;
        final Button date;

        final ListView listView;
        listView = (ListView)dlgView.findViewById(R.id.listView);
        ListViewAdapter adapter = new ListViewAdapter();
        listView.setAdapter(adapter);

        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon0), "밥");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon1), "택시");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon2), "운동");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon3), "공부");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon4), "영화");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon5), "대화");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon6), "게임");
        adapter.addItem(ContextCompat.getDrawable(this, R.drawable.icon7), "기타");


        ok = (TextView)dlgView.findViewById(R.id.ok_bt);
        title = (TextView)dlgView.findViewById(R.id.title);
        content = (TextView)dlgView.findViewById(R.id.content);
        place = (TextView)dlgView.findViewById(R.id.place);  // 위치 수정받아야함
        place.setText(getCurrentAddress(new LatLng(addLocation.getLatitude(), addLocation.getLongitude())));
        name = (TextView)dlgView.findViewById(R.id.name);
        link = (TextView)dlgView.findViewById(R.id.link);
        date = (Button)dlgView.findViewById(R.id.date);
        date2 = (TextView)dlgView.findViewById(R.id.date2);

        final DatePickerDialog.OnDateSetListener mDateSetListener =
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int yy, int mm, int dd) {
                        String mm2 = (mm+1)/10 ==0 ? "0"+(mm+1) : ""+(mm+1);
                        String dd2 = dd/10 ==0 ? "0"+dd : ""+dd;
                        date.setText(String.format("%d-%s-%s", yy,mm2,dd2));
                    }
                };

        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(MapsActivity.this, mDateSetListener, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE)).show();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (title.getText().toString().equals("") || content.getText().toString().equals("") || place.getText().toString().equals("") || name.getText().toString().equals("") || date.getText().toString().equals("날짜를 선택하세요") || date2.getText().toString().equals("") || link.getText().toString().equals("")) {
                    Toast.makeText(MapsActivity.this, "모든 항목을 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else {
                    Date curtDate = null;
                    try{
                        curtDate = dateFormat.parse(date.getText().toString());
                    }catch(ParseException e){
                        e.getStackTrace();
                    }
                    if(today.compareTo(curtDate)>0) {
                        Toast.makeText(MapsActivity.this, "유효하지 않은 날짜 입니다.\n다시 선택해주세요.", Toast.LENGTH_SHORT).show();
                    }else {
                        String str = date.getText().toString()+title.getText().toString();
                        myRef.child(str).child("title").setValue(title.getText().toString());
                        myRef.child(str).child("place").setValue(place.getText().toString());
                        myRef.child(str).child("content").setValue(content.getText().toString());
                        myRef.child(str).child("link").setValue(link.getText().toString());
                        myRef.child(str).child("name").setValue(name.getText().toString());
                        myRef.child(str).child("date").setValue(date.getText().toString());
                        myRef.child(str).child("date2").setValue(date2.getText().toString());
                        myRef.child(str).child("lat").setValue(addLocation.getLatitude());  // 나중에 수정(일단 현재 위치에 마커 생성하도록 함)
                        myRef.child(str).child("long").setValue(addLocation.getLongitude());  // 나중에 수정22
                        myRef.child(str).child("icon").setValue(listView.getCheckedItemPosition());

                        Toast.makeText(MapsActivity.this, "등록되었습니다.", Toast.LENGTH_SHORT).show();
                        dlg.dismiss();

                        LatLng m = new LatLng(addLocation.getLatitude(), addLocation.getLongitude());
                        int[] icons = {R.drawable.icon0, R.drawable.icon1, R.drawable.icon2, R.drawable.icon3, R.drawable.icon4, R.drawable.icon5, R.drawable.icon6, R.drawable.icon7};
                        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(icons[listView.getCheckedItemPosition()]);
                        Bitmap b = bitmapdraw.getBitmap();
                        Bitmap smallMarker;
                        smallMarker = Bitmap.createScaledBitmap(b, 72, 96, false);
                        mMap.addMarker(new MarkerOptions().position(m).title(title.getText().toString()).icon(BitmapDescriptorFactory.fromBitmap(smallMarker)).snippet("상세 정보 보기"));
                    }
                }
            }
        });

        dlg.show();
    }

    public void InfoDialog(String tit, final String con, String p, String nm, String lin, String dt, String dt2, final LatLng latLngInfo) {
        View dlgView = View.inflate(this,R.layout.dialog_info,null);
        final Dialog dlg = new Dialog(this);
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dlg.setContentView(dlgView);
        TextView ok, push;
        final TextView title, content, place, link, name, date, date2;

        push = (TextView)dlgView.findViewById(R.id.push_bt);
        ok = (TextView)dlgView.findViewById(R.id.ok_bt);
        title = (TextView)dlgView.findViewById(R.id.title);
        content = (TextView)dlgView.findViewById(R.id.content);
        place = (TextView)dlgView.findViewById(R.id.place);
        name = (TextView)dlgView.findViewById(R.id.name);
        link = (TextView)dlgView.findViewById(R.id.link);
        date = (TextView)dlgView.findViewById(R.id.date);
        date2 = (TextView)dlgView.findViewById(R.id.date2);

        title.setText(tit);
        content.setText(con);
        place.setText(p);
        name.setText(nm);
        link.setText(lin);
        date.setText(dt);
        date2.setText(dt2);

        push.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 푸시 알림 설정 하기
                placeList.add((String)place.getText());
                alarmList.add(latLngInfo);
//                AlertReceiver receiver = new AlertReceiver();
//                IntentFilter filter = new IntentFilter("jeong_won_hyeok.inhatc.talktudy.alert");
//                registerReceiver(receiver, filter);
//
//                Intent intent = new Intent("jeong_won_hyeok.inhatc.talktudy.alert");
//                PendingIntent proximityIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
//
//                try {
//                    locManager.addProximityAlert(latLngInfo.latitude, latLngInfo.longitude, 20, -1, proximityIntent);
//                } catch (SecurityException e) {
//                    e.printStackTrace();
//                }
                Toast.makeText(getApplicationContext(), "알림이 등록 되었습니다.", Toast.LENGTH_SHORT).show();
                dlg.dismiss();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0 && !recur) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);
                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());
                String markerTitle = getCurrentAddress(currentPosition);
                String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                        + " 경도:" + String.valueOf(location.getLongitude());
                Log.d(TAG, "onLocationResult : " + markerSnippet);
                //현재 위치에 마커 생성하고 이동
                setCurrentLocation(location, markerTitle, markerSnippet);
                if(!recur) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentPosition);
                    mMap.moveCamera(cameraUpdate);
                }
                recur=true;
            }
            mCurrentLocation = location;
            System.out.println("현재위치" + mCurrentLocation);
            System.out.println("placeList : " + placeList);
            System.out.println("alarmList : " + alarmList);
            // 알림 범위 내에 왔음을 감지하기
            if (isAlert(currentPosition)) alert();
        }
    };

    private void startLocationUpdates() {
        if (!checkLocationServicesStatus()) {
            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        }else {
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED   ) {

                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");
                return;
            }
            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            if (checkPermission())
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (checkPermission()) {
            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            if (mMap!=null)
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFusedLocationClient != null) {
            Log.d(TAG, "onStop : call stopLocationUpdates");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 어플 종료시 알람 리스트 저장
        if(placeList != null) {
            setStringArrayList("place",placeList);
            setStringArrayList("alarmm",getLatLngArrayListToStringArrayList(alarmList));
        }

    }

    public String getCurrentAddress(LatLng latlng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
            if (addresses == null || addresses.size() == 0) {
                addresses = geocoder.getFromLocation(
                        latlng.latitude,
                        latlng.longitude,
                        2);
            }
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }
        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }

    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        if (currentMarker != null) currentMarker.remove();

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        //currentMarker = mMap.addMarker(markerOptions);

        if(circleOptions == null) {
            circleOptions = new CircleOptions().center(currentLatLng)
                    .radius(1000)      //반지름 단위 : m
                    .strokeWidth(0f)  //선너비 0f : 선없음
                    .fillColor(Color.parseColor("#331187cf")); //배경색
            circle = mMap.addCircle(circleOptions);
        }
        circle.setCenter(currentLatLng);
    }

    public void setDefaultLocation() {
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 여부를 확인하세요.";

        if (currentMarker != null) currentMarker.remove();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        //currentMarker = mMap.addMarker(markerOptions);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mMap.moveCamera(cameraUpdate);
    }

    private boolean checkPermission() {
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {
        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            boolean check_result = true;
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }
            if ( check_result ) {
                startLocationUpdates();
            }
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();

                }else {
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();
                }
            }

        }
    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d(TAG, "onActivityResult : GPS 활성화 되있음");
                        needRequest = true;
                        return;
                    }
                }
                break;
            case 100: // 주소 선택
                if (resultCode == RESULT_OK) {
                    final Geocoder geocoder = new Geocoder(MapsActivity.this);
                    String address = data.getStringExtra("address");
                    List<Address> list = null;
                    try {
                        list = geocoder.getFromLocationName(address, 10);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (list != null) {
                        if (list.size() == 0)
                            Toast.makeText(MapsActivity.this, "잘못된 주소입니다.", Toast.LENGTH_SHORT).show();
                        else {
                            Address addr = list.get(0);
                            double latitude = addr.getLatitude();
                            double longitude = addr.getLongitude();

                            LatLng latLng = new LatLng(latitude, longitude);
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14);
                            mMap.moveCamera(cameraUpdate);
                        }
                    }
                }
        }
    }

    public boolean isAlert(LatLng latLng) {
        double distance;

        for(LatLng a : alarmList) {
            distance = getDistanceBetween(latLng.latitude, latLng.longitude, a.latitude, a.longitude);
            if (distance < 500) {
                return true;
            }
        }
        return false;
    }

    public void alert() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder= null;

        // API26이상은 NotificationChannel 필수
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            String channelID="알림 설정"; //알림채널 식별자
            String channelName="알림 설정"; //알림채널의 이름(별명)

            NotificationChannel channel= new NotificationChannel(channelID,channelName,NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            builder=new NotificationCompat.Builder(this, channelID);
        } else {
            builder= new NotificationCompat.Builder(this, null);
        }
        builder.setSmallIcon(android.R.drawable.ic_menu_view);

        builder.setContentTitle("근처에 도착하였습니다.");
        builder.setContentText("모임 장소가 맞는지 한번 더 확인해주세요!");

        // 큰 이미지
        Bitmap bitmap= BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_background);
        builder.setLargeIcon(bitmap);

        // 클릭 시 수행작업
        Intent intent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        // 클릭 시 알림 제거
        builder.setAutoCancel(true);

        // 알림 사운드
        Uri soundUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(soundUri);

        // 진동 효과
        builder.setVibrate(new long[]{1000, 1000});

        Notification notification=builder.build();

        // 알림(Notify) 요청
        notificationManager.notify(1, notification);

        // 요청 시 사용한 번호의 알람 제거
        //notificationManager.cancel(1);
    }

    // 알람 리스트 저장 메소드
    public void setStringArrayList(String key, ArrayList<String> list) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();    // 에디터 생성
        JSONArray jsonArray = new JSONArray();          // JSONArray 생성 - 여기에 Arraylist를 담음

        for(int i=0; i<list.size(); i++) {
            jsonArray.put(list.get(i));
        }

        if(!list.isEmpty()) {
            editor.putString(key, jsonArray.toString());
        } else {
            editor.putString(key, null);
        }
        editor.apply();
//        editor.commit();
    }

    // 알람 리스트 불러오기 메소드
    public ArrayList<String> getStringArrayList(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String json = sp.getString(key, null);
        ArrayList<String> list = new ArrayList<>();

        if(json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);

                for(int i=0; i<jsonArray.length(); i++){
                    String data = jsonArray.optString(i);
                    list.add(data);
                }
            } catch(JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return list;
    }

    // 알람 리스트 StringArrayList 인수로 받아서 LatLng 형태로 변환한 뒤 LatLngArrayList인  alarmList에 추가하기
    public void setStringArrayListToLatLngArrayList(ArrayList<String> stringArrayList) {
        if(stringArrayList != null) {
            for(int i=0; i<stringArrayList.size(); i++) {
                LatLng data = getStringToLatLng(stringArrayList.get(i));
                alarmList.add(data);
            }
        }
    }

    // 알람 리스트 LatLngArrayList 인수로 받아서 String 형태로 변환한 뒤 StringArrayList로 리턴하기
    public ArrayList<String> getLatLngArrayListToStringArrayList(ArrayList<LatLng> latlngArrayList) {
        ArrayList<String> result = new ArrayList<>();

        if(latlngArrayList != null) {
            for(int i=0; i<latlngArrayList.size(); i++) {
                String data = getLatLngToString(latlngArrayList.get(i));
                result.add(data);
            }
        }
        return result;
    }

    // LatLng 형태를 인수로 받아서 String 형태로 변환하고 리턴하기
    public String getLatLngToString(LatLng latlng) {
        Double lat = latlng.latitude;
        Double lng = latlng.longitude;
        String result = lat.toString() + "," + lng.toString();

        return result;
    }

    // String 형태를 인수로 받아서 LatLng 형태로 변환하고 리턴하기
    public LatLng getStringToLatLng(String string) {
        String[] latlng = string.split(",");
        LatLng result = new LatLng(Double.parseDouble(latlng[0]),Double.parseDouble(latlng[1]));

        return result;
    }

    public double getDistanceBetween(double P1_latitude, double P1_longitude, double P2_latitude, double P2_longitude) {
        if ((P1_latitude == P2_latitude) && (P1_longitude == P2_longitude)) {
            return 0;
        }

        double e10 = P1_latitude * Math.PI / 180;
        double e11 = P1_longitude * Math.PI / 180;
        double e12 = P2_latitude * Math.PI / 180;
        double e13 = P2_longitude * Math.PI / 180;

        /* 타원체 GRS80 */
        double c16 = 6356752.314140910;
        double c15 = 6378137.000000000;
        double c17 = 0.0033528107;

        double f15 = c17 + c17 * c17;
        double f16 = f15 / 2;
        double f17 = c17 * c17 / 2;
        double f18 = c17 * c17 / 8;
        double f19 = c17 * c17 / 16;

        double c18 = e13 - e11;
        double c20 = (1 - c17) * Math.tan(e10);
        double c21 = Math.atan(c20);
        double c22 = Math.sin(c21);
        double c23 = Math.cos(c21);
        double c24 = (1 - c17) * Math.tan(e12);
        double c25 = Math.atan(c24);
        double c26 = Math.sin(c25);
        double c27 = Math.cos(c25);

        double c29 = c18;
        double c31 = (c27 * Math.sin(c29) * c27 * Math.sin(c29))
                + (c23 * c26 - c22 * c27 * Math.cos(c29))
                * (c23 * c26 - c22 * c27 * Math.cos(c29));
        double c33 = (c22 * c26) + (c23 * c27 * Math.cos(c29));
        double c35 = Math.sqrt(c31) / c33;
        double c36 = Math.atan(c35);
        double c38 = 0;
        if (c31 == 0) {
            c38 = 0;
        } else {
            c38 = c23 * c27 * Math.sin(c29) / Math.sqrt(c31);
        }

        double c40 = 0;
        if ((Math.cos(Math.asin(c38)) * Math.cos(Math.asin(c38))) == 0) {
            c40 = 0;
        } else {
            c40 = c33 - 2 * c22 * c26
                    / (Math.cos(Math.asin(c38)) * Math.cos(Math.asin(c38)));
        }

        double c41 = Math.cos(Math.asin(c38)) * Math.cos(Math.asin(c38))
                * (c15 * c15 - c16 * c16) / (c16 * c16);
        double c43 = 1 + c41 / 16384
                * (4096 + c41 * (-768 + c41 * (320 - 175 * c41)));
        double c45 = c41 / 1024 * (256 + c41 * (-128 + c41 * (74 - 47 * c41)));
        double c47 = c45
                * Math.sqrt(c31)
                * (c40 + c45
                / 4
                * (c33 * (-1 + 2 * c40 * c40) - c45 / 6 * c40
                * (-3 + 4 * c31) * (-3 + 4 * c40 * c40)));
        double c50 = c17
                / 16
                * Math.cos(Math.asin(c38))
                * Math.cos(Math.asin(c38))
                * (4 + c17
                * (4 - 3 * Math.cos(Math.asin(c38))
                * Math.cos(Math.asin(c38))));
        double c52 = c18
                + (1 - c50)
                * c17
                * c38
                * (Math.acos(c33) + c50 * Math.sin(Math.acos(c33))
                * (c40 + c50 * c33 * (-1 + 2 * c40 * c40)));

        double c54 = c16 * c43 * (Math.atan(c35) - c47);

        // return distance in meter
        return c54;
    }
}