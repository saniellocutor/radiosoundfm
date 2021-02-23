package com.streaminghost.radiosoundfm.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.streaminghost.radiosoundfm.R;
import com.streaminghost.radiosoundfm.activities.ActivityAbout;

import java.util.List;

public class AdapterAbout extends RecyclerView.Adapter<AdapterAbout.UserViewHolder> {

    private List<ActivityAbout.Data> dataList;
    private Context context;

    public AdapterAbout(List<ActivityAbout.Data> dataList, Context context) {
        this.dataList = dataList;
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.lsv_item_about, null);
        UserViewHolder userViewHolder = new UserViewHolder(view);
        return userViewHolder;
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, final int position) {

        ActivityAbout.Data data = dataList.get(position);

        holder.image.setImageResource(data.getImage());
        holder.title.setText(data.getTitle());
        holder.sub_title.setText(data.getSub_title());

        holder.relativeLayout.setOnClickListener(view -> {
            if (position == 4) {
                final String appName = context.getPackageName();
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
                }
            } else if (position == 5) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.play_more_apps))));
            } else {
                Log.d("Log", "Do Nothing!");
            }
        });

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title;
        TextView sub_title;
        RelativeLayout relativeLayout;

        public UserViewHolder(View itemView) {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.image);
            title = (TextView) itemView.findViewById(R.id.title);
            sub_title = (TextView) itemView.findViewById(R.id.sub_title);
            relativeLayout = (RelativeLayout) itemView.findViewById(R.id.lyt_parent);
        }

    }

}