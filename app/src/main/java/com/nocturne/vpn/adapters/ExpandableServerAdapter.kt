package com.nocturne.vpn.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nocturnevpn.R
import com.nocturne.vpn.models.Server
import com.nocturne.vpn.models.ServerGroup

class ExpandableServerAdapter(
    private var serverGroups: List<ServerGroup>,
    private val onServerSelected: (Server) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_SERVER = 1
    }

    override fun getItemViewType(position: Int): Int {
        var itemCount = 0
        for (group in serverGroups) {
            if (position == itemCount) {
                return VIEW_TYPE_GROUP
            }
            itemCount++
            if (group.isExpanded) {
                if (position < itemCount + group.servers.size) {
                    return VIEW_TYPE_SERVER
                }
                itemCount += group.servers.size
            }
        }
        return VIEW_TYPE_GROUP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_GROUP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_server_group, parent, false)
                GroupViewHolder(view)
            }
            VIEW_TYPE_SERVER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_server, parent, false)
                ServerViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupViewHolder -> {
                val group = getGroupForPosition(position)
                holder.bind(group)
            }
            is ServerViewHolder -> {
                val server = getServerForPosition(position)
                holder.bind(server)
            }
        }
    }

    override fun getItemCount(): Int {
        var count = serverGroups.size
        for (group in serverGroups) {
            if (group.isExpanded) {
                count += group.servers.size
            }
        }
        return count
    }

    private fun getGroupForPosition(position: Int): ServerGroup {
        var itemCount = 0
        for (group in serverGroups) {
            if (position == itemCount) {
                return group
            }
            itemCount++
            if (group.isExpanded) {
                itemCount += group.servers.size
            }
        }
        throw IllegalArgumentException("Invalid position for group")
    }

    private fun getServerForPosition(position: Int): Server {
        var itemCount = 0
        for (group in serverGroups) {
            itemCount++
            if (group.isExpanded) {
                if (position < itemCount + group.servers.size) {
                    return group.servers[position - itemCount]
                }
                itemCount += group.servers.size
            }
        }
        throw IllegalArgumentException("Invalid position for server")
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCountryName: TextView = itemView.findViewById(R.id.tvCountryName)
        private val ivCountryFlag: ImageView = itemView.findViewById(R.id.ivCountryFlag)
        private val ivExpand: ImageView = itemView.findViewById(R.id.ivExpand)

        fun bind(group: ServerGroup) {
            tvCountryName.text = group.countryName
            // TODO: Load country flag using country code
            ivExpand.rotation = if (group.isExpanded) 180f else 0f

            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Collapse all other groups first
                    serverGroups.forEach { otherGroup ->
                        if (otherGroup != group) {
                            otherGroup.isExpanded = false
                        }
                    }
                    // Toggle the clicked group
                    group.isExpanded = !group.isExpanded
                    notifyDataSetChanged()
                }
            }
        }
    }

    inner class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvServerName: TextView = itemView.findViewById(R.id.tvServerName)
        private val tvServerDetails: TextView = itemView.findViewById(R.id.tvServerDetails)
        private val tvPing: TextView = itemView.findViewById(R.id.tvPing)

        fun bind(server: Server) {
            tvServerName.text = server.getName()
            tvServerDetails.text = "${server.getProtocol()} • ${server.getIp()}:${server.getPort()}"
            tvPing.text = "${server.getPing()}ms"

            itemView.setOnClickListener {
                onServerSelected(server)
            }
        }
    }

    fun updateData(newGroups: List<ServerGroup>) {
        serverGroups = newGroups
        notifyDataSetChanged()
    }
} 