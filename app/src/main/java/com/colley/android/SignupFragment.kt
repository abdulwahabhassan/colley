package com.colley.android

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.colley.android.databinding.FragmentHomeBinding
import com.colley.android.databinding.FragmentSignupBinding


class SignupFragment : Fragment() {

    val args: SignupFragmentArgs by navArgs()
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val schoolName = args.schoolName
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}