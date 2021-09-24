package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.adapter.IssueCommentsPagingAdapter
import com.colley.android.databinding.FragmentViewIssueBinding
import com.colley.android.model.Comment
import com.colley.android.model.Issue
import com.colley.android.model.Profile
import com.colley.android.repository.DatabaseRepository
import com.colley.android.view.dialog.IssueCommentBottomSheetDialogFragment
import com.colley.android.viewmodel.ViewIssueViewModel
import com.colley.android.factory.ViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class ViewIssueFragment :
    Fragment(),
    IssueCommentsPagingAdapter.IssueCommentItemClickedListener,
    IssueCommentBottomSheetDialogFragment.CommentListener {

    private val args: ViewIssueFragmentArgs by navArgs()
    private var _binding: FragmentViewIssueBinding? = null
    private val binding get() = _binding
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var commentsCount: Int = 0
    private var differenceCount: Int = 0
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var commentSheetDialog: IssueCommentBottomSheetDialogFragment
    private var commentsAdapter: IssueCommentsPagingAdapter? = null
    private var manager: LinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid
    private val commentsCountValueEventListener = object : ValueEventListener {
        @SuppressLint("SetTextI18n")
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue(Int::class.java)
            if(count != null) {
                binding?.contributionsTextView?.text = count.toString()
                differenceCount = count - commentsCount
            }
            if(differenceCount > 0 && count != differenceCount) {
                if(differenceCount == 1) {
                    binding?.newCommentNotificationTextView?.text = "^ $differenceCount new post"
                } else {
                    binding?.newCommentNotificationTextView?.text = "^ $differenceCount new posts"
                }
                binding?.newCommentNotificationTextView?.visibility = VISIBLE
            }
        }

        override fun onCancelled(error: DatabaseError) {}
    }

    private val endorsementsCountValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue<Int>()
            if(count != null) {
                //remove minus sign when displaying count
                binding?.endorsementTextView?.text = count.toString().removePrefix("-")
            }
        }

        override fun onCancelled(error: DatabaseError) {} }

    //observer for adapter item changes
    private val commentsAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(0)
            binding?.newCommentNotificationTextView?.visibility = View.INVISIBLE
        }
    }

    //listener for recycler view scroll events
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy != 0) {
                binding?.newCommentNotificationTextView?.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentViewIssueBinding.inflate(inflater, container, false)
        recyclerView = binding?.issueCommentsRecyclerView!!
        swipeRefreshLayout = binding?.issueCommentsSwipeRefreshLayout!!
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!

        // get the view model
        val viewModel = ViewModelProvider(this, ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(ViewIssueViewModel::class.java)

        //get issue
        dbRef.child("issues").child(args.issueId).get().addOnSuccessListener { issueSnapShot ->

            val issue = issueSnapShot.getValue(Issue::class.java)
            //set issue title, body and time stamp, these don't need to change
            binding?.issueTitleTextView?.text = issue?.title
            binding?.issueBodyTextView?.text = issue?.body
            binding?.issueTimeStampTextView?.text = issue?.timeStamp.toString()

            //listener for user photo
            dbRef.child("photos").child(issue?.userId.toString()).get()
                .addOnSuccessListener { photoSnapShot ->
                    val photo = photoSnapShot.getValue(String::class.java)
                    if(photo != null) {
                        context?.let { context -> binding?.userImageView?.let {
                                imageView ->
                            Glide.with(context).load(photo).into(
                                imageView
                            )
                        } }
                    } else {
                        context?.let { context -> binding?.userImageView?.let {
                                imageView ->
                            Glide.with(context).load(R.drawable.ic_profile).into(
                                imageView
                            )
                        } }
                    }

                }

            //listener for profile to set name and school
            dbRef.child("profiles").child(issue?.userId.toString()).get()
                .addOnSuccessListener { profileSnapShot ->
                    val profile = profileSnapShot.getValue<Profile>()
                    if (profile != null) {
                        binding?.userNameTextView?.text = profile.name
                        binding?.userSchoolTextView?.text = profile.school
                    }
                }

            //view profile when clicked
            binding?.userImageView?.setOnClickListener {
                val action = issue?.userId?.let { it1 ->
                    ViewIssueFragmentDirections.actionViewIssueFragmentToUserInfoFragment(it1)
                }
                if (action != null) {
                    parentFragment?.findNavController()?.navigate(action)
                }
            }

            //view user profile when clicked
            binding?.userNameTextView?.setOnClickListener {
                val action = issue?.userId?.let { it1 ->
                    ViewIssueFragmentDirections.actionViewIssueFragmentToUserInfoFragment(it1)
                }
                if (action != null) {
                    parentFragment?.findNavController()?.navigate(action)
                }
            }

        }

        binding?.commentLinearLayout?.setOnClickListener {
            commentSheetDialog = IssueCommentBottomSheetDialogFragment(
                requireContext(),
                requireView(),
                this)
            commentSheetDialog.arguments = bundleOf("issueIdKey" to args.issueId)
            commentSheetDialog.show(parentFragmentManager, null)
        }

        binding?.endorseLinearLayout?.setOnClickListener {
            //update contributions count
            dbRef.child("issues").child(args.issueId).child("endorsementsCount")
                .runTransaction(
                    object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            //retrieve the current value of endorsement count at this location
                            var endorsementsCount = currentData.getValue<Int>()
                            if (endorsementsCount != null) {
                                //increase the count by 1
                                //Actually, decrease count by 1, this is used for orderByChild when
                                //returning issues query from firebase database
                                endorsementsCount--
                                //reassign the value to reflect the new update
                                currentData.value = endorsementsCount
                            }
                            //set database issue value to the new update
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(
                            error: DatabaseError?,
                            committed: Boolean,
                            currentData: DataSnapshot?
                        ) {
                            if (error == null && committed) {
                                Toast.makeText(requireContext(), "Endorsed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            //after database update is completed, update ui
                            binding?.endorsementTextView?.text =
                                currentData?.getValue(Int::class.java).toString().removePrefix("-")
                        }

                    }
                )
        }

        //get a query reference to issue comments ordered by time code so that the most recent
        //comments appear first
        val commentsQuery = dbRef.child("issue-comments").child(args.issueId).orderByChild("timeId")

        //initialize adapter
        commentsAdapter = IssueCommentsPagingAdapter(requireContext(), currentUser, this)

        //add scroll listener to remove notification text view when recycler view is scrolled
        recyclerView.addOnScrollListener(scrollListener)

        //retrieve number of comments from database to be used for estimating the number of new
        //comments added since the user last refreshed
        getCommentsCount()

        //set recycler view layout manager
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = commentsAdapter

        //refresh adapter everytime refresh action is called on swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            commentsAdapter?.refresh()
            //reset posts count on refresh so that this fragment knows the correct database posts count
            getCommentsCount()

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchIssueComments(commentsQuery).collectLatest {
                    pagingData ->
                commentsAdapter?.submitData(pagingData)

            }
        }

        //Perform some action every time data changes or when there is an error.
        viewLifecycleOwner.lifecycleScope.launch {
            commentsAdapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching posts! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding?.noCommentsLayout?.visibility = VISIBLE
                        commentsAdapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        //The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        if (commentsAdapter?.itemCount == 0) {
                            binding?.noCommentsLayout?.visibility = VISIBLE
                        } else {
                            binding?.noCommentsLayout?.visibility = GONE
                        }

                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        commentsAdapter?.retry()

                    }
                    is LoadState.Loading -> {
                        // The adapter has started to load an additional page
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        if (loadStates.append.endOfPaginationReached) {
                            // The adapter has finished loading all of the data set
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        }

    }
    //retrieve database comments count
    private fun getCommentsCount() {
        dbRef.child("issues").child(args.issueId).child("contributionsCount")
                .get().addOnSuccessListener {
                    snapShot ->
                if(snapShot.getValue(Int::class.java) != null) {
                    commentsCount = snapShot.getValue(Int::class.java)!!
                }
            }
    }


    override fun onItemClick(comment: Comment, view: View) {
        //expand comment
    }

    override fun onItemLongCLicked(comment: Comment, view: View) {
        //create option to delete
        //create option to respond
    }

    //view user profile
    override fun onUserClicked(userId: String, view: View) {
        val action = ViewIssueFragmentDirections.actionViewIssueFragmentToUserInfoFragment(userId)
        parentFragment?.findNavController()?.navigate(action)
    }

    override fun onStart() {
        super.onStart()

        //register observer to adapter to scroll to  position when new items are added
        commentsAdapter?.registerAdapterDataObserver(commentsAdapterObserver)

        //listener for contributions count used to set count text
        dbRef.child("issues").child(args.issueId)
            .child("contributionsCount").addValueEventListener(commentsCountValueEventListener)

        //listener for endorsements count used to set endorsement count text
        dbRef.child("issues").child(args.issueId)
            .child("endorsementsCount").addValueEventListener(endorsementsCountValueEventListener)


    }

    override fun onStop() {
        super.onStop()
        //clear observers and listeners
        commentsAdapter?.unregisterAdapterDataObserver(commentsAdapterObserver)
        recyclerView.removeOnScrollListener(scrollListener)

        dbRef.child("issues").child(args.issueId)
            .child("contributionsCount").removeEventListener(commentsCountValueEventListener)

        dbRef.child("issues").child(args.issueId)
            .child("endorsementsCount").removeEventListener(endorsementsCountValueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    //after database update is completed, update ui
    override fun onComment(currentData: DataSnapshot?) {
        binding?.contributionsTextView?.text = currentData?.getValue(Int::class.java).toString()
    }


}