package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Campus;
import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.Merchant;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.util.MotionUtils;
import com.uccd3223.group13.foodhero.util.SystemBarUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MerchantOnboardingActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String EXTRA_EDIT_MODE = "edit_merchant_profile";
    public static final String EXTRA_START_STEP = "start_step";
    private static final long MAX_QR_BYTES = 5L * 1024L * 1024L;
    private static final String STATE_STEP = "merchant_step";
    private static final String STATE_QR = "merchant_qr";
    private static final String STATE_CAMPUS = "merchant_campus";
    private static final String STATE_LAT = "merchant_lat";
    private static final String STATE_LNG = "merchant_lng";
    private static final String STATE_UPLOADED_QR = "merchant_uploaded_qr";

    private EditText business, description, phone, duitNowName;
    private TextInputLayout businessLayout, descriptionLayout, phoneLayout, duitNowLayout, campusLayout, landmarkLayout;
    private MaterialAutoCompleteTextView campusDropdown, landmarkDropdown;
    private ImageView qrPreview;
    private TextView qrStatus, pinLabel, review, stepLabel, title;
    private CheckBox terms;
    private MaterialButton primary, back, chooseQr, removeQr;
    private ProgressBar loading;
    private LinearProgressIndicator stepProgress;
    private View businessStep, paymentStep, locationStep, root;
    private MapView mapView;
    private GoogleMap map;
    private Marker marker;
    private final List<Campus> campuses = new ArrayList<>();
    private final List<CampusLandmark> landmarks = new ArrayList<>();
    private Uri qrUri;
    private String uploadedQrPath;
    private double pinnedLat = Double.NaN, pinnedLng = Double.NaN;
    private int selectedCampus = -1;
    private CampusLandmark selectedLandmark;
    private int currentStep = 1;
    private boolean submitting;
    private boolean editMode;
    private Merchant existingMerchant;
    private AuthRepository auth;
    private FoodHeroRepository food;
    private ActivityResultLauncher<String> picker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_merchant_onboarding);
        root = findViewById(R.id.root_merchant_onboarding);
        SystemBarUtils.applySafeInsets(this, root);
        auth = AuthRepository.getInstance(this);
        food = FoodHeroRepository.getInstance(this);
        editMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false);
        if (state == null && getIntent().hasExtra(EXTRA_START_STEP)) {
            currentStep = getIntent().getIntExtra(EXTRA_START_STEP, 1);
        }
        bindViews();
        restoreState(state);
        mapView.onCreate(state);
        mapView.getMapAsync(this);
        setupLaunchers();
        setupActions();
        renderStep(false);
        loadCampuses();
        if (editMode && state == null) loadExistingMerchant();
        MotionUtils.enter(root);
    }

    private void bindViews() {
        ImageView btnClose = findViewById(R.id.btn_onboarding_close);
        if (btnClose != null) {
            btnClose.setVisibility(editMode ? View.VISIBLE : View.GONE);
            btnClose.setOnClickListener(v -> finish());
        }
        business = findViewById(R.id.et_onboarding_business);
        description = findViewById(R.id.et_onboarding_description);
        phone = findViewById(R.id.et_onboarding_phone);
        duitNowName = findViewById(R.id.et_onboarding_duitnow_name);
        businessLayout = findViewById(R.id.til_onboarding_business);
        descriptionLayout = findViewById(R.id.til_onboarding_description);
        phoneLayout = findViewById(R.id.til_onboarding_phone);
        duitNowLayout = findViewById(R.id.til_onboarding_duitnow_name);
        campusLayout = findViewById(R.id.til_onboarding_campus);
        campusDropdown = findViewById(R.id.dropdown_onboarding_campus);
        landmarkLayout = findViewById(R.id.til_onboarding_landmark);
        landmarkDropdown = findViewById(R.id.dropdown_onboarding_landmark);
        qrPreview = findViewById(R.id.iv_onboarding_qr);
        qrStatus = findViewById(R.id.tv_onboarding_qr_status);
        pinLabel = findViewById(R.id.tv_onboarding_pin);
        review = findViewById(R.id.tv_onboarding_review);
        stepLabel = findViewById(R.id.tv_onboarding_step);
        title = findViewById(R.id.tv_onboarding_title);
        title.setText(editMode ? "Edit Merchant Profile" : "Complete Merchant Setup");
        terms = findViewById(R.id.check_onboarding_terms);
        primary = findViewById(R.id.btn_complete_onboarding);
        back = findViewById(R.id.btn_onboarding_back);
        chooseQr = findViewById(R.id.btn_choose_duitnow_qr);
        removeQr = findViewById(R.id.btn_remove_duitnow_qr);
        loading = findViewById(R.id.progress_onboarding);
        stepProgress = findViewById(R.id.progress_onboarding_steps);
        businessStep = findViewById(R.id.step_onboarding_business);
        paymentStep = findViewById(R.id.step_onboarding_payment);
        locationStep = findViewById(R.id.step_onboarding_location);
        mapView = findViewById(R.id.map_onboarding_location);
        mapView.setOnTouchListener((view, event) -> {
            // The map is nested in a scrolling review step. Keep pan, pinch and
            // marker-drag gestures with Google Maps until the gesture finishes.
            int action = event.getActionMasked();
            boolean interacting = action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL;
            view.getParent().requestDisallowInterceptTouchEvent(interacting);
            return false;
        });
    }

    private void setupLaunchers() {
        picker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            try {
                String type = getContentResolver().getType(uri);
                if (type != null && !type.startsWith("image/")) {
                    throw new IOException("Choose a JPEG, PNG or WebP image.");
                }
                try (android.content.res.AssetFileDescriptor fd = getContentResolver().openAssetFileDescriptor(uri, "r")) {
                    if (fd != null && fd.getLength() > MAX_QR_BYTES) {
                        throw new IOException("The QR image must be 5 MB or smaller.");
                    }
                } catch (IOException e) {
                    if (e.getMessage() != null && e.getMessage().contains("5 MB")) throw e;
                }
                qrUri = uri;
                uploadedQrPath = null;
                renderQr();
            } catch (Exception error) {
                showError(error.getMessage());
            }
        });
    }

    private void setupActions() {
        chooseQr.setOnClickListener(v -> { MotionUtils.press(v); picker.launch("image/*"); });
        removeQr.setOnClickListener(v -> { qrUri = null; uploadedQrPath = null; qrPreview.setImageDrawable(null); renderQr(); });
        back.setOnClickListener(v -> { if (!submitting && currentStep > 1) { currentStep--; renderStep(true); } });
        primary.setOnClickListener(v -> {
            MotionUtils.press(v);
            if (submitting) return;
            if (currentStep == 1 && validateBusiness()) { currentStep = 2; renderStep(true); }
            else if (currentStep == 2 && validatePayment()) { currentStep = 3; renderStep(true); }
            else if (currentStep == 3) submit();
        });
    }

    private void loadCampuses() {
        campusLayout.setEnabled(false);
        auth.getInstitutionsAndCampuses(new ResultCallback<List<Campus>>() {
            @Override public void onSuccess(List<Campus> result) {
                if (isFinishing()) return;
                campuses.clear();
                if (result != null) campuses.addAll(result);
                List<String> names = new ArrayList<>();
                for (Campus c : campuses) names.add(c.getInstitutionCode() + " — " + c.getName());
                campusDropdown.setSimpleItems(names.toArray(new String[0]));
                campusLayout.setError(null);
                campusDropdown.setOnItemClickListener((parent, view, position, id) -> {
                    selectedCampus = position;
                    selectedLandmark = null;
                    pinnedLat = Double.NaN;
                    pinnedLng = Double.NaN;
                    landmarkDropdown.setText("", false);
                    if (marker != null) marker.remove();
                    centerCampus(position);
                    loadLandmarks(campuses.get(position));
                    updateReview();
                });
                campusLayout.setEnabled(true);
                if (campuses.isEmpty()) {
                    campusLayout.setError("No active campus is configured.");
                    primary.setEnabled(false);
                } else if (selectedCampus >= 0 && selectedCampus < campuses.size()) {
                    primary.setEnabled(!submitting);
                    campusDropdown.setText(names.get(selectedCampus), false);
                    centerCampus(selectedCampus);
                    loadLandmarks(campuses.get(selectedCampus));
                } else {
                    primary.setEnabled(!submitting);
                }
                applyExistingCampus();
            }
            @Override public void onError(DataError error) {
                campusLayout.setEnabled(true);
                String detail = error != null && error.getMessage() != null ? error.getMessage() : "Unable to load campuses.";
                campusLayout.setError(detail + " Tap to retry.");
                campusDropdown.setOnClickListener(v -> loadCampuses());
            }
        });
    }

    private void loadLandmarks(Campus campus) {
        selectedLandmark = null;
        landmarks.clear();
        landmarkDropdown.setText("", false);
        landmarkLayout.setError(null);
        landmarkLayout.setEnabled(false);
        pinLabel.setText("Loading Supabase-defined pickup landmarks…");
        food.getCampusLandmarks(campus.getId(), new ResultCallback<List<CampusLandmark>>() {
            @Override public void onSuccess(List<CampusLandmark> result) {
                if (isFinishing() || selectedCampus < 0
                    || !campuses.get(selectedCampus).getId().equals(campus.getId())) return;
                if (result != null) landmarks.addAll(result);
                List<String> names = new ArrayList<>();
                for (CampusLandmark landmark : landmarks) names.add(landmark.getName());
                landmarkDropdown.setSimpleItems(names.toArray(new String[0]));
                landmarkDropdown.setOnItemClickListener((parent, view, position, id) ->
                    selectLandmark(landmarks.get(position)));
                landmarkLayout.setEnabled(true);
                if (landmarks.isEmpty()) {
                    landmarkLayout.setError("No active pickup landmarks are configured for this campus.");
                    pinLabel.setText("Ask an administrator to configure a campus pickup landmark.");
                    return;
                }
                restoreSelectedLandmark();
                if (selectedLandmark == null)
                    pinLabel.setText("Select one of the Supabase-defined pickup landmarks above.");
            }
            @Override public void onError(DataError error) {
                landmarkLayout.setEnabled(true);
                landmarkLayout.setError("Unable to load pickup landmarks. Tap to retry.");
                landmarkDropdown.setOnClickListener(v -> loadLandmarks(campus));
            }
        });
    }

    private void restoreSelectedLandmark() {
        String savedLocation = existingMerchant == null ? null : existingMerchant.getCampusLocation();
        for (CampusLandmark landmark : landmarks) {
            boolean sameName = savedLocation != null && savedLocation.contains(landmark.getName());
            boolean samePoint = !Double.isNaN(pinnedLat)
                && Math.abs(pinnedLat - landmark.getLatitude()) < 0.000001
                && Math.abs(pinnedLng - landmark.getLongitude()) < 0.000001;
            if (sameName || samePoint) {
                selectLandmark(landmark);
                return;
            }
        }
    }

    private void selectLandmark(CampusLandmark landmark) {
        selectedLandmark = landmark;
        pinnedLat = landmark.getLatitude();
        pinnedLng = landmark.getLongitude();
        landmarkDropdown.setText(landmark.getName(), false);
        setMerchantPin(new LatLng(pinnedLat, pinnedLng));
        if (map != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(pinnedLat, pinnedLng), 18f));
        landmarkLayout.setError(null);
        updateReview();
    }

    private boolean validateBusiness() {
        boolean valid = true;
        businessLayout.setError(null); descriptionLayout.setError(null); phoneLayout.setError(null);
        if (text(business).length() < 2) { businessLayout.setError("Enter the business name."); valid = false; }
        if (text(description).length() < 10) { descriptionLayout.setError("Describe the stall in at least 10 characters."); valid = false; }
        if (!text(phone).matches("^[+0-9][0-9 -]{6,18}$")) { phoneLayout.setError("Enter a valid contact phone number."); valid = false; }
        return valid;
    }

    private boolean validatePayment() {
        duitNowLayout.setError(null);
        if (text(duitNowName).length() < 2) { duitNowLayout.setError("Enter the DuitNow display name."); return false; }
        if (qrUri == null && uploadedQrPath == null) { showError("Choose the real DuitNow QR image used by this merchant."); return false; }
        return true;
    }

    private void renderStep(boolean animate) {
        View outgoing = businessStep.getVisibility() == View.VISIBLE ? businessStep
            : paymentStep.getVisibility() == View.VISIBLE ? paymentStep : locationStep;
        View incoming = currentStep == 1 ? businessStep : currentStep == 2 ? paymentStep : locationStep;
        if (animate && outgoing != incoming) MotionUtils.crossfade(outgoing, incoming);
        else {
            businessStep.setVisibility(currentStep == 1 ? View.VISIBLE : View.GONE);
            paymentStep.setVisibility(currentStep == 2 ? View.VISIBLE : View.GONE);
            locationStep.setVisibility(currentStep == 3 ? View.VISIBLE : View.GONE);
        }
        stepProgress.setProgressCompat(currentStep, animate && MotionUtils.enabled());
        stepLabel.setText(currentStep == 1 ? "Step 1 of 3 · Business details"
            : currentStep == 2 ? "Step 2 of 3 · DuitNow payment" : "Step 3 of 3 · Pickup and review");
        primary.setText(currentStep == 3 ? (editMode ? "Save Profile" : "Activate") : "Continue");
        back.setVisibility(currentStep == 1 ? View.INVISIBLE : View.VISIBLE);
        if (currentStep == 3) updateReview();
    }

    private void renderQr() {
        boolean selected = qrUri != null || uploadedQrPath != null;
        qrPreview.setVisibility(selected ? View.VISIBLE : View.GONE);
        removeQr.setVisibility(selected ? View.VISIBLE : View.GONE);
        chooseQr.setText(selected ? "Replace QR" : "Choose QR");
        if (qrUri != null) {
            qrStatus.setText("New QR selected and ready to save");
            Glide.with(this).load(qrUri).into(qrPreview);
        } else if (uploadedQrPath != null) {
            qrStatus.setText("Current verified DuitNow QR · choose Replace QR to change it");
            String url = com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.getMerchantQrUrl(uploadedQrPath);
            GlideUrl authorized = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + com.uccd3223.group13.foodhero.data.session.SessionManager.getInstance(this).getAccessToken())
                .addHeader("apikey", com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.SUPABASE_ANON_KEY).build());
            Glide.with(this).load(authorized).placeholder(R.drawable.ic_qr_code).error(R.drawable.ic_qr_code).into(qrPreview);
        } else {
            qrStatus.setText("No QR selected\nJPEG, PNG or WebP · maximum 5 MB");
            Glide.with(this).clear(qrPreview);
            qrPreview.setImageDrawable(null);
        }
    }

    private void centerCampus(int position) {
        if (map == null || position < 0 || position >= campuses.size()) return;
        Campus campus = campuses.get(position);
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(campus.getLatitude(), campus.getLongitude()), 16f));
        pinLabel.setText("Loading Supabase-defined pickup landmarks for " + campus.getName() + "…");
    }

    @Override public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setZoomGesturesEnabled(true);
        map.getUiSettings().setScrollGesturesEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        if (!Double.isNaN(pinnedLat)) setMerchantPin(new LatLng(pinnedLat, pinnedLng));
        else if (selectedCampus >= 0) centerCampus(selectedCampus);
    }

    private void setMerchantPin(LatLng point) {
        pinnedLat = point.latitude; pinnedLng = point.longitude;
        if (marker != null) marker.remove();
        marker = map.addMarker(new MarkerOptions().position(point)
            .title(selectedLandmark == null ? "Supabase pickup landmark" : selectedLandmark.getName())
            .draggable(false));
        if (marker != null) marker.showInfoWindow();
        String name = selectedLandmark == null ? "Supabase pickup landmark" : selectedLandmark.getName();
        pinLabel.setText(String.format(Locale.US, "%s · %.6f, %.6f", name, pinnedLat, pinnedLng));
        updateReview();
    }

    private void updateReview() {
        String campus = selectedCampus >= 0 && selectedCampus < campuses.size() ? campuses.get(selectedCampus).getName() : "No campus selected";
        String pin = selectedLandmark == null ? "No Supabase pickup landmark selected"
            : selectedLandmark.getName() + " — " + String.format(Locale.US, "%.6f, %.6f", pinnedLat, pinnedLng);
        review.setText(text(business) + "\nDuitNow: " + text(duitNowName) + "\nCampus: " + campus + "\nPickup: " + pin);
    }

    private void submit() {
        terms.setError(null); campusLayout.setError(null); landmarkLayout.setError(null);
        if (selectedCampus < 0 || selectedCampus >= campuses.size()) { campusLayout.setError("Select an active campus."); return; }
        if (selectedLandmark == null) { landmarkLayout.setError("Select a Supabase-defined pickup landmark."); return; }
        if (!terms.isChecked()) { terms.setError(editMode ? "Confirm the updated details." : "Confirm the details before activation."); return; }
        setLoading(true);
        if (qrUri != null) {
            uploadNewQrAndFinish(campuses.get(selectedCampus));
        } else if (uploadedQrPath != null) {
            finishRegistration(campuses.get(selectedCampus), uploadedQrPath);
        } else {
            setLoading(false);
            showError("Choose the real DuitNow QR image used by this merchant.");
        }
    }

    private void uploadNewQrAndFinish(Campus campus) {
        try (InputStream input = getContentResolver().openInputStream(qrUri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);

            int sampleSize = 1;
            int maxDim = Math.max(options.outWidth, options.outHeight);
            while (maxDim / sampleSize > 2048) {
                sampleSize *= 2;
            }

            Bitmap bitmap;
            try (InputStream secondInput = getContentResolver().openInputStream(qrUri)) {
                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                decodeOptions.inSampleSize = sampleSize;
                bitmap = BitmapFactory.decodeStream(secondInput, null, decodeOptions);
            }

            if (bitmap == null) throw new IOException("The selected QR image can no longer be read.");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output);
            food.uploadMerchantDuitNowQr(output.toByteArray(), new ResultCallback<String>() {
                @Override public void onSuccess(String path) {
                    uploadedQrPath = path;
                    qrUri = null;
                    finishRegistration(campus, path);
                }
                @Override public void onError(DataError error) { setLoading(false); showError(error.getMessage()); }
            });
        } catch (Exception error) { setLoading(false); showError("Unable to read QR: " + error.getMessage()); }
    }

    private void finishRegistration(Campus campus, String path) {
        String location = selectedLandmark.getName();
        auth.addMerchantRole(text(business), text(description), text(phone), text(duitNowName), path,
            campus, location, pinnedLat, pinnedLng, new ResultCallback<Merchant>() {
                @Override public void onSuccess(Merchant merchant) {
                    primary.setText(editMode ? "Merchant profile updated" : "Merchant account activated");
                    MotionUtils.success(primary, () -> {
                        if (editMode) {
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Intent intent = new Intent(MerchantOnboardingActivity.this, MerchantHomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }
                    });
                }
                @Override public void onError(DataError error) { setLoading(false); showError(error.getMessage()); }
            });
    }

    private void setLoading(boolean active) {
        submitting = active;
        loading.setVisibility(active ? View.VISIBLE : View.GONE);
        primary.setEnabled(!active);
        back.setEnabled(!active);
        primary.setText(active ? (editMode ? "Saving…" : "Activating…")
            : currentStep == 3 ? (editMode ? "Save Profile" : "Activate") : "Continue");
    }

    private void loadExistingMerchant() {
        food.getMerchantProfile(null, new ResultCallback<Merchant>() {
            @Override public void onSuccess(Merchant merchant) {
                if (isFinishing() || merchant == null) return;
                existingMerchant = merchant;
                business.setText(merchant.getBusinessName());
                description.setText(merchant.getStallDescription());
                phone.setText(merchant.getContactPhone());
                duitNowName.setText(merchant.getDuitNowDisplayName());
                uploadedQrPath = merchant.getDuitNowQrPath();
                pinnedLat = merchant.getLatitude();
                pinnedLng = merchant.getLongitude();
                if (editMode) {
                    terms.setChecked(true);
                    terms.setText("I confirm the updated business, payment, and pickup details are accurate.");
                }
                if (uploadedQrPath != null) {
                    String url = com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.getMerchantQrUrl(uploadedQrPath);
                    GlideUrl authorized = new GlideUrl(url, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + com.uccd3223.group13.foodhero.data.session.SessionManager.getInstance(MerchantOnboardingActivity.this).getAccessToken())
                        .addHeader("apikey", com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.SUPABASE_ANON_KEY).build());
                    Glide.with(MerchantOnboardingActivity.this).load(authorized).error(R.drawable.ic_qr_code).into(qrPreview);
                }
                renderQr();
                applyExistingCampus();
                if (map != null && !Double.isNaN(pinnedLat)) setMerchantPin(new LatLng(pinnedLat, pinnedLng));
            }
            @Override public void onError(DataError error) {
                showError(error == null ? "Merchant information could not be loaded." : error.getMessage());
            }
        });
    }

    private void applyExistingCampus() {
        if (existingMerchant == null || campuses.isEmpty() || selectedCampus >= 0) return;
        for (int i = 0; i < campuses.size(); i++) {
            if (campuses.get(i).getId().equals(existingMerchant.getCampusId())) {
                selectedCampus = i;
                campusDropdown.setText(campuses.get(i).getInstitutionCode() + " — " + campuses.get(i).getName(), false);
                if (map != null) centerCampus(i);
                loadLandmarks(campuses.get(i));
                updateReview();
                return;
            }
        }
    }

    private void showError(String message) {
        Snackbar.make(root, message == null ? "Something went wrong. Try again." : message, Snackbar.LENGTH_LONG).show();
    }

    private String text(EditText field) { return field.getText() == null ? "" : field.getText().toString().trim(); }

    private void restoreState(Bundle state) {
        if (state == null) return;
        currentStep = state.getInt(STATE_STEP, 1);
        selectedCampus = state.getInt(STATE_CAMPUS, -1);
        pinnedLat = state.getDouble(STATE_LAT, Double.NaN);
        pinnedLng = state.getDouble(STATE_LNG, Double.NaN);
        String uri = state.getString(STATE_QR);
        uploadedQrPath = state.getString(STATE_UPLOADED_QR);
        if (uri != null) { qrUri = Uri.parse(uri); renderQr(); }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt(STATE_STEP, currentStep);
        out.putInt(STATE_CAMPUS, selectedCampus);
        out.putDouble(STATE_LAT, pinnedLat);
        out.putDouble(STATE_LNG, pinnedLng);
        if (qrUri != null) out.putString(STATE_QR, qrUri.toString());
        if (uploadedQrPath != null) out.putString(STATE_UPLOADED_QR, uploadedQrPath);
        mapView.onSaveInstanceState(out);
        super.onSaveInstanceState(out);
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
