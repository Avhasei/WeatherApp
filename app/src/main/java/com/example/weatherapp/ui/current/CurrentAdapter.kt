package com.example.weatherapp.ui.current

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R

class CurrentAdapter(private val mList: List<CurrentItemsViewModel>): RecyclerView.Adapter<CurrentAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.current_row_design, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = mList[position]

        holder.txtDay.text = itemsViewModel.txtDay

        if(itemsViewModel.textMain == "Rainy"){
            holder.imgIcon.setImageResource(R.drawable.rain3x)
        }else if(itemsViewModel.textMain == "Cloudy"){
            holder.imgIcon.setImageResource(R.drawable.partlysunny3x)
        }else{
            holder.imgIcon.setImageResource(R.drawable.clear3x)
        }
        holder.txtTemp.text = itemsViewModel.txtTemp

    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val txtDay: TextView = itemView.findViewById(R.id.txtDay)
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        val txtTemp: TextView = itemView.findViewById(R.id.txtTemp)
    }
}