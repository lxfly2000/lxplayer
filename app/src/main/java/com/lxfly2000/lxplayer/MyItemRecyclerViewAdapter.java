package com.lxfly2000.lxplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ItemViewHolder>{
    public static class ItemViewHolder extends RecyclerView.ViewHolder{
        private final TextView textView;
        private final CheckBox checkBox;
        private final View rootView;
        public ItemViewHolder(@NonNull View itemView){
            super(itemView);
            rootView=itemView.getRootView();
            textView=itemView.findViewById(R.id.textViewPlaylistName);
            checkBox=itemView.findViewById(R.id.checkBoxPlaylistName);
        }

        public void SetName(String name){
            textView.setText(name);
            checkBox.setText(name);
        }

        public void SetIsChoosing(boolean b){
            textView.setVisibility(b?View.GONE:View.VISIBLE);
            checkBox.setVisibility(b?View.VISIBLE:View.GONE);
        }

        public void SetChecked(boolean b){
            checkBox.setChecked(b);
        }

        public void SetOnItemClickListener(View.OnClickListener listener){
            rootView.setOnClickListener(listener);
        }

        public void SetOnCheckChangedListener(CompoundButton.OnCheckedChangeListener listener){
            checkBox.setOnCheckedChangeListener(listener);
        }
    }

    public static class ItemData{
        ItemData(){this("");}
        ItemData(String _name){this(_name,false);}
        ItemData(String _name,boolean _checked){
            name=_name;
            checked=_checked;
        }
        String name;
        boolean checked;
    }
    private ArrayList<ItemData> localDataSet;
    private boolean isEditing=false;
    public MyItemRecyclerViewAdapter(){
        localDataSet=new ArrayList<>();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_row,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.SetName(localDataSet.get(position).name);
        holder.SetChecked(localDataSet.get(position).checked);
        holder.SetIsChoosing(isEditing);
        holder.SetOnItemClickListener(view -> {
            if(!isEditing&&onPlaylistSelectedListener!=null)
                onPlaylistSelectedListener.onPlaylistSelected(localDataSet.get(position).name);
        });
        holder.SetOnCheckChangedListener((compoundButton, b) -> localDataSet.get(position).checked=b);
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void AddList(String name){
        localDataSet.add(new ItemData(name));
        notifyItemInserted(localDataSet.size()-1);
    }

    public void DeleteList(String name){
        for(int i=0;i<localDataSet.size();i++){
            if(name.compareTo(localDataSet.get(i).name)==0){
                localDataSet.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void DeleteChecked(OnPlaylistSelectedListener deleteNameListener){
        for(int i=0;i<localDataSet.size();){
            if(localDataSet.get(i).checked){
                deleteNameListener.onPlaylistSelected(localDataSet.get(i).name);
                localDataSet.remove(i);
                notifyItemRemoved(i);
            }else {
                i++;
            }
        }
    }

    public void SetIsChoosing(Boolean b){
        isEditing=b;
        if(!isEditing){
            for (ItemData d : localDataSet) {
                d.checked = false;
            }
        }
        notifyDataSetChanged();
    }

    public boolean GetIsChoosing(){
        return isEditing;
    }

    public interface OnPlaylistSelectedListener{
        void onPlaylistSelected(String name);
    }
    private OnPlaylistSelectedListener onPlaylistSelectedListener=null;
    public void SetOnPlaylistClickListener(OnPlaylistSelectedListener listener){
        onPlaylistSelectedListener=listener;
    }
}
