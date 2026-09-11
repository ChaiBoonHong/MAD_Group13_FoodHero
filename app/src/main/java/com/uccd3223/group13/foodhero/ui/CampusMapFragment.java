package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
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
import com.uccd3223.group13.foodhero.data.model.Campus;
import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.GeoPoint;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CampusMapFragment extends Fragment implements OnMapReadyCallback {
    private MapView mapView;
    private GoogleMap googleMap;
    private FoodHeroRepository foodHeroRepo;
    private ChipGroup chipGroupTravelMode;
    private MaterialCardView cardRouteInfo, cardDealPreview;
    private ImageView ivRouteModeIcon, ivPreviewImage;
    private TextView tvRouteDistanceEta, tvEntranceFallbackWarning, tvPreviewTitle,
        tvPreviewMerchant, tvPreviewPrice, tvNearbyCount;
    private MaterialButton btnPreviewViewDeal;
    private TravelMode selectedTravelMode = TravelMode.WALKING;
    private final Map<Marker, Listing> markerListingMap = new HashMap<>();
    private Polyline currentRoutePolyline;
    private Marker studentMarker;
    private Listing selectedListing;
    private Double studentLat, studentLng;
    private FusedLocationProviderClient locationClient;
    private String campusId;
    private int pendingMapLoads;

    private final ActivityResultLauncher<String> locationPermission = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) loadStudentLocation();
            else showWarning("Location permission denied. You can browse campus deals, but route guidance needs location access.");
        });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campus_map, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        initViews(view);
        setupListeners();
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void initViews(View view) {
        mapView = view.findViewById(R.id.map_view);
        chipGroupTravelMode = view.findViewById(R.id.chip_group_travel_mode);
        cardRouteInfo = view.findViewById(R.id.card_route_info);
        cardDealPreview = view.findViewById(R.id.card_deal_preview);
        ivRouteModeIcon = view.findViewById(R.id.iv_route_mode_icon);
        ivPreviewImage = view.findViewById(R.id.iv_preview_image);
        tvRouteDistanceEta = view.findViewById(R.id.tv_route_distance_eta);
        tvEntranceFallbackWarning = view.findViewById(R.id.tv_entrance_fallback_warning);
        tvPreviewTitle = view.findViewById(R.id.tv_preview_title);
        tvPreviewMerchant = view.findViewById(R.id.tv_preview_merchant);
        tvPreviewPrice = view.findViewById(R.id.tv_preview_price);
        tvNearbyCount = view.findViewById(R.id.tv_nearby_count);
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
            if (selectedListing != null) drawRouteToListing(selectedListing);
        });
        btnPreviewViewDeal.setOnClickListener(v -> {
            if (selectedListing == null) return;
            Intent intent = new Intent(requireContext(), ListingDetailsActivity.class);
            intent.putExtra("extra_listing", selectedListing);
            startActivity(intent);
        });
    }

    @Override public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setMinZoomPreference(14f);
        googleMap.setMaxZoomPreference(20f);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setOnMarkerClickListener(marker -> {
            Listing listing = markerListingMap.get(marker);
            if (listing == null) return false;
            showListingPreview(listing);
            drawRouteToListing(listing);
            return false;
        });
        googleMap.setOnMapClickListener(point -> cardDealPreview.setVisibility(View.GONE));
        loadCurrentCampus();
        requestStudentLocation();
    }

    private void loadCurrentCampus() {
        AuthRepository auth = AuthRepository.getInstance(requireContext());
        campusId = auth.getCurrentProfile() == null ? null : auth.getCurrentProfile().getCampusId();
        if (campusId == null || campusId.trim().isEmpty()) {
            showWarning("Select and verify your campus before loading campus map data.");
            return;
        }
        auth.getInstitutionsAndCampuses(new ResultCallback<List<Campus>>() {
            @Override public void onSuccess(List<Campus> campuses) {
                if (!isAdded() || googleMap == null) return;
                boolean found = false;
                if (campuses != null) for (Campus campus : campuses) {
                    if (campusId.equals(campus.getId())) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(campus.getLatitude(), campus.getLongitude()), 16f));
                        found = true;
                        break;
                    }
                }
                if (!found) showWarning("Your verified campus could not be loaded.");
                loadMapData();
            }
            @Override public void onError(DataError error) {
                showWarning("Unable to load the campus centre. Check your connection and retry.");
                loadMapData();
            }
        });
    }

    private void requestStudentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) loadStudentLocation();
        else locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void loadStudentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (!isAdded() || googleMap == null || location == null) return;
            studentLat = location.getLatitude();
            studentLng = location.getLongitude();
            if (studentMarker != null) studentMarker.remove();
            studentMarker = googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(studentLat, studentLng)).title("Your current location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        });
    }

    private void loadMapData() {
        if (googleMap == null || campusId == null || campusId.trim().isEmpty()) return;
        googleMap.clear();
        markerListingMap.clear();
        studentMarker = null;
        if (studentLat != null && studentLng != null) {
            studentMarker = googleMap.addMarker(new MarkerOptions().position(new LatLng(studentLat, studentLng))
                .title("Your current location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        }
        pendingMapLoads = 2;
        foodHeroRepo.getCampusLandmarks(campusId, new ResultCallback<List<CampusLandmark>>() {
            @Override public void onSuccess(List<CampusLandmark> landmarks) {
                if (!isAdded() || googleMap == null) return;
                if (landmarks != null) for (CampusLandmark landmark : landmarks) {
                    if (!validCoordinate(landmark.getLatitude(), landmark.getLongitude())) continue;
                    float hue = landmark.isEntrance() ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_CYAN;
                    googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(landmark.getLatitude(), landmark.getLongitude()))
                        .title(landmark.getName()).snippet(landmark.getCategory())
                        .icon(BitmapDescriptorFactory.defaultMarker(hue)));
                }
                mapLoadFinished();
            }
            @Override public void onError(DataError error) { showWarning(error.getMessage()); mapLoadFinished(); }
        });
        foodHeroRepo.getActiveFeedForCampus(campusId, new ResultCallback<List<Listing>>() {
            @Override public void onSuccess(List<Listing> listings) {
                if (!isAdded() || googleMap == null) return;
                int count = listings == null ? 0 : listings.size();
                tvNearbyCount.setText(getResources().getQuantityString(R.plurals.campus_spots_nearby, count, count));
                Map<String, Integer> locationTotals = new HashMap<>();
                Map<String, Integer> locationIndexes = new HashMap<>();
                if (listings != null) for (Listing listing : listings) {
                    if (!validCoordinate(listing.getLatitude(), listing.getLongitude())) continue;
                    String key = coordinateKey(listing);
                    locationTotals.put(key, locationTotals.getOrDefault(key, 0) + 1);
                }
                if (listings != null) for (Listing listing : listings) {
                    if (!validCoordinate(listing.getLatitude(), listing.getLongitude())) continue;
                    String key = coordinateKey(listing);
                    int markerIndex = locationIndexes.getOrDefault(key, 0);
                    locationIndexes.put(key, markerIndex + 1);
                    Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(getVisibleMarkerPosition(listing, markerIndex, locationTotals.get(key)))
                        .title(listing.getTitle()).snippet(CurrencyUtils.format(listing.getDiscountedPrice()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    if (marker != null) markerListingMap.put(marker, listing);
                }
                mapLoadFinished();
            }
            @Override public void onError(DataError error) {
                tvNearbyCount.setText("Campus spots unavailable");
                showWarning(error.getMessage());
                mapLoadFinished();
            }
        });
    }

    private String coordinateKey(Listing listing) {
        return String.format(Locale.US, "%.6f,%.6f", listing.getLatitude(), listing.getLongitude());
    }

    /**
     * Separates markers which share one pickup landmark so every listing remains tappable.
     * This is display-only: directions and listing details continue using the saved coordinates.
     */
    private LatLng getVisibleMarkerPosition(Listing listing, int index, int totalAtLocation) {
        double latitude = listing.getLatitude();
        double longitude = listing.getLongitude();
        if (totalAtLocation <= 1) return new LatLng(latitude, longitude);

        double angle = (2.0 * Math.PI * index / totalAtLocation) - (Math.PI / 2.0);
        double radiusMeters = 8.0;
        double latitudeOffset = radiusMeters * Math.sin(angle) / 111_320.0;
        double longitudeScale = 111_320.0 * Math.max(0.2, Math.cos(Math.toRadians(latitude)));
        double longitudeOffset = radiusMeters * Math.cos(angle) / longitudeScale;
        return new LatLng(latitude + latitudeOffset, longitude + longitudeOffset);
    }

    private void mapLoadFinished() {
        pendingMapLoads = Math.max(0, pendingMapLoads - 1);
        if (pendingMapLoads == 0 && tvEntranceFallbackWarning.getVisibility() == View.VISIBLE
            && markerListingMap.size() > 0) tvEntranceFallbackWarning.setVisibility(View.GONE);
    }

    private boolean validCoordinate(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
            && !(latitude == 0 && longitude == 0);
    }

    private void showListingPreview(Listing listing) {
        selectedListing = listing;
        cardDealPreview.setVisibility(View.VISIBLE);
        tvPreviewTitle.setText(listing.getTitle());
        String merchant = listing.getMerchant() == null ? "Campus Merchant" : listing.getMerchant().getBusinessName();
        tvPreviewMerchant.setText(String.format("%s • %s", merchant, listing.getPickupLocation()));
        tvPreviewPrice.setText(CurrencyUtils.format(listing.getDiscountedPrice()));
        if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
            Glide.with(this).load(listing.getImageUrl()).placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder).centerCrop().into(ivPreviewImage);
        } else ivPreviewImage.setImageResource(R.drawable.ic_food_placeholder);
    }

    private void drawRouteToListing(Listing listing) {
        if (studentLat == null || studentLng == null) {
            showWarning("Enable location to calculate a Google route to this pickup point.");
            return;
        }
        float[] directDistance = new float[1];
        android.location.Location.distanceBetween(studentLat, studentLng,
            listing.getLatitude(), listing.getLongitude(), directDistance);
        if (directDistance[0] <= 25f) {
            if (currentRoutePolyline != null) {
                currentRoutePolyline.remove();
                currentRoutePolyline = null;
            }
            cardRouteInfo.setVisibility(View.VISIBLE);
            tvRouteDistanceEta.setText("You are at the pickup point");
            tvEntranceFallbackWarning.setVisibility(View.GONE);
            return;
        }
        cardRouteInfo.setVisibility(View.VISIBLE);
        tvRouteDistanceEta.setText("Calculating Google route…");
        tvEntranceFallbackWarning.setVisibility(View.GONE);
        foodHeroRepo.calculateRoute(studentLat, studentLng, listing.getLatitude(), listing.getLongitude(),
            selectedTravelMode, new ResultCallback<RouteResult>() {
                @Override public void onSuccess(RouteResult route) {
                    if (!isAdded() || googleMap == null || route == null || route.getPoints() == null) return;
                    if (currentRoutePolyline != null) currentRoutePolyline.remove();
                    PolylineOptions options = new PolylineOptions().color(Color.parseColor("#216E39")).width(12f);
                    for (GeoPoint point : route.getPoints()) options.add(new LatLng(point.getLatitude(), point.getLongitude()));
                    currentRoutePolyline = googleMap.addPolyline(options);
                    cardRouteInfo.setVisibility(View.VISIBLE);
                    String distance = route.getDistanceMeters() < 1000
                        ? String.format(Locale.US, "%.0fm", route.getDistanceMeters())
                        : String.format(Locale.US, "%.1fkm", route.getDistanceKm());
                    tvRouteDistanceEta.setText(String.format(Locale.US, "%s • ~%d mins", distance, route.getDurationMinutes()));
                    tvEntranceFallbackWarning.setVisibility(View.GONE);
                }
                @Override public void onError(DataError error) {
                    showWarning(error == null ? "Google route guidance is unavailable. Tap the marker to retry."
                        : error.getMessage() + " Tap the marker to retry.");
                }
            });
    }

    private void showWarning(String message) {
        if (!isAdded() || tvEntranceFallbackWarning == null) return;
        cardRouteInfo.setVisibility(View.VISIBLE);
        tvEntranceFallbackWarning.setVisibility(View.VISIBLE);
        tvEntranceFallbackWarning.setText(message == null ? "Campus map data could not be loaded." : message);
    }

    public void refreshMapData() {
        if (googleMap != null) loadCurrentCampus();
    }

    @Override public void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }
    @Override public void onPause() { if (mapView != null) mapView.onPause(); super.onPause(); }
    @Override public void onDestroy() { if (mapView != null) mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); if (mapView != null) mapView.onLowMemory(); }
}
