package com.example.dronemr.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dronemr.databinding.FragmentSlideshowBinding
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.stream.GsdkStreamView

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /** Reference to the current drone stream server Peripheral. */
    private var streamServerRef: Ref<StreamServer>? = null
    /** Reference to the current drone live stream. */
    private var liveStreamRef: Ref<CameraLive>? = null
    /** Current drone live stream. */
    private var liveStream: CameraLive? = null
    // User interface:
    /** Video stream view. */
    //private val streamView by lazy { findViewById<GsdkStreamView>(R.id.stream_view) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(SlideshowViewModel::class.java)

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow
        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}