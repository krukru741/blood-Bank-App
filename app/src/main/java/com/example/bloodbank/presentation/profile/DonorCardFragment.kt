package com.example.bloodbank.presentation.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bloodbank.databinding.FragmentDonorCardBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DonorCardFragment
 *
 * Displays a premium digital donor ID card with:
 * - Profile photo, name, blood type
 * - Unique Donor ID (BBANK-000001)
 * - Registration date, status
 * - Auto-generated QR code (offline, no Google API needed)
 * - Share as image via Android share sheet
 */
@AndroidEntryPoint
class DonorCardFragment : Fragment() {

    private var _binding: FragmentDonorCardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DonorCardViewModel by viewModels()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnShareCard.setOnClickListener {
            shareCardAsImage()
        }

        observeUser()
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.user.collect { user ->
                    if (user == null) return@collect

                    // Profile photo
                    if (user.profilePhotoUrl.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(user.profilePhotoUrl)
                            .circleCrop()
                            .into(binding.ivCardPhoto)
                    }

                    // Text fields
                    binding.tvCardName.text = user.displayName.ifEmpty { user.email }
                    binding.tvCardBloodType.text = user.bloodType.label
                    binding.tvCardDonorId.text = user.donorId.ifEmpty { "Pending..." }

                    val regDate = user.donorVerificationDate ?: user.createdAt
                    binding.tvCardRegDate.text = dateFormatter.format(Date(regDate))
                    binding.tvCardStatus.text = if (user.isAvailableToDonate) "✅ Active Donor" else "⏸️ Unavailable"

                    // Generate QR Code
                    val qrContent = buildString {
                        append("DONORCARD|")
                        append("ID:${user.donorId}|")
                        append("NAME:${user.displayName}|")
                        append("BLOOD:${user.bloodType.label}|")
                        append("DATE:${dateFormatter.format(Date(regDate))}")
                    }
                    binding.ivQrCode.setImageBitmap(generateQrCode(qrContent))
                }
            }
        }
    }

    private fun generateQrCode(content: String): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun shareCardAsImage() {
        try {
            // Capture the card view to a bitmap
            val cardBitmap = binding.donorCardContainer.drawToBitmap()

            // Save to cache
            val cachePath = File(requireContext().cacheDir, "donor_card")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "donor_card.png")
            FileOutputStream(imageFile).use { out ->
                cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Get content URI via FileProvider
            val contentUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )

            // Launch share sheet
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "My Blood Bank App Verified Donor Card 🩸")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Donor Card"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not share card: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
