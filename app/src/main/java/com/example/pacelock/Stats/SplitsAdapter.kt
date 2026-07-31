package com.example.pacelock.Stats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pacelock.Data.Split
import com.example.pacelock.R
import java.util.Locale

class SplitsAdapter(
    private var splits : List<Split>
) : RecyclerView.Adapter<SplitsAdapter.SplitsViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SplitsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_split,
                parent,
                false
            )
        return SplitsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SplitsViewHolder,
        position: Int
    ) {
        val split = splits[position]
        val time = split.seconds

        val formattedTime = String.format(
            Locale.getDefault(),
            "%d:%02d",
            time / 60, time % 60
        )


        val paceInSeconds = time / (1000f / 1000f)

        val paceMinutes = (paceInSeconds / 60).toInt()
        val paceSeconds = (paceInSeconds % 60).toInt()


        val formattedPace = String.format(
            Locale.getDefault(),
            "%d:%02d",
            paceMinutes, paceSeconds
        )

        val formattedDistance = if(split.distanceMeters >= 1000f) {split.distanceMeters/1000f}
            else {split.distanceMeters}

        holder.number.text = split.kilometerNo.toString()
        holder.time.text = formattedTime
        holder.pace.text = formattedPace
        holder.distance.text = formattedDistance.toString()



    }

    override fun getItemCount(): Int {
        return splits.size
    }

    fun updateRecyclerView(newSplits: List<Split>) {
        splits = newSplits
        notifyDataSetChanged()
    }

    class SplitsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val number : TextView = itemView.findViewById<TextView>(R.id.tvNumber)
        val time : TextView = itemView.findViewById<TextView>(R.id.tvTime)
        val distance : TextView = itemView.findViewById<TextView>(R.id.tvDistance)
        val pace : TextView = itemView.findViewById<TextView>(R.id.tvPace)
    }
}