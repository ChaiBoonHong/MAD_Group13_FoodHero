package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.*;
import com.uccd3223.group13.foodhero.data.model.*;
import com.uccd3223.group13.foodhero.data.repository.*;
import java.io.*;
import java.util.*;

public class MerchantOnboardingActivity extends AppCompatActivity implements OnMapReadyCallback {
    private EditText business, description, phone, duitNowName;
    private Spinner campusSpinner;
    private ImageView qrPreview;
    private TextView pinLabel;
    private CheckBox terms;
    private Button complete;
    private ProgressBar progress;
    private MapView mapView;
    private GoogleMap map;
    private Marker marker;
    private final List<Campus> campuses = new ArrayList<>();
    private Uri qrUri;
    private double pinnedLat = Double.NaN, pinnedLng = Double.NaN;
    private AuthRepository auth;
    private FoodHeroRepository food;
    private ActivityResultLauncher<String> picker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_merchant_onboarding);
        auth=AuthRepository.getInstance(this); food=FoodHeroRepository.getInstance(this);
        business=findViewById(R.id.et_onboarding_business); description=findViewById(R.id.et_onboarding_description);
        phone=findViewById(R.id.et_onboarding_phone); duitNowName=findViewById(R.id.et_onboarding_duitnow_name);
        campusSpinner=findViewById(R.id.spinner_onboarding_campus); qrPreview=findViewById(R.id.iv_onboarding_qr);
        pinLabel=findViewById(R.id.tv_onboarding_pin); terms=findViewById(R.id.check_onboarding_terms);
        complete=findViewById(R.id.btn_complete_onboarding); progress=findViewById(R.id.progress_onboarding);
        mapView=findViewById(R.id.map_onboarding_location); mapView.onCreate(state); mapView.getMapAsync(this);
        picker=registerForActivityResult(new ActivityResultContracts.GetContent(),uri->{ if(uri!=null){qrUri=uri;qrPreview.setImageURI(uri);} });
        findViewById(R.id.btn_choose_duitnow_qr).setOnClickListener(v->picker.launch("image/*"));
        complete.setOnClickListener(v->submit());
        loadCampuses();
    }

    private void loadCampuses() {
        auth.getInstitutionsAndCampuses(new ResultCallback<List<Campus>>() {
            @Override public void onSuccess(List<Campus> result) {
                campuses.clear(); campuses.addAll(result);
                List<String> names=new ArrayList<>(); for(Campus c:campuses) names.add(c.getInstitutionCode()+" — "+c.getName());
                campusSpinner.setAdapter(new ArrayAdapter<>(MerchantOnboardingActivity.this,android.R.layout.simple_spinner_dropdown_item,names));
                campusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    public void onNothingSelected(AdapterView<?> p){} public void onItemSelected(AdapterView<?> p,View v,int pos,long id){centerCampus(pos);}});
            }
            @Override public void onError(DataError error){Toast.makeText(MerchantOnboardingActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();}
        });
    }

    private void centerCampus(int position){
        if(map==null||position<0||position>=campuses.size())return; Campus c=campuses.get(position);
        LatLng center=new LatLng(c.getLatitude(),c.getLongitude()); map.animateCamera(CameraUpdateFactory.newLatLngZoom(center,15f));
        pinnedLat=Double.NaN;pinnedLng=Double.NaN;if(marker!=null)marker.remove();pinLabel.setText("Tap the map to pin the stall within "+c.getName());
    }

    @Override public void onMapReady(GoogleMap googleMap){
        map=googleMap; map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnMapClickListener(point->{pinnedLat=point.latitude;pinnedLng=point.longitude;if(marker!=null)marker.remove();
            marker=map.addMarker(new MarkerOptions().position(point).title("Merchant pickup"));
            pinLabel.setText(String.format(Locale.US,"Pinned: %.6f, %.6f",pinnedLat,pinnedLng));});
        if(!campuses.isEmpty())centerCampus(campusSpinner.getSelectedItemPosition());
    }

    private void submit(){
        int pos=campusSpinner.getSelectedItemPosition();
        if(pos<0||pos>=campuses.size()||qrUri==null||Double.isNaN(pinnedLat)||!terms.isChecked()
            ||text(business).length()<2||text(description).length()<2||text(phone).length()<7||text(duitNowName).length()<2){
            Toast.makeText(this,"Complete all fields, select a QR, pin the stall and accept the terms.",Toast.LENGTH_LONG).show();return;}
        Campus campus=campuses.get(pos);
        if(distanceKm(campus.getLatitude(),campus.getLongitude(),pinnedLat,pinnedLng)>5.0){
            Toast.makeText(this,"The pin must be within 5 km of the selected main campus.",Toast.LENGTH_LONG).show();return;}
        setLoading(true);
        try(InputStream in=getContentResolver().openInputStream(qrUri)){
            Bitmap bitmap=BitmapFactory.decodeStream(in);if(bitmap==null)throw new IOException("Unreadable QR image");
            ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);
            food.uploadMerchantDuitNowQr(out.toByteArray(),new ResultCallback<String>(){
                @Override public void onSuccess(String path){finishRegistration(campus,path);}
                @Override public void onError(DataError error){setLoading(false);Toast.makeText(MerchantOnboardingActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();}});
        }catch(Exception e){setLoading(false);Toast.makeText(this,"Unable to read QR: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void finishRegistration(Campus campus,String path){
        String location=campus.getName()+" — "+String.format(Locale.US,"%.6f, %.6f",pinnedLat,pinnedLng);
        auth.completeMerchantRegistration(text(business),text(description),text(phone),text(duitNowName),path,campus,location,pinnedLat,pinnedLng,
            new ResultCallback<Merchant>(){
                @Override public void onSuccess(Merchant m){Intent i=new Intent(MerchantOnboardingActivity.this,MerchantHomeActivity.class);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(i);}
                @Override public void onError(DataError e){setLoading(false);Toast.makeText(MerchantOnboardingActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();}});
    }

    private String text(EditText e){return e.getText()==null?"":e.getText().toString().trim();}
    private void setLoading(boolean loading){progress.setVisibility(loading?View.VISIBLE:View.GONE);complete.setEnabled(!loading);}
    private double distanceKm(double a,double b,double c,double d){double r=6371,x=Math.toRadians(c-a),y=Math.toRadians(d-b);double q=Math.sin(x/2)*Math.sin(x/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(y/2)*Math.sin(y/2);return 2*r*Math.atan2(Math.sqrt(q),Math.sqrt(1-q));}
    @Override protected void onResume(){super.onResume();mapView.onResume();}
    @Override protected void onPause(){mapView.onPause();super.onPause();}
    @Override protected void onDestroy(){mapView.onDestroy();super.onDestroy();}
    @Override public void onLowMemory(){super.onLowMemory();mapView.onLowMemory();}
}
