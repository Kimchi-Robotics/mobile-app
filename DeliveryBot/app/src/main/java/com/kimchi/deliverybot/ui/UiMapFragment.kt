package com.kimchi.deliverybot.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kimchi.deliverybot.R
import androidx.fragment.app.activityViewModels
import com.kimchi.deliverybot.utils.MapInfo
import com.kimchi.deliverybot.utils.Pose2D

class UiMapFragment: Fragment() {
    private lateinit var _originalBitmap: Bitmap

    private lateinit var _currentBitmap: Bitmap
    private lateinit var _robotBitmap: Bitmap

    private val _uiViewModel : UiViewModel by activityViewModels()
    private val _robotRadius = 30

    private var _mapInfo = MapInfo.empty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.ui_map_fragment, container, false)

        setOriginalBipmap(createMutableBitmap(R.drawable.map))
        setRobotBitmap(createScaleBitmap(R.drawable.robot_image, _robotRadius, _robotRadius))
        val bitmap = getBitmapWithRobot(Pose2D(0f, 0f, 0f))
        view.findViewById<com.ortiz.touchview.TouchImageView>(R.id.imageSingle).setImageBitmap(bitmap)

        _uiViewModel.pose.observe(viewLifecycleOwner) {
            val mapBitmap = getBitmapWithRobot(it)
            view.findViewById<com.ortiz.touchview.TouchImageView>(R.id.imageSingle).setImageBitmap(mapBitmap)
        }

        _uiViewModel.mapInfo.observe(viewLifecycleOwner) {
            _mapInfo = it

            setOriginalBipmap(it.bitmap)
        }

        return view
    }

    private fun setOriginalBipmap(originalBitmap: Bitmap){
        _originalBitmap = originalBitmap
        _currentBitmap = _originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun setRobotBitmap(robotBitmap: Bitmap) {
        _robotBitmap = robotBitmap
    }

    fun createMutableBitmap(drawableId: Int): Bitmap {
        var myBitmap = BitmapFactory.decodeResource(getResources(), drawableId)
        val mutableBitmap: Bitmap = myBitmap.copy(Bitmap.Config.ARGB_8888, true)
        return mutableBitmap
    }

    fun createScaleBitmap(drawableId: Int, newHeight: Int, newWidth: Int): Bitmap {
        var myBitmap = BitmapFactory.decodeResource(resources, drawableId)
        myBitmap = Bitmap.createScaledBitmap(myBitmap!!, newWidth, newHeight, false)
        return myBitmap
    }

    fun getBitmapWithRobot(pose: Pose2D): Bitmap {
        val cs = Bitmap.createBitmap(
            _currentBitmap.getWidth(),
            _currentBitmap.getHeight(),
            Bitmap.Config.ARGB_8888
        )

        var canvas = Canvas(cs)
        canvas.drawBitmap(_currentBitmap,0f,0f, null);
        val mapCoords = _mapInfo.WorldToBitmap(pose)
        canvas.drawBitmap(_robotBitmap, mapCoords.x - _robotRadius/2, mapCoords.y - _robotRadius/2, null);

        return cs
    }
}