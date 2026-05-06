package com.hostelops.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.hostelops.R
import com.hostelops.databinding.ItemUserBinding
import com.hostelops.models.User

class UserAdapter(
    private var users: List<User>,
    private val onDeleteClick: (User) -> Unit,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.binding.tvUserName.text = user.name.ifEmpty { "No Name" }
        holder.binding.tvUserInfo.text = "${user.email}\n${if (user.role == "STUDENT") user.rollNo else user.phone}"

        holder.binding.ivUserAvatar.load(user.photoUrl ?: R.drawable.ic_profile_placeholder) {
            transformations(CircleCropTransformation())
        }

        holder.binding.btnDeleteUser.setOnClickListener { onDeleteClick(user) }
        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    override fun getItemCount() = users.size

    fun updateList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }
}
