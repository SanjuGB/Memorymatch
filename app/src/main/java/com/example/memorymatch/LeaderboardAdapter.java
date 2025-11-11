package com.example.memorymatch;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {

    public interface OnDeleteClick { void onDelete(LeaderboardItem item); }

    private final List<LeaderboardItem> items = new ArrayList<>();
    private final String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    private final OnDeleteClick onDelete;

    public LeaderboardAdapter(OnDeleteClick onDelete) { this.onDelete = onDelete; }

    public void setItems(List<LeaderboardItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vType) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_leaderboard, p, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        LeaderboardItem it = items.get(pos);
        h.name.setText(it.name);
        h.score.setText(String.valueOf(it.score));
        h.time.setText(it.timeSeconds + "s");

        boolean isMine = it.uid != null && it.uid.equals(myUid);
        h.delete.setVisibility(isMine ? View.VISIBLE : View.INVISIBLE);
        h.delete.setOnClickListener(v -> onDelete.onDelete(it));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, score, time;
        ImageButton delete;
        VH(View item) {
            super(item);
            name = item.findViewById(R.id.nameText);
            score = item.findViewById(R.id.scoreText);
            time  = item.findViewById(R.id.timeText);
            delete = item.findViewById(R.id.deleteBtn);
        }
    }
}
