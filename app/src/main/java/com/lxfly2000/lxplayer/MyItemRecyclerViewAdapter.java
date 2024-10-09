package com.lxfly2000.lxplayer;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

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

        public View GetRootView() {
            return rootView;
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

        public void SetOnCheckClickedListener(View.OnClickListener listener){
            //在RecyclerView的item滑动过程中，若使用OnCheckChanged监听状态会造成重复调用导致状态错乱
            checkBox.setOnClickListener(listener);
        }
    }

    public static class ItemData{
        ItemData(){this("","");}
        ItemData(String _name,String _value){this(_name,_value,false);}
        ItemData(String _name,String _value,boolean _checked){
            textName=_name;
            textValue=_value;
            checked=_checked;
        }
        String textName,textValue;
        boolean checked;
    }
    private ArrayList<ItemData> localDataSet;
    private boolean isEditing=false,showValue=false;
    public MyItemRecyclerViewAdapter(){
        localDataSet=new ArrayList<>();
    }

    public void Swap(int a,int b){
        Collections.swap(localDataSet,a,b);
    }

    public void SetShowValue(boolean b){
        showValue=b;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_row,parent,false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        if(showValue)
            holder.SetName(localDataSet.get(position).textName+"\n"+localDataSet.get(position).textValue);
        else
            holder.SetName(localDataSet.get(position).textName);
        holder.SetChecked(localDataSet.get(position).checked);
        holder.SetIsChoosing(isEditing);
        holder.SetOnItemClickListener(view -> {
            //注意此处不能用position
            if(!isEditing&&onPlaylistSelectedListener!=null) {
                int newPosition= holder.getAdapterPosition();
                onPlaylistSelectedListener.onPlaylistSelected(newPosition,localDataSet.get(newPosition).textName, localDataSet.get(newPosition).textValue);
            }
        });
        holder.SetOnCheckClickedListener(view -> {
            //注意此处不能用position
            int newPosition= holder.getAdapterPosition();
            localDataSet.get(newPosition).checked=((CheckBox)view).isChecked();
        });
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public void AddList(String name,String value){
        localDataSet.add(new ItemData(name,value));
        notifyItemInserted(localDataSet.size()-1);
    }

    public void DeleteList(String value){
        for(int i=0;i<localDataSet.size();i++){
            if(value.compareTo(localDataSet.get(i).textValue)==0){
                localDataSet.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void ClearList(){
        localDataSet.clear();
        notifyDataSetChanged();
    }

    public void DeleteCheckedItems(OnPlaylistSelectedListener deleteNameListener){
        for(int i=0;i<localDataSet.size();){
            if(localDataSet.get(i).checked){
                deleteNameListener.onPlaylistSelected(i,localDataSet.get(i).textName,localDataSet.get(i).textValue);
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
        void onPlaylistSelected(int pos,String name,String value);
    }
    private OnPlaylistSelectedListener onPlaylistSelectedListener=null;
    public void SetOnPlaylistClickListener(OnPlaylistSelectedListener listener){
        onPlaylistSelectedListener=listener;
    }
}
