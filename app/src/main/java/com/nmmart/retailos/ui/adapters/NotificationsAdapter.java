package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.nmmart.retailos.R;
import com.nmmart.retailos.models.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private final List<NotificationItem> notifications = new ArrayList<>();

    public NotificationsAdapter(Context context, List<NotificationItem> notifications) {
        if (notifications != null) {
            this.notifications.addAll(notifications);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvMessage.setText(item.message);

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
        String timeStr = sdf.format(new Date(item.timestamp));
        holder.tvTime.setText(timeStr);

        // Show unread dot and adjust alpha
        holder.viewUnreadDot.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
        holder.itemView.setAlpha(item.isRead ? 0.6f : 1.0f);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<NotificationItem> newNotifications) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NotificationDiffCallback(this.notifications, newNotifications));
        this.notifications.clear();
        if (newNotifications != null) {
            this.notifications.addAll(newNotifications);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime;
        View viewUnreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }
    }

    private static class NotificationDiffCallback extends DiffUtil.Callback {
        private final List<NotificationItem> oldList;
        private final List<NotificationItem> newList;

        public NotificationDiffCallback(List<NotificationItem> oldList, List<NotificationItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).id == newList.get(newPos).id;
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            NotificationItem oldItem = oldList.get(oldPos);
            NotificationItem newItem = newList.get(newPos);
            return oldItem.title.equals(newItem.title) &&
                   oldItem.message.equals(newItem.message) &&
                   oldItem.isRead == newItem.isRead &&
                   oldItem.timestamp == newItem.timestamp;
        }
    }
}
