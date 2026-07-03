package com.example.bloodbank.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.bloodbank.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filters)
        val btnApply = view.findViewById<MaterialButton>(R.id.btn_apply_filters)

        // Pre-select based on current filter state
        when (viewModel.uiState.value.activeFilter) {
            FeedFilter.ALL -> chipGroup.check(R.id.chip_all)
            FeedFilter.CRITICAL -> chipGroup.check(R.id.chip_critical)
            FeedFilter.URGENT -> chipGroup.check(R.id.chip_urgent)
            FeedFilter.MY_TYPE -> chipGroup.check(R.id.chip_my_type)
            FeedFilter.HOSPITALS -> chipGroup.clearCheck()
        }

        btnApply.setOnClickListener {
            val selectedFilter = when (chipGroup.checkedChipId) {
                R.id.chip_critical -> FeedFilter.CRITICAL
                R.id.chip_urgent -> FeedFilter.URGENT
                R.id.chip_my_type -> FeedFilter.MY_TYPE
                else -> FeedFilter.ALL
            }
            viewModel.setFilter(selectedFilter)
            dismiss()
        }
    }
}
