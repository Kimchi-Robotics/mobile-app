package com.example.deliverybot.ui.reflow

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.deliverybot.R
import com.example.deliverybot.databinding.FragmentReflowBinding
import com.ortiz.touchview.OnTouchCoordinatesListener
import com.ortiz.touchview.OnTouchImageViewListener
import java.text.DecimalFormat


class ReflowFragment : Fragment() {

    private var _binding: FragmentReflowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val thisInstance = this

    private lateinit var _reflowViewModel: ReflowViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _reflowViewModel =
            ViewModelProvider(this).get(ReflowViewModel::class.java)
        _reflowViewModel.SetOriginalBipmap(createMutableBitmap(R.drawable.map))
        _reflowViewModel.SetRobotBitmap(createScaleBitmap(R.drawable.andy_no_background, 400, 400))

        // https://developer.android.com/topic/libraries/view-binding
        _binding = FragmentReflowBinding.inflate(layoutInflater)
        val root = binding.root

        // DecimalFormat rounds to 2 decimal places.
        val df = DecimalFormat("#.##")

        // Set the OnTouchImageViewListener which updates edit texts with zoom and scroll diagnostics.
        binding.imageSingle.setOnTouchImageViewListener(object : OnTouchImageViewListener {
            override fun onMove() {
                val point = binding.imageSingle.scrollPosition
                val rect = binding.imageSingle.zoomedRect
                val currentZoom = binding.imageSingle.currentZoom
                val isZoomed = binding.imageSingle.isZoomed
//                binding.scrollPosition.text = "x: " + df.format(point.x.toDouble()) + " y: " + df.format(point.y.toDouble())
//                binding.zoomedRect.text = ("left: " + df.format(rect.left.toDouble()) + " top: " + df.format(rect.top.toDouble())
//                        + "\nright: " + df.format(rect.right.toDouble()) + " bottom: " + df.format(rect.bottom.toDouble()))
//                binding.currentZoom.text = "getCurrentZoom(): $currentZoom isZoomed(): $isZoomed"
            }
        }
        )

        binding.imageSingle.setOnTouchCoordinatesListener(object: OnTouchCoordinatesListener {
            override fun onTouchCoordinate(view: View, event: MotionEvent, bitmapPoint: PointF) {
                if (event.getAction() == ACTION_DOWN && event.getDownTime() >= 500) {

                    _reflowViewModel.AddPointToPath(Pair(bitmapPoint.x, bitmapPoint.y))
                    binding.imageSingle.setImageBitmap(_reflowViewModel.GetBitmap())
                }
            }
        })

        binding.buttonPlay.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                thisInstance.Play()
            }

        })
        return root
    }

    fun Play() {
        Thread(Runnable {
            while(true) {
                Thread.sleep(300)
                _reflowViewModel.robotStep()
                val bitmap = _reflowViewModel.GetBitmapWithRobot()
                binding.imageSingle.setImageBitmap(bitmap)
            }
        }).start()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}