package com.colley.android.glide

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.colley.android.glide.ProgressAppGlideModule

//custom class for glide image loader with which to indicate progress in time
class GlideImageLoader(imageView: ImageView?, progressBar: ProgressBar?) {
    private val mImageView: ImageView? = imageView
    private val mProgressBar: ProgressBar? = progressBar
    fun load(url: String?, options: RequestOptions?) {
        if (url == null || options == null) return
        onConnecting()

        //set Listener & start
        ProgressAppGlideModule.expect(url, object : ProgressAppGlideModule.UIonProgressListener {
            override fun onProgress(bytesRead: Long, expectedLength: Long) {
                mProgressBar?.progress = (100 * bytesRead / expectedLength).toInt()
            }

            override val granularityPercentage: Float
                get() = 1.0f
        })
        //Get Image
        if (mImageView != null) {
            Glide.with(mImageView.context)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(options.skipMemoryCache(true))
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        ProgressAppGlideModule.forget(url)
                        onFinished()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable?>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        ProgressAppGlideModule.forget(url)
                        onFinished()
                        return false
                    }

                })
                .into(mImageView)
        }
    }

    private fun onConnecting() {
        mProgressBar?.visibility = View.VISIBLE
    }

    private fun onFinished() {
        if (mProgressBar != null && mImageView != null) {
            mProgressBar.visibility = View.GONE
            mImageView.visibility = View.VISIBLE
        }
    }

}