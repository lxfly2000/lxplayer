package com.lxfly2000.lxplayer;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ItemMoveCallback extends ItemTouchHelper.Callback{
    public interface OnItemMovedListener{
        void itemMoved(int fromPos,int toPos);
    }
    private OnItemMovedListener itemMovedListener=null;
    public void SetOnItemMovedListener(OnItemMovedListener listener){
        itemMovedListener=listener;
    }

    private final MyItemRecyclerViewAdapter myAdapter;

    public void ViewItemMoved(int fromPos, int toPos) {
        if (fromPos < toPos) {
            for (int i = fromPos; i < toPos; i++) {
                myAdapter.Swap(i, i + 1);
            }
        } else {
            for (int i = fromPos; i > toPos; i--) {
                myAdapter.Swap(i, i - 1);
            }
        }
        myAdapter.notifyItemMoved(fromPos, toPos);
        if(itemMovedListener!=null)
            itemMovedListener.itemMoved(fromPos,toPos);
    }

    public void ViewItemSelected(MyItemRecyclerViewAdapter.ItemViewHolder vh) {
        vh.GetRootView().setBackgroundColor(Color.GRAY);
    }

    public void ViewItemClear(MyItemRecyclerViewAdapter.ItemViewHolder vh) {
        vh.GetRootView().setBackgroundColor(Color.WHITE);
    }

    public ItemMoveCallback(MyItemRecyclerViewAdapter adapter) {
        myAdapter=adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }



    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                          RecyclerView.ViewHolder target) {
        ViewItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder,
                                  int actionState) {


        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            if (viewHolder instanceof MyItemRecyclerViewAdapter.ItemViewHolder) {
                MyItemRecyclerViewAdapter.ItemViewHolder myViewHolder=
                        (MyItemRecyclerViewAdapter.ItemViewHolder) viewHolder;
                ViewItemSelected(myViewHolder);
            }

        }

        super.onSelectedChanged(viewHolder, actionState);
    }
    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (viewHolder instanceof MyItemRecyclerViewAdapter.ItemViewHolder) {
            MyItemRecyclerViewAdapter.ItemViewHolder myViewHolder=
                    (MyItemRecyclerViewAdapter.ItemViewHolder) viewHolder;
            ViewItemClear(myViewHolder);
        }
    }
}
