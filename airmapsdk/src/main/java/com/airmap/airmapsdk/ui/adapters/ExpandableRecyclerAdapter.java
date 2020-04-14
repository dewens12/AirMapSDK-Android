package com.airmap.airmapsdk.ui.adapters;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public abstract class ExpandableRecyclerAdapter<P,C> extends RecyclerView.Adapter {

    protected static final int PARENT_VIEW_TYPE = 0;
    protected static final int CHILD_VIEW_TYPE = 1;

    protected LinkedHashMap<P, List<C>> dataMap;
    private Set<P> expandedParents;

    public ExpandableRecyclerAdapter(LinkedHashMap<P,List<C>> dataMap) {
        this.dataMap = dataMap;
        expandedParents = new HashSet<>();
    }

    public void setData(@Nullable LinkedHashMap<P,List<C>> dataMap) {
        this.dataMap = dataMap;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case PARENT_VIEW_TYPE: {
                final P parent = (P) getItem(position);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!expandedParents.contains(parent)) {
                            expandedParents.add(parent);
                            toggleExpandingViewHolder(holder, true);
                            notifyItemRangeInserted(holder.getAdapterPosition() + 1, dataMap.get(parent).size());
                        } else {
                            expandedParents.remove(parent);
                            toggleExpandingViewHolder(holder, false);
                            notifyItemRangeRemoved(holder.getAdapterPosition() + 1, dataMap.get(parent).size());
                        }
                    }
                });

                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = getItem(position);
        if (dataMap.containsKey(item)) {
            return PARENT_VIEW_TYPE;
        } else {
            return CHILD_VIEW_TYPE;
        }
    }

    protected abstract void toggleExpandingViewHolder(final RecyclerView.ViewHolder holder, final boolean expanded);

    public Object getItem(int position) {
        int index = 0;
        for (P parent : dataMap.keySet()) {
            if (index == position) {
                return parent;
            }
            index++;

            if (expandedParents.contains(parent)) {
                List<C> children = dataMap.get(parent);
                if (index + children.size() > position) {
                    return children.get(position - index);
                }
                index += children.size();
            }
        }

        return null;
    }

    protected boolean isExpanded(P parent) {
        return expandedParents.contains(parent);
    }

    public void expandAll() {
        expandedParents.addAll(dataMap.keySet());
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (dataMap == null) {
            return 0;
        }

        int count = 0;
        for (P key : dataMap.keySet()) {
            count++; // add 1 for parent

            if (expandedParents.contains(key)) {
                count += dataMap.get(key).size();
            }
        }

        return count;
    }
}
