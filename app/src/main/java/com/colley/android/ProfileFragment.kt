package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.databinding.FragmentProfileBinding
import com.colley.android.model.DummyData

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(requireContext()).load(DummyData.getListOfPosts()[2].userPhoto).into(binding.profilePhotoImageView)
        Glide.with(requireContext()).load("https://rebrand.ly/7iock37").into(binding.photoOne)
        Glide.with(requireContext()).load("https://rebrand.ly/0a9d35").into(binding.photoTwo)
        Glide.with(requireContext()).load("https://rebrand.ly/3af2ivu").into(binding.photoThree)

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}