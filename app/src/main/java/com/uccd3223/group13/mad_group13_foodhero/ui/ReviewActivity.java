package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Order;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Review;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.FoodHeroRepository;

public class ReviewActivity extends AppCompatActivity {
    private Order order;
    private FoodHeroRepository foodHeroRepo;

    private Toolbar toolbar;
    private TextView tvItemTitle, tvMerchantName;
    private RatingBar ratingBar;
    private EditText etComment;
    private MaterialButton btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        foodHeroRepo = FoodHeroRepository.getInstance(this);
        order = (Order) getIntent().getSerializableExtra("extra_order");

        if (order == null) {
            Toast.makeText(this, "Order information unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindData();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_review);
        tvItemTitle = findViewById(R.id.tv_review_item_title);
        tvMerchantName = findViewById(R.id.tv_review_merchant_name);
        ratingBar = findViewById(R.id.rating_bar_review);
        etComment = findViewById(R.id.et_review_comment);
        btnSubmit = findViewById(R.id.btn_submit_review);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindData() {
        String title = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Meal Bag";
        tvItemTitle.setText(title);

        String merchant = (order.getMerchant() != null) ? order.getMerchant().getBusinessName() : "Campus Merchant";
        String loc = (order.getMerchant() != null) ? order.getMerchant().getCampusLocation() : "UTAR Kampar";
        tvMerchantName.setText(String.format("%s • %s", merchant, loc));
    }

    private void setupListeners() {
        btnSubmit.setOnClickListener(v -> {
            int rating = Math.max(1, (int) ratingBar.getRating());
            String comment = etComment.getText().toString().trim();

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting...");

            foodHeroRepo.submitReview(order.getId(), order.getListingId(), order.getMerchantId(), rating, comment, new ResultCallback<Review>() {
                @Override
                public void onSuccess(Review review) {
                    Toast.makeText(ReviewActivity.this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(DataError error) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(R.string.submit_review);
                    Toast.makeText(ReviewActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
