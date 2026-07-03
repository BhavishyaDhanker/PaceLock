package com.example.pacelock.PastRuns

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.pacelock.Data.RunView
import com.example.pacelock.R
import java.time.format.TextStyle
import java.util.Locale

class PastRunsAdapter(
    private var runList : List<RunView>
) : RecyclerView.Adapter<PastRunsAdapter.PastRunsViewHolder>() {


    class PastRunsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val monthText : TextView = itemView.findViewById(R.id.tvMonthText)
        val dateText : TextView = itemView.findViewById(R.id.tvDateText)
        val distance : TextView = itemView.findViewById(R.id.tvDistance)
        val duration : TextView = itemView.findViewById(R.id.tvDuration)
        val pace : TextView = itemView.findViewById(R.id.tvPace)
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PastRunsViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_run_history,
                parent,
                false
            )

        return PastRunsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PastRunsViewHolder,
        position: Int
    ) {

        val run = runList[position]

        val timestamp = run.timestamp
        val duration = run.durationSeconds
        val distance = run.distanceMeters

        val instant = java.time.Instant.ofEpochMilli(timestamp)
        val date = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()

        val day = date.dayOfMonth
        val month = date.monthValue       // 1-12
        val monthName = date.month.getDisplayName(
            TextStyle.SHORT,
            Locale.getDefault()
        )       // JUL
        val year = date.year


        val formattedDuration = String.format(
            Locale.getDefault(),
            "%d:%02d",
            duration/ 60, duration%60
        )

        val paceInSeconds = duration / (distance / 1000f)

        val paceMinutes = (paceInSeconds/60).toInt()
        val paceSeconds = (paceInSeconds%60).toInt()


        val formattedPace = String.format(
            Locale.getDefault(),
            "%d:%02d",
            paceMinutes, paceSeconds
            )


        holder.monthText.text = monthName
        holder.dateText.text = day.toString()
        holder.duration.text = formattedDuration
        holder.distance.text =
            if(distance < 1000f) String.format(
                Locale.getDefault(),
                "%.2f m",
                distance
            )
            else String.format(
                Locale.getDefault(),
                "%.2f km",
                distance / 1000f
        )
        holder.pace.text = if((distance < 10f )|| (duration < 10)) "--:--" else formattedPace
    }

    override fun getItemCount(): Int {
        return runList.size
    }


    fun updateRuns(newRuns : List<RunView>){
        runList = newRuns

        notifyDataSetChanged()
    }

}