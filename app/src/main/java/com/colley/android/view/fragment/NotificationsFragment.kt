package com.colley.android.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.colley.android.adapter.NotificationsFragmentRecyclerAdapter
import com.colley.android.databinding.FragmentNotificationsBinding
import com.colley.android.model.DummyData
import com.colley.android.model.Notification


class NotificationsFragment : Fragment(), NotificationsFragmentRecyclerAdapter.ItemClickedListener {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = binding.notificationsRecyclerView
        val recyclerViewAdapter = NotificationsFragmentRecyclerAdapter(this)
        recyclerViewAdapter.setList(DummyData.getListOfNotifications())
        recyclerView.adapter = recyclerViewAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onItemClick(notification: Notification) {

    }

}