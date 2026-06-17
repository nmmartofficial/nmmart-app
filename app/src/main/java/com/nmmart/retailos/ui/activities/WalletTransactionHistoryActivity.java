package com.nmmart.retailos.ui.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.List;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;
import com.nmmart.retailos.ui.adapters.WalletTransactionAdapter;
import com.nmmart.retailos.utils.WalletTransactionStorage;

public class WalletTransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private LinearLayout layoutEmptyState;
    private WalletTransactionAdapter adapter;
    private WalletTransactionStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_transaction_history);

        storage = WalletTransactionStorage.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvTransactions = findViewById(R.id.rvWalletTransactions);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        setupRecyclerView();
        loadTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void setupRecyclerView() {
        adapter = new WalletTransactionAdapter(this);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
        setupSwipeToDelete();
    }
    
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                List<com.nmmart.retailos.models.WalletTransaction> items = storage.getTransactions();
                com.nmmart.retailos.models.WalletTransaction item = items.get(position);
                storage.deleteTransaction(item.getId());
                loadTransactions();
                Toast.makeText(WalletTransactionHistoryActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float itemHeight = itemView.getBottom() - itemView.getTop();
                
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#F44336"));
                
                RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                c.drawRect(background, paint);
                
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(48f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                String text = "DELETE";
                float textWidth = textPaint.measureText(text);
                float x = itemView.getRight() - (textWidth / 2) - 48;
                float y = itemView.getTop() + (itemHeight / 2) + (textPaint.descent() + textPaint.ascent()) / 2;
                c.drawText(text, x, y, textPaint);
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        
        new ItemTouchHelper(callback).attachToRecyclerView(rvTransactions);
    }

    private void loadTransactions() {
        List<com.nmmart.retailos.models.WalletTransaction> transactions = storage.getTransactions();
        if (transactions.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            adapter.updateTransactions(transactions);
        }
    }
}
