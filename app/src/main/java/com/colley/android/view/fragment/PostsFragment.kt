package com.colley.android.view.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.colley.android.R
import com.colley.android.adapter.*
import com.colley.android.databinding.FragmentPostsBinding
import com.colley.android.repository.DatabaseRepository
import com.colley.android.view.dialog.NewPostBottomSheetDialogFragment
import com.colley.android.view.dialog.CommentOnPostBottomSheetDialogFragment
import com.colley.android.viewmodel.PostsViewModel
import com.colley.android.factory.ViewModelFactory
import com.colley.android.view.dialog.PostBottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class PostsFragment : Fragment(),
    PostsPagingAdapter.PostPagingItemClickedListener,
    NewPostBottomSheetDialogFragment.NewPostListener,
    CommentOnPostBottomSheetDialogFragment.CommentListener,
    PostBottomSheetDialogFragment.PostDialogListener {

    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private var postsAdapter: PostsPagingAdapter? = null
    private var manager: LinearLayoutManager? = null
    private lateinit var recyclerView: RecyclerView
    private var postViewHolder: PostViewHolder? = null
    private var postsCount: Int = 0
    private var differenceCount: Int = 0
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var postDialog: PostBottomSheetDialogFragment
    private lateinit var sheetDialogCommentOn: CommentOnPostBottomSheetDialogFragment
    private val uid: String
        get() = currentUser.uid
    private val postsCountValueEventListener = object : ValueEventListener {
        @SuppressLint("SetTextI18n")
        override fun onDataChange(snapshot: DataSnapshot) {
            val count = snapshot.getValue(Int::class.java)
            if(count != null) {
                differenceCount = count - postsCount
            }
            if(differenceCount > 0 && count != differenceCount) {
                if(differenceCount == 1) {
                    binding.newPostNotificationTextView.text = "^ $differenceCount new post"
                } else {
                    binding.newPostNotificationTextView.text = "^ $differenceCount new posts"
                }
                binding.newPostNotificationTextView.visibility = VISIBLE
            }
        }


        override fun onCancelled(error: DatabaseError) {}
    }

    //observer for adapter item changes
    private val postsAdapterObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            manager?.scrollToPosition(0)
            binding.newPostNotificationTextView.visibility = View.INVISIBLE
        }
    }

    //listener for recycler view scroll events
    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (dy != 0) {
                binding.newPostNotificationTextView.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //fragment can participate in populating the options menu
        setHasOptionsMenu(true)

        //initialize Realtime Database
        dbRef = Firebase.database.reference

        //initialize authentication
        auth = Firebase.auth

        //initialize currentUser
        currentUser = auth.currentUser!!

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.posts_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_posts_menu_item -> {
                Toast.makeText(context, "Searching posts", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)
        recyclerView = binding.postRecyclerView
        swipeRefreshLayout = binding.swipeRefreshLayout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // get the view model
        val viewModel = ViewModelProvider(this,
            ViewModelFactory(owner = this, repository = DatabaseRepository()))
            .get(PostsViewModel::class.java)



        //get a query reference to posts
        val postsQuery = dbRef.child("posts").orderByChild("timeId")

        //initialize adapter
        postsAdapter = PostsPagingAdapter(requireContext(), currentUser, this)


        //retrieve number of posts from database to be used for estimating the number of new posts
        //added since the user last refreshed
        getPostsCount()

        //set recycler view layout manager
        manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager
        //initialize adapter
        recyclerView.adapter = postsAdapter

        //refresh adapter everytime refresh action is called on swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            postsAdapter?.refresh()
            //reset posts count on refresh so that this fragment knows the correct database posts
            //count
            getPostsCount()

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPosts(postsQuery).collectLatest {
                    pagingData ->
                postsAdapter?.submitData(pagingData)

            }
        }

        //Perform some action every time data changes or when there is an error.
        viewLifecycleOwner.lifecycleScope.launch {
            postsAdapter?.loadStateFlow?.collectLatest { loadStates ->

                when (loadStates.refresh) {
                    is LoadState.Error -> {

                        // The initial load failed. Call the retry() method
                        // in order to retry the load operation.
                        Toast.makeText(
                            context,
                            "Error fetching posts! Retrying..",
                            Toast.LENGTH_SHORT).show()
                        //display no posts available at the moment
                        binding.noPostsLayout.visibility = VISIBLE
                        postsAdapter?.retry()
                    }
                    is LoadState.Loading -> {
                        // The initial Load has begun
                        // ...
                        swipeRefreshLayout.isRefreshing = true
                    }
                    is LoadState.NotLoading -> {
                        //The previous load (either initial or additional) completed
                        swipeRefreshLayout.isRefreshing = false
                        if (postsAdapter?.itemCount == 0) {
                            binding.noPostsLayout.visibility = VISIBLE
                        } else {
                            binding.noPostsLayout.visibility = GONE
                        }

                    }
                }

                when (loadStates.append) {
                    is LoadState.Error -> {
                        // The additional load failed. Call the retry() method
                        // in order to retry the load operation.
                        postsAdapter?.retry()

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

    //retrieve database posts count
    private fun getPostsCount() {
        dbRef.child("postsCount").get().addOnSuccessListener {
                snapShot ->
            if(snapShot.getValue(Int::class.java) != null) {
                postsCount = snapShot.getValue(Int::class.java)!!
            }
        }
    }

    override fun onItemClick(postId: String, view: View, viewHolder: PostViewHolder) {
        //reference to viewHolder clicked
        postViewHolder = viewHolder
        Log.w("clickoverride", "$postId")
        postDialog = PostBottomSheetDialogFragment(
            requireContext(),
            requireView(),
            this
        )
        postDialog.arguments = bundleOf("postIdKey" to postId)
        postDialog.show(parentFragmentManager, null)
    }

    override fun onItemLongCLicked(postId: String, view: View, viewHolder: PostViewHolder) {

    }

    override fun onUserClicked(userId: String, view: View) {
        val action = HomeFragmentDirections.actionHomeFragmentToUserInfoFragment(userId)
        parentFragment?.findNavController()?.navigate(action)
    }

    override fun onCommentClicked(postId: String, view: View, viewHolder: PostViewHolder) {
        //reference to viewHolder clicked
        postViewHolder = viewHolder
        sheetDialogCommentOn = CommentOnPostBottomSheetDialogFragment(
            requireContext(),
            requireView(),
            this
        )
        sheetDialogCommentOn.arguments = bundleOf("postIdKey" to postId)
        sheetDialogCommentOn.show(parentFragmentManager, null)
    }

    override fun onLikeClicked(postId: String, view: View, viewHolder: PostViewHolder) {
        //reference to viewHolder clicked
        postViewHolder = viewHolder
        //Register like on database
        dbRef.child("post-likes").child(postId).child(uid)
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    //retrieve the current value of like at this location
                    var liked = currentData.getValue<Boolean>()
                    //compute new value of liked
                    liked = liked == null || liked == false
                    //if false set value at location to null else true
                     if (liked == false) {
                         currentData.value = null
                     } else {
                         currentData.value = true
                     }
                    //set database liked value to the new update
                    return Transaction.success(currentData)
                }

                //on successful entry, update likes count
                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    //get current value of liked
                    val liked = currentData?.getValue(Boolean::class.java)

                    if (error != null) {
                        Toast.makeText(
                            context,
                            "Unable to write like to database",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        //update likes count
                        dbRef.child("posts").child(postId).child("likesCount")
                            .runTransaction(
                                object : Transaction.Handler {
                                    override fun doTransaction(currentData: MutableData):
                                            Transaction.Result {
                                        //retrieve the current value of count at this location
                                        var count = currentData.getValue<Int>()
                                        if (count == null) {
                                            count = 1
                                        }

                                        //if liked, increase count by 1 else decrease by 1
                                        if (liked == true) {
                                            count++
                                        } else {
                                            count--
                                        }
                                            currentData.value = count
                                        //set database count value to the new update
                                        return Transaction.success(currentData)
                                    }

                                    //after successfully updating likes count on database, update ui
                                    @SuppressLint("SetTextI18n")
                                    override fun onComplete(
                                        error: DatabaseError?,
                                        committed: Boolean,
                                        currentData: DataSnapshot?
                                    ) {
                                        updateLikeCountTextView(currentData, liked)
                                    }
                                }
                            )
                    }
                }

            }
        )
    }

    override fun onSaveClicked(postId: String, it: View, viewHolder: PostViewHolder) {
        //reference to viewHolder clicked
        postViewHolder = viewHolder
        //Register like on database
        dbRef.child("user-saved_posts").child(uid)
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    //retrieve the current list of saved posts at this location
                    val savedPosts = currentData.getValue<ArrayList<String>>()
                    //if no list is found and null is returned, create a new list, add post id to it
                    if (savedPosts == null) {
                        //update database
                        currentData.value = arrayListOf(postId)
                        return Transaction.success(currentData)
                    } else {
                        //if list does not already contain post id, add it
                        if (!savedPosts.contains(postId)) {
                            savedPosts.add(postId)
                            //update database
                            currentData.value = savedPosts
                            return Transaction.success(currentData)
                        } else {
                            //if list already contains post, return unchanged list
                            return Transaction.success(currentData)
                        }
                    }
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    //on successful save, toast
                    if (error == null) {
                        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                        //update savedPostTextView start drawable icon
                        postViewHolder?.itemBinding?.savePostTextView?.isActivated = true
                    }
                }

            }
            )
    }

    override fun onStart() {
        super.onStart()
        //add value event listener for posts count
        dbRef.child("postsCount").addValueEventListener(postsCountValueEventListener)

        //register observer to adapter to scroll to  position when new items are added
        postsAdapter?.registerAdapterDataObserver(postsAdapterObserver)

        //add scroll listener to remove notification text view when recycler view is scrolled
        recyclerView.addOnScrollListener(scrollListener)

    }

    override fun onStop() {
        super.onStop()
        //clear observers and listeners
        dbRef.child("postsCount").removeEventListener(postsCountValueEventListener)
        postsAdapter?.unregisterAdapterDataObserver(postsAdapterObserver)
        recyclerView.removeOnScrollListener(scrollListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun refreshPosts() {
        postsAdapter?.refresh()
    }

    //update view holder ui to display updated comment count
    //interface from comment listener
    override fun onComment(currentData: DataSnapshot?) {
        if (postViewHolder != null) {
            updateCommentCountTextView(currentData)
        }
    }

    //update view holder ui to display updated comments count
    //interface from post dialog listener
    override fun onCommented(currentData: DataSnapshot?) {
        if (postViewHolder != null) {
            updateCommentCountTextView(currentData)
        }
    }

    //update view holder ui to display updated like count
    //interface from post dialog listener
    override fun onLiked(currentData: DataSnapshot?, liked: Boolean?) {
        updateLikeCountTextView(currentData, liked)
    }

    @SuppressLint("SetTextI18n")
    private fun updateLikeCountTextView(currentData: DataSnapshot?, liked: Boolean?) {
        when (currentData?.getValue(Int::class.java)) {
            0 -> postViewHolder?.itemBinding?.likeCountTextView?.visibility = GONE
            1 -> {
                postViewHolder?.itemBinding?.likeCountTextView?.visibility = VISIBLE
                postViewHolder?.itemBinding?.likeCountTextView?.text =
                    "${currentData.getValue(Int::class.java).toString()} like"
            }
            else -> {
                postViewHolder?.itemBinding?.likeCountTextView?.visibility = VISIBLE
                postViewHolder?.itemBinding?.likeCountTextView?.text =
                    "${currentData?.getValue(Int::class.java).toString()} likes"

            }
        }
        //update likeTextView start drawable depending on the value
        //of liked
        postViewHolder?.itemBinding?.likeTextView?.isActivated =
            !(liked == null || liked == false)
    }

    @SuppressLint("SetTextI18n")
    private fun updateCommentCountTextView(currentData: DataSnapshot?) {
        when (currentData?.getValue(Int::class.java)) {
            0 -> postViewHolder?.itemBinding?.commentCountTextView?.visibility = GONE
            1 -> {
                postViewHolder?.itemBinding?.commentCountTextView?.visibility = VISIBLE
                postViewHolder?.itemBinding?.commentCountTextView?.text =
                    "${currentData.getValue(Int::class.java).toString()} comment"
            }
            else -> {
                postViewHolder?.itemBinding?.commentCountTextView?.visibility = VISIBLE
                postViewHolder?.itemBinding?.commentCountTextView?.text =
                    "${currentData?.getValue(Int::class.java).toString()} comments"
            }
        }
    }

}