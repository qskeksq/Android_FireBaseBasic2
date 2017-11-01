package com.example.administrator.firebasebasic2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017-10-31.
 */

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.Holder> {

    Callback callback;
    List<User> users = new ArrayList<>();

    public UserAdapter(Callback callback) {
        this.callback = callback;
    }

    public void setDataAndRefresh(List<User> users){
        this.users = users;
        notifyDataSetChanged();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        User user = users.get(position);
        holder.textId.setText(user.email);
        holder.token = user.token;
    }

    @Override
    public int getItemCount() {
        return users.size();
    }


    public class Holder extends RecyclerView.ViewHolder {

        TextView textId;
        String token;

        public Holder(View itemView) {
            super(itemView);
                                                    // 안드로이드 기본 레이아웃을 사용했을 때 뷰를 찾아오는 주소
            textId = (TextView) itemView.findViewById(android.R.id.text1);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.setIdAndToken(textId.getText().toString(), token);
                }
            });
        }
    }

    public interface Callback {
        void setIdAndToken(String id, String token);
    }
}
