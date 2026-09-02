package com.uccd3223.group13.foodhero.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.ui.adapter.NotificationAdapter;
import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {
    private FoodHeroRepository foodHeroRepo;
    private NotificationAdapter adapter;
    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        foodHeroRepo = FoodHeroRepository.getInstance(this);

        initViews();
        loadNotifications();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_notifications);
        rvNotifications = findViewById(R.id.rv_notifications_list);
        layoutEmpty = findViewById(R.id.layout_empty_notifications);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new NotificationAdapter(this, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        UserRole role = SessionManager.getInstance(this).getUserRole();
        foodHeroRepo.getNotifications(role, new ResultCallback<List<FoodHeroNotification>>() {
            @Override
            public void onSuccess(List<FoodHeroNotification> list) {
                adapter.setItems(list);
                layoutEmpty.setVisibility((list == null || list.isEmpty()) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(DataError error) {
                adapter.setItems(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onNotificationClick(FoodHeroNotification notification) {
        if (!notification.isRead()) {
            notification.setRead(true);
            adapter.notifyDataSetChanged();
            foodHeroRepo.markNotificationRead(notification.getId(), new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {}

                @Override
                public void onError(DataError error) {}
            });
        }
        Toast.makeText(this, notification.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
