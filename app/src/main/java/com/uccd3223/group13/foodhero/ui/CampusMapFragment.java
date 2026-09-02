package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.GeoPoint;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.util.CampusBoundaryManager;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampusMapFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private FoodHeroRepository foodHeroRepo;

    private ChipGroup chipGroupTravelMode;
    private MaterialCardView cardRouteInfo, cardDealPreview;
    private ImageView ivRouteModeIcon, ivPreviewImage;
    private TextView tvRouteDistanceEta, tvEntranceFallbackWarning, tvPreviewTitle, tvPreviewMerchant, tvPreviewPrice;
    private MaterialButton btnPreviewViewDeal;

    private TravelMode selectedTravelMode = TravelMode.WALKING;
    private final Map<Marker, Listing> markerListingMap = new HashMap<>();
    private Polyline currentRoutePolyline;
    private Listing selectedListing;

    // Simulated student position on UTAR Kampar campus (Near FICT Block N)
    private double studentLat = 4.336500;
    private double studentLng = 101.140200;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        initViews(view);
        setupListeners();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void initViews(View view) {
        mapView = view.findViewById(R.id.map_view);
        chipGroupTravelMode = view.findViewById(R.id.chip_group_travel_mode);
        cardRouteInfo = view.findViewById(R.id.card_route_info);
        ivRouteModeIcon = view.findViewById(R.id.iv_route_mode_icon);
        tvRouteDistanceEta = view.findViewById(R.id.tv_route_distance_eta);
        tvEntranceFallbackWarning = view.findViewById(R.id.tv_entrance_fallback_warning);

        cardDealPreview = view.findViewById(R.id.card_deal_preview);
        ivPreviewImage = view.findViewById(R.id.iv_preview_image);
        tvPreviewTitle = view.findViewById(R.id.tv_preview_title);
        tvPreviewMerchant = view.findViewById(R.id.tv_preview_merchant);
        tvPreviewPrice = view.findViewById(R.id.tv_preview_price);
        btnPreviewViewDeal = view.findViewById(R.id.btn_preview_view_deal);
    }

    private void setupListeners() {
        chipGroupTravelMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_mode_bike) {
                selectedTravelMode = TravelMode.CYCLING;
                ivRouteModeIcon.setImageResource(R.drawable.ic_bike);
            } else if (checkedId == R.id.chip_mode_shuttle) {
                selectedTravelMode = TravelMode.SHUTTLE;
                ivRouteModeIcon.setImageResource(R.drawable.ic_shuttle);
            } else {
                selectedTravelMode = TravelMode.WALKING;
                ivRouteModeIcon.setImageResource(R.drawable.ic_walk);
            }
            if (selectedListing != null) {
                drawRouteToListing(selectedListing);
            }
        });

        btnPreviewViewDeal.setOnClickListener(v -> {
            if (selectedListing != null) {
                Intent intent = new Intent(requireContext(), ListingDetailsActivity.class);
                intent.putExtra("extra_listing", selectedListing);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Center on UTAR Kampar Campus
        LatLng utarKamparCenter = new LatLng(CampusBoundaryManager.CAMPUS_CENTER_LAT, CampusBoundaryManager.CAMPUS_CENTER_LNG);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(utarKamparCenter, 15.8f));

        // Restrict bounds so camera doesn't scroll off-campus
        LatLngBounds campusBounds = new LatLngBounds(
            new LatLng(4.327000, 101.135000), // Southwest
            new LatLng(4.344500, 101.150000)  // Northeast
        );
        googleMap.setLatLngBoundsForCameraTarget(campusBounds);
        googleMap.setMinZoomPreference(14.0f);
        googleMap.setMaxZoomPreference(19.0f);

        // Add Student Marker
        googleMap.addMarker(new MarkerOptions()
            .position(new LatLng(studentLat, studentLng))
            .title("You (Student Location)")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // Load Landmarks and Listings
        loadMapData();

        // Marker Click Listener
        googleMap.setOnMarkerClickListener(marker -> {
            Listing listing = markerListingMap.get(marker);
            if (listing != null) {
                showListingPreview(listing);
                drawRouteToListing(listing);
                return true;
            }
            return false;
        });

        googleMap.setOnMapClickListener(latLng -> {
            cardDealPreview.setVisibility(View.GONE);
        });
    }

    private void loadMapData() {
        // Add Campus Landmarks
        foodHeroRepo.getCampusLandmarks(new ResultCallback<List<CampusLandmark>>() {
            @Override
            public void onSuccess(List<CampusLandmark> landmarks) {
                if (googleMap == null || landmarks == null) return;
                for (CampusLandmark lm : landmarks) {
                    float color = lm.isEntrance() ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_CYAN;
                    googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lm.getLatitude(), lm.getLongitude()))
                        .title(lm.getName())
                        .snippet(lm.getCategory())
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));
                }
            }

            @Override
            public void onError(DataError error) {}
        });

        // Add Surplus Food Listings
        foodHeroRepo.getActiveFeed(new ResultCallback<List<Listing>>() {
            @Override
            public void onSuccess(List<Listing> listings) {
                if (googleMap == null || listings == null) return;
                for (Listing l : listings) {
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(l.getLatitude(), l.getLongitude()))
                        .title(l.getTitle())
                        .snippet(CurrencyUtils.format(l.getDiscountedPrice()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    if (marker != null) {
                        markerListingMap.put(marker, l);
                    }
                }
            }

            @Override
            public void onError(DataError error) {}
        });
    }

    private void showListingPreview(Listing listing) {
        selectedListing = listing;
        cardDealPreview.setVisibility(View.VISIBLE);
        tvPreviewTitle.setText(listing.getTitle());
        String merchantName = listing.getMerchant() != null ? listing.getMerchant().getBusinessName() : "Campus Merchant";
        tvPreviewMerchant.setText(String.format("%s • %s", merchantName, listing.getPickupLocation()));
        tvPreviewPrice.setText(CurrencyUtils.format(listing.getDiscountedPrice()));

        if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
            Glide.with(this)
                .load(listing.getImageUrl())
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .centerCrop()
                .into(ivPreviewImage);
        } else {
            ivPreviewImage.setImageResource(R.drawable.ic_food_placeholder);
        }
    }

    private void drawRouteToListing(Listing listing) {
        foodHeroRepo.calculateRoute(studentLat, studentLng, listing.getLatitude(), listing.getLongitude(), selectedTravelMode, new ResultCallback<RouteResult>() {
            @Override
            public void onSuccess(RouteResult route) {
                if (googleMap == null || route == null) return;

                if (currentRoutePolyline != null) {
                    currentRoutePolyline.remove();
                }

                PolylineOptions polyOptions = new PolylineOptions()
                    .color(Color.parseColor("#216E39"))
                    .width(12f)
                    .geodesic(true);

                for (GeoPoint p : route.getPoints()) {
                    polyOptions.add(new LatLng(p.getLatitude(), p.getLongitude()));
                }
                currentRoutePolyline = googleMap.addPolyline(polyOptions);

                // Update Route Summary Card
                cardRouteInfo.setVisibility(View.VISIBLE);
                String distStr = route.getDistanceMeters() < 1000
                    ? String.format("%.0fm", route.getDistanceMeters())
                    : String.format("%.1fkm", route.getDistanceKm());
                String modeLabel = (selectedTravelMode == TravelMode.CYCLING) ? "cycling"
                    : (selectedTravelMode == TravelMode.SHUTTLE) ? "shuttle" : "walk";
                tvRouteDistanceEta.setText(String.format("%s • ~%d mins %s", distStr, route.getDurationMinutes(), modeLabel));

                if (route.isFallbackEntrance()) {
                    tvEntranceFallbackWarning.setVisibility(View.VISIBLE);
                    tvEntranceFallbackWarning.setText("Outside campus: Route starting from " + route.getEntranceName());
                } else {
                    tvEntranceFallbackWarning.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(DataError error) {}
        });
    }

    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause() { super.onPause(); mapView.onPause(); }
    @Override public void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
