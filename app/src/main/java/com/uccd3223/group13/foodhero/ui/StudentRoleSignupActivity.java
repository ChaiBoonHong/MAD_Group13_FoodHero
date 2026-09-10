package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.util.MotionUtils;
import com.uccd3223.group13.foodhero.util.SystemBarUtils;

public class StudentRoleSignupActivity extends AppCompatActivity {
    private View root, detailsStep, codeStep, successStep;
    private TextInputLayout emailLayout, idLayout, facultyLayout, codeLayout;
    private EditText email, studentId, faculty, code;
    private TextView stepLabel, destination, successSummary;
    private LinearProgressIndicator stepProgress;
    private MaterialButton primary, back, changeEmail, resend;
    private ProgressBar loading;
    private AuthRepository auth;
    private int step = 1;
    private boolean busy;
    private CountDownTimer timer;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_student_role_signup);
        root = findViewById(R.id.root_student_role_signup);
        SystemBarUtils.applySafeInsets(this, root);
        auth = AuthRepository.getInstance(this);
        bindViews();
        if (state != null) step = state.getInt("step", 1);
        setupActions();
        render(false);
        MotionUtils.enter(root);
    }

    private void bindViews() {
        detailsStep = findViewById(R.id.step_student_details); codeStep = findViewById(R.id.step_student_code);
        successStep = findViewById(R.id.step_student_success);
        emailLayout = findViewById(R.id.til_student_signup_email); idLayout = findViewById(R.id.til_student_signup_id);
        facultyLayout = findViewById(R.id.til_student_signup_faculty); codeLayout = findViewById(R.id.til_student_signup_code);
        email = findViewById(R.id.et_student_signup_email); studentId = findViewById(R.id.et_student_signup_id);
        faculty = findViewById(R.id.et_student_signup_faculty); code = findViewById(R.id.et_student_signup_code);
        stepLabel = findViewById(R.id.tv_student_signup_step); destination = findViewById(R.id.tv_student_code_destination);
        successSummary = findViewById(R.id.tv_student_success_summary); stepProgress = findViewById(R.id.progress_student_signup_steps);
        primary = findViewById(R.id.btn_student_signup_primary); back = findViewById(R.id.btn_student_signup_back);
        changeEmail = findViewById(R.id.btn_student_change_email); resend = findViewById(R.id.btn_student_resend_code);
        loading = findViewById(R.id.progress_student_signup);
    }

    private void setupActions() {
        primary.setOnClickListener(v -> { MotionUtils.press(v); if (step == 1) sendCode(); else if (step == 2) verifyCode(); });
        back.setOnClickListener(v -> { if (busy) return; if (step == 2) { step = 1; render(true); } else finish(); });
        changeEmail.setOnClickListener(v -> { if (!busy) { step = 1; render(true); } });
        resend.setOnClickListener(v -> sendCode());
    }

    private void sendCode() {
        if (!validateDetails() || busy) return;
        setBusy(true, "Sending…");
        auth.requestInstitutionalEmailVerification(text(email), new ResultCallback<Void>() {
            @Override public void onSuccess(Void ignored) {
                setBusy(false, null); step = 2; destination.setText("We sent a code to " + text(email)); render(true); startResendTimer();
            }
            @Override public void onError(DataError error) { setBusy(false, null); showError(error.getMessage()); }
        });
    }

    private void verifyCode() {
        codeLayout.setError(null);
        if (!text(code).matches("\\d{6}")) { codeLayout.setError("Enter the complete 6-digit code."); return; }
        setBusy(true, "Verifying…");
        auth.addStudentRole(text(email), text(code), text(studentId), text(faculty), new ResultCallback<Profile>() {
            @Override public void onSuccess(Profile profile) {
                setBusy(false, null); step = 3;
                successSummary.setText((profile.getInstitutionName() == null ? "University identity verified" : profile.getInstitutionName())
                    + "\nStudent ID: " + text(studentId));
                render(true);
                successStep.postDelayed(() -> {
                    Intent intent = new Intent(StudentRoleSignupActivity.this, StudentHomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); startActivity(intent);
                }, MotionUtils.enabled() ? 700L : 0L);
            }
            @Override public void onError(DataError error) { setBusy(false, null); codeLayout.setError(error.getMessage()); }
        });
    }

    private boolean validateDetails() {
        emailLayout.setError(null); idLayout.setError(null); facultyLayout.setError(null); boolean valid = true;
        if (!text(email).matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { emailLayout.setError("Enter a valid institutional email."); valid = false; }
        if (text(studentId).length() < 3) { idLayout.setError("Enter your Student ID."); valid = false; }
        if (text(faculty).length() < 2) { facultyLayout.setError("Enter your faculty."); valid = false; }
        return valid;
    }

    private void render(boolean animate) {
        View outgoing = detailsStep.getVisibility() == View.VISIBLE ? detailsStep : codeStep.getVisibility() == View.VISIBLE ? codeStep : successStep;
        View incoming = step == 1 ? detailsStep : step == 2 ? codeStep : successStep;
        if (animate && outgoing != incoming) MotionUtils.crossfade(outgoing, incoming);
        else { detailsStep.setVisibility(step == 1 ? View.VISIBLE : View.GONE); codeStep.setVisibility(step == 2 ? View.VISIBLE : View.GONE); successStep.setVisibility(step == 3 ? View.VISIBLE : View.GONE); }
        stepProgress.setProgressCompat(step, animate && MotionUtils.enabled());
        stepLabel.setText(step == 1 ? "Step 1 of 3 · Student details" : step == 2 ? "Step 2 of 3 · Verify code" : "Step 3 of 3 · Complete");
        primary.setVisibility(step == 3 ? View.GONE : View.VISIBLE); back.setVisibility(step == 3 ? View.GONE : View.VISIBLE);
        primary.setText(step == 1 ? "Send code" : "Verify and activate"); back.setText(step == 1 ? "Cancel" : "Back");
    }

    private void startResendTimer() {
        if (timer != null) timer.cancel(); resend.setEnabled(false);
        timer = new CountDownTimer(60000, 1000) {
            public void onTick(long left) { resend.setText("Resend in " + Math.max(1, left / 1000) + "s"); }
            public void onFinish() { resend.setText("Resend code"); resend.setEnabled(true); }
        }.start();
    }

    private void setBusy(boolean value, String label) {
        busy = value; loading.setVisibility(value ? View.VISIBLE : View.GONE); primary.setEnabled(!value); back.setEnabled(!value);
        if (label != null) primary.setText(label); else primary.setText(step == 1 ? "Send code" : "Verify and activate");
    }

    private String text(EditText value) { return value.getText() == null ? "" : value.getText().toString().trim(); }
    private void showError(String message) { Snackbar.make(root, message == null ? "Unable to continue. Try again." : message, Snackbar.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putInt("step", step); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
