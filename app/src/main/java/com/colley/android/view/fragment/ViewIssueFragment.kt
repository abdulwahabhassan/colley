package com.colley.android.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.colley.android.R
import com.colley.android.adapter.IssuesCommentsRecyclerAdapter
import com.colley.android.databinding.FragmentViewIssueBinding
import com.colley.android.model.Comment
import com.colley.android.model.Issue
import com.colley.android.model.Profile
import com.colley.android.view.dialog.IssueCommentBottomSheetDialogFragment
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.ObservableSnapshotArray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase


class ViewIssueFragment :
    Fragment(),
    IssuesCommentsRecyclerAdapter.ItemClickedListener,
    IssuesCommentsRecyclerAdapter.DataChangedListener {

    private val args: ViewIssueFragmentArgs by navArgs()
    private var _binding: FragmentViewIssueBinding? = null
    private val binding get() = _binding
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var recyclerView: RecyclerView
    private lateinit var commentSheetDialog: IssueCommentBottomSheetDialogFragment
    private var issue: Issue? = null
    private var adapter: IssuesCommentsRecyclerAdapter? = null
    private var manager: LinearLayoutManager? = null
    private val uid: String
        get() = currentUser.uid

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
//    }
//
//    //since we have set hasOptionsMenu to true, our fragment can now override this call to allow us
//    //modify the menu
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        menu.clear()
////        //this inflates a new menu
////        inflater.inflate(R.menu.group_message_menu, menu)
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
////        return when (item.itemId) {
////            R.id.search_issues_menu_item -> {
////                Toast.makeText(context, "Searching issues", Toast.LENGTH_LONG).show()
////                true
////            }
////            else -> super.onOptionsItemSelected(item)
////        }
//        return true
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentViewIssueBinding.inflate(inflater, container, false)
        recyclerView = binding?.issuesCommentsRecyclerView!!
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

        //get a query reference to issue comments //order by time stamp
        val commentsRef = dbRef.child("issues").child(args.issueId)
            .child("comments").orderByChild("commentTimeStamp")

        //the FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        //build an options to configure adapter. setQuery takes firebase query to listen to and a
        //model class to which snapShots should be parsed
        val options = FirebaseRecyclerOptions.Builder<Comment>()
            .setQuery(commentsRef, Comment::class.java)
            .build()

        //initialize issue comments adapter
        adapter = IssuesCommentsRecyclerAdapter(options, currentUser,this, this, requireContext(),)
        manager = LinearLayoutManager(requireContext())
        //reversing and stacking is actually counterintuitive as used in this scenario, the purpose
        //of the manipulation is such that most recent items appear at the top since firebase does
        //not provide a method to sort queries in descending order
        manager?.reverseLayout = true
        manager?.stackFromEnd = true
        recyclerView.layoutManager = manager
        recyclerView.adapter = adapter
        adapter?.startListening()

        dbRef.child("issues").child(args.issueId).addValueEventListener(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    issue = snapshot.getValue<Issue>()
                    if(issue != null) {

                        //listener for contrbutions count used to set count text
                        dbRef.child("issues").child(args.issueId).child("contributionsCount").addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val count = snapshot.getValue<Int>()
                                    if(count != null) {
                                        binding?.contributionsTextView?.text = count.toString()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            }
                        )

                        //listener for endorsement counts used to set endorsement count text
                        dbRef.child("issues").child(args.issueId).child("endorsementsCount").addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val count = snapshot.getValue<Int>()
                                    if(count != null) {
                                        binding?.endorsementTextView?.text = count.toString()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {} }
                        )

                        //set issue title, body and time stamp, these don't need to change
                        binding?.issueTitleTextView?.text = issue?.title
                        binding?.issueBodyTextView?.text = issue?.body
                        binding?.issueTimeStampTextView?.text = issue?.timeStamp.toString()

                        //listener for user photo
                        dbRef.child("photos").child(issue?.userId.toString())
                            .addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val photo = snapshot.getValue<String>()
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

                                override fun onCancelled(error: DatabaseError) {}
                            }
                        )

                        //listener for profile to set name and school
                        dbRef.child("profiles").child(issue?.userId.toString()).addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val profile = snapshot.getValue<Profile>()
                                    if (profile != null) {
                                        binding?.userNameTextView?.text = profile.name
                                        binding?.userSchoolTextView?.text = profile.school
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            }
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }
        )

        binding?.commentLinearLayout?.setOnClickListener {
            commentSheetDialog = IssueCommentBottomSheetDialogFragment(requireContext(), requireView())
            commentSheetDialog.arguments = bundleOf("issueIdKey" to args.issueId)
            commentSheetDialog.show(parentFragmentManager, null)
        }

        binding?.endorseLinearLayout?.setOnClickListener {
            //update contributions count
            dbRef.child("issues").child(args.issueId).child("endorsementsCount").runTransaction(
                object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        //retrieve the current value of endorsement count at this location
                        var endorsementsCount = currentData.getValue<Int>()
                        if (endorsementsCount != null) {
                            //increase the count by 1
                            endorsementsCount++
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
                            Toast.makeText(requireContext(), "Endorsed", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            )
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

    override fun onResume() {
        super.onResume()
        adapter?.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }


    override fun onDestroy() {
        super.onDestroy()
        adapter?.stopListening()
        _binding = null
    }

    override fun onDataAvailable(snapshotArray: ObservableSnapshotArray<Comment>) {
        //dismiss progress bar once snapshot is available
        binding?.issuesCommentProgressBar?.visibility = GONE

        //show that there are no comments if snapshot is empty else hide view
        //show recycler view if snapshot is not empty else hide
        if (snapshotArray.isEmpty()) {
            binding?.noCommentsLayout?.visibility = VISIBLE
        } else {
            binding?.noCommentsLayout?.visibility = GONE
            binding?.issuesCommentsRecyclerView?.visibility = VISIBLE
        }
    }


}