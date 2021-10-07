package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.colley.android.R
import com.colley.android.adapter.IssueCommentsPagingAdapter
import com.colley.android.databinding.FragmentViewIssueBinding
import com.colley.android.model.Comment
import com.colley.android.model.Issue
import com.colley.android.model.Profile
import com.colley.android.repository.DatabaseRepository
import com.colley.android.view.dialog.CommentOnIssueBottomSheetDialogFragment
import com.colley.android.viewmodel.ViewIssueViewModel
import com.colley.android.factory.ViewModelFactory
import com.colley.android.view.dialog.IssueOptionsBottomSheetDialogFragment
import com.colley.android.wrapper.WrapContentLinearLayoutManager
import com.firebase.ui.auth.AuthUI
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
    CommentOnIssueBottomSheetDialogFragment.CommentListener,
    IssueOptionsBottomSheetDialogFragment.IssueOptionsDialogListener{

    private val args: ViewIssueFragmentArgs by navArgs()
    private var _binding: FragmentViewIssueBinding? = null
    private var issueUserId: String? = null
    private val binding get() = _binding
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private var commentsCount: Int = 0
    private var differenceCount: Int = 0
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var commentOnIssueSheetDialog: CommentOnIssueBottomSheetDialogFragment
    private var commentsAdapter: IssueCommentsPagingAdapter? = null
    private var manager: WrapContentLinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid
    private lateinit var issueOptionsDialog: IssueOptionsBottomSheetDialogFragment

    private val commentsCountValueEventListener = object : ValueEventListener {
        @SuppressLint("SetTextI18n")
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue(Int::class.java)

            if(count != null) {
                //set contributions count text
                binding?.contributionsTextView?.text = count.toString()
                differenceCount = count - commentsCount
            }
            //ignore changes due to adding value event listener to database first time or
            //initial paging load. we know this when current comments count is equal to the difference
            //between the previous count (initial count will be zero) and the current count
            if(differenceCount > 0 && count != differenceCount) {
                if(differenceCount == 1) {
                    binding?.newCommentNotificationTextView?.text = "^ $differenceCount new comment"
                } else {
                    binding?.newCommentNotificationTextView?.text = "^ $differenceCount new comments"
                }
                binding?.newCommentNotificationTextView?.visibility = VISIBLE
                return

            } else if(differenceCount > 0) {
                //acknowledge changes due to adding listener first time / initial paging load and
                //only when comments count increases, so that if initially, comments count was zero
                //and then increases, we also want to inform the user if they want to refresh,
                //that a new like has occurred after initial page load and adding value event
                //listener to database first time even though the difference between the current
                //comments count and the previous comments count equals (suggesting either initial page
                //load or adding value event listener to database first time, which in fact is not
                //the case since both events have already occurred)
                if(differenceCount == 1) {
                    binding?.newCommentNotificationTextView?.text = "$differenceCount comment"
                } else {
                    binding?.newCommentNotificationTextView?.text = "$differenceCount comments"
                }
                binding?.newCommentNotificationTextView?.visibility = VISIBLE
                return
            } else {
                //if no change in likes count or comments count decreases, it's not important to show it
                binding?.newCommentNotificationTextView?.visibility = INVISIBLE
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
            manager?.scrollToPosition(positionStart)
            binding?.newCommentNotificationTextView?.visibility = INVISIBLE
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
        val viewModel = ViewModelProvider(this,
            ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(ViewIssueViewModel::class.java)

        //get issue
        dbRef.child("issues").child(args.issueId).get().addOnSuccessListener { issueSnapShot ->

            val issue = issueSnapShot.getValue(Issue::class.java)
            //get the id of the user who raised the issue
            issueUserId = issue?.userId
            //set issue title, body and time stamp, these don't need to change
            binding?.issueTitleTextView?.text = issue?.title
            binding?.issueBodyTextView?.text = issue?.body
            binding?.issueTimeStampTextView?.text = issue?.timeStamp.toString()

            //get and set user photo
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
                            Glide.with(context).load(R.drawable.ic_profile)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(imageView)
                        } }
                    }

                }

            //get and set name and school
            dbRef.child("profiles").child(issue?.userId.toString()).get()
                .addOnSuccessListener { profileSnapShot ->
                    val profile = profileSnapShot.getValue<Profile>()
                    if (profile != null) {
                        binding?.userNameTextView?.text = profile.name
                        binding?.userSchoolTextView?.text = profile.school
                    }
                }

            //update book marked drawable icon based on whether user has book-marked this issue
            //or not
                dbRef.child("user-book_marked_issues").child(uid).get()
                    .addOnSuccessListener { dataSnapshot ->
                        binding?.bookMarkmageView?.isActivated =
                            dataSnapshot.getValue<ArrayList<String>>()?.contains(args.issueId) == true
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

        binding?.bookMarkmageView?.setOnClickListener { bookMarkImageView ->
            //register issue to user's list of book-marked issues on database
            dbRef.child("user-book_marked_issues").child(uid)
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        //retrieve the current list of saved posts at this location
                        var savedIssues = currentData.getValue<ArrayList<String>>()
                        //if no list is found and null is returned, create a new list
                        if (savedIssues == null) {
                            savedIssues = arrayListOf()
                        }
                        //if list does not already contain issue id, add it else remove it
                        if (!savedIssues.contains(args.issueId)) {
                            savedIssues.add(args.issueId)
                        } else {
                            savedIssues.remove(args.issueId)
                        }
                        currentData.value = savedIssues
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        if (error == null) {
                            //get the updated list
                            val updatedList = currentData?.getValue<ArrayList<String>>()
                            //if it contains issueId, toast saved
                            if (updatedList?.contains(args.issueId) == true) {
                                Toast.makeText(
                                    requireContext(),
                                    "BookMarked",
                                    Toast.LENGTH_SHORT).show()
                                //update book mark drawable icon
                                bookMarkImageView.isActivated = true
                            } else {
                                //Toast unsaved
                                Toast.makeText(
                                    requireContext(),
                                    "UnMarked",
                                    Toast.LENGTH_SHORT).show()
                                //update book mark drawable icon
                                bookMarkImageView.isActivated = false
                            }
                        }
                    }
                })
        }

        binding?.commentLinearLayout?.setOnClickListener {
            commentOnIssueSheetDialog = CommentOnIssueBottomSheetDialogFragment(
                requireContext(),
                requireView(),
                this)
            //pass issueId and the id of user who raise the issue as arguments to comment dialog
            commentOnIssueSheetDialog.arguments = bundleOf(
                "issueIdKey" to args.issueId, "issueUserIdKey" to issueUserId)
            commentOnIssueSheetDialog.show(parentFragmentManager, null)
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
                                currentData?.getValue(Int::class.java).toString()
                                    .removePrefix("-")
                        }

                    }
                )
        }

        binding?.moreImageView?.setOnClickListener {
            issueOptionsDialog = IssueOptionsBottomSheetDialogFragment(
                requireContext(),
                requireView(),
                this
            )
            issueOptionsDialog.arguments =
                bundleOf("issueIdKey" to args.issueId, "userIdKey" to issueUserId)
            issueOptionsDialog.show(parentFragmentManager, null)
        }

        //get a query reference to issue comments ordered by time code so that the most recent
        //comments appear first
        val commentsQuery = dbRef.child("issue-comments").child(args.issueId)
            .orderByChild("timeId")

        //initialize adapter
        commentsAdapter = IssueCommentsPagingAdapter(requireContext(), currentUser, this)

        //retrieve number of comments from database to be used for estimating the number of new
        //comments added since the user last refreshed
        getCommentsCount()

        //set recycler view layout manager
        manager =  WrapContentLinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false)
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = commentsAdapter

        //refresh adapter everytime refresh action is called on swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            commentsAdapter?.refresh()
            //reset posts count on refresh so that this fragment knows the correct database posts
            //count
            getCommentsCount()

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getIssueComments(commentsQuery).collectLatest {
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
                            "Error fetching comments! Retrying..",
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

    //to delete a comment from an issue
    override fun onItemLongCLicked(comment: Comment, view: View) {
        if (comment.commenterId == uid) {
            AlertDialog.Builder(requireContext())
                .setMessage("Delete comment?")
                .setPositiveButton("Yes") { dialog, which ->
                    //locate and delete comment from database only if the user is the owner of the comment
                    comment.commentId?.let { commentId -> dbRef.child("issue-comments").child(args.issueId)
                        .child(commentId).get().addOnSuccessListener { dataSnapshot ->
                            val commentSnap = dataSnapshot.getValue(Comment::class.java)
                            //if comment exists, i.e hasn't already been removed, delete and
                            //update count
                            if(commentSnap != null) {
                                dbRef.child("issue-comments").child(args.issueId)
                                    .child(commentId).setValue(null) { error, ref ->
                                        //if comment was successfully deleted, decrease comments count
                                        if (error == null) {
                                            dbRef.child("issues").child(args.issueId)
                                                .child("contributionsCount").runTransaction(object :
                                                    Transaction.Handler {
                                                    override fun doTransaction(currentData: MutableData):
                                                            Transaction.Result {
                                                        var count =
                                                            currentData.getValue(Int::class.java)
                                                        if (count != null) {
                                                            count--
                                                            currentData.value = count
                                                        }
                                                        return Transaction.success(currentData)
                                                    }

                                                    override fun onComplete(
                                                        error: DatabaseError?,
                                                        committed: Boolean,
                                                        currentData: DataSnapshot?
                                                    ) {
                                                        if (error == null) {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Deleted, Refresh",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }

                                                })
                                        }
                                    }
                            }
                        }
                         }
                    dialog.dismiss()
                }.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }.show()
        }

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
        dbRef.child("issues").child(args.issueId).child("contributionsCount")
            .addValueEventListener(commentsCountValueEventListener)

        //listener for endorsements count used to set endorsement count text
        dbRef.child("issues").child(args.issueId).child("endorsementsCount")
            .addValueEventListener(endorsementsCountValueEventListener)


    }

    override fun onStop() {
        super.onStop()
        //clear observers and listeners
        commentsAdapter?.unregisterAdapterDataObserver(commentsAdapterObserver)

        dbRef.child("issues").child(args.issueId).child("contributionsCount")
            .removeEventListener(commentsCountValueEventListener)

        dbRef.child("issues").child(args.issueId).child("endorsementsCount")
            .removeEventListener(endorsementsCountValueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    //after database update is completed, update ui
    override fun onComment(currentData: DataSnapshot?) {
        binding?.contributionsTextView?.text = currentData?.getValue(Int::class.java).toString()
    }

    override fun onDeleteIssue(issueId: String?) {
        findNavController().navigateUp()
        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onReportIssue(issueId: String?) {
        AlertDialog.Builder(requireContext())
            .setMessage("This issue has been flagged and will be reviewed")
            .setNegativeButton("Ok, dismiss") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }


}