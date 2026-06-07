package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SelfCheckoutCartManager;

import java.util.Random;
import java.util.UUID;

public class ExitPassActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exit_pass);

        ImageView ivQrCode = findViewById(R.id.ivQrCode);
        TextView tvOrderId = findViewById(R.id.tvOrderId);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        Button btnDone = findViewById(R.id.btnDone);

        // Generate a unique order ID for QR code
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String orderId = "NM-" + uuid;
        String qrData = "NM_MART_EXIT_PASS:" + orderId;

        double total = SelfCheckoutCartManager.getInstance(this).getTotalPrice();

        tvOrderId.setText("Order #" + orderId);
        tvTotalAmount.setText("Total: ₹" + String.format("%.2f", total));

        try {
            Bitmap qrBitmap = generateQRCode(qrData);
            ivQrCode.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        btnDone.setOnClickListener(v -> {
            SelfCheckoutCartManager.getInstance(this).clearCart();
            Intent intent = new Intent(ExitPassActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private Bitmap generateQRCode(String data) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        
        return bitmap;
    }
}
