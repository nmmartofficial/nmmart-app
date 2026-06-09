package com.nmmart.retailos.utils;

import android.content.Context;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;

@GlideModule
public class NMMartGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Set default format to RGB_565 for faster loading (uses half the memory of ARGB_8888)
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_RGB_565)
        );
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false; // Disable manifest parsing for better performance
    }
}

