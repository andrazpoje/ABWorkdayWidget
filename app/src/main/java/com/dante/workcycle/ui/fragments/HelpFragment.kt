package com.dante.workcycle.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dante.workcycle.R
import com.dante.workcycle.core.ui.applySystemBarsBottomInsetAsPadding
import com.dante.workcycle.core.ui.applySystemBarsHorizontalInsetAsPadding
import com.dante.workcycle.data.prefs.LaunchPrefs
import com.dante.workcycle.databinding.FragmentHelpBinding
import com.dante.workcycle.ui.help.HelpSectionAdapter
import com.dante.workcycle.ui.help.HelpSectionItem

class HelpFragment : Fragment(R.layout.fragment_help) {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    private val isOnboarding: Boolean
        get() = arguments?.getBoolean(ARG_IS_ONBOARDING, false) == true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHelpBinding.bind(view)

        binding.helpScrollView.applySystemBarsBottomInsetAsPadding()
        binding.helpContentContainer.applySystemBarsHorizontalInsetAsPadding()
        binding.helpBottomBar.applySystemBarsBottomInsetAsPadding()

        setupHelpList()
        setupOnboardingUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHelpList() {
        val items = listOf(
            HelpSectionItem(
                iconRes = R.drawable.ic_help_start_24,
                titleRes = R.string.help_getting_started_title,
                contentRes = R.string.help_getting_started_content,
                isExpanded = true
            ),
            HelpSectionItem(
                iconRes = R.drawable.ic_help_cycle_24,
                titleRes = R.string.help_cycle_logic_title,
                contentRes = R.string.help_cycle_logic_content
            ),
            HelpSectionItem(
                iconRes = R.drawable.ic_help_assignment_24,
                titleRes = R.string.help_assignments_title,
                contentRes = R.string.help_assignments_content
            ),
            HelpSectionItem(
                iconRes = R.drawable.ic_help_preview_24,
                titleRes = R.string.help_preview_title,
                contentRes = R.string.help_preview_content
            ),
            HelpSectionItem(
                iconRes = R.drawable.ic_help_calendar_24,
                titleRes = R.string.help_calendar_title,
                contentRes = R.string.help_calendar_content
            ),
            HelpSectionItem(
                iconRes = R.drawable.ic_help_widget_24,
                titleRes = R.string.help_widget_title,
                contentRes = R.string.help_widget_content
            ),
            HelpSectionItem(
                iconRes = R.drawable.ic_help_tips_24,
                titleRes = R.string.help_tips_title,
                contentRes = R.string.help_tips_content
            )
        )

        binding.helpRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.helpRecyclerView.adapter = HelpSectionAdapter(items)
        binding.helpRecyclerView.setHasFixedSize(false)
    }

    private fun setupOnboardingUi() {
        if (!isOnboarding) {
            binding.onboardingIntroCard.visibility = View.GONE
            binding.helpBottomBar.visibility = View.GONE
            return
        }

        binding.onboardingIntroCard.visibility = View.VISIBLE
        binding.helpBottomBar.visibility = View.VISIBLE

        binding.buttonContinueHelp.setOnClickListener {
            val launchPrefs = LaunchPrefs(requireContext())
            launchPrefs.setOnboardingCompleted(true)
            launchPrefs.markWhatsNewSeen()

            val navController = findNavController()
            runCatching {
                navController.navigate(R.id.homeFragment)
            }.onFailure {
                navController.popBackStack()
            }
        }
    }

    companion object {
        const val ARG_IS_ONBOARDING = "isOnboarding"

        fun onboardingArgs(): Bundle = bundleOf(ARG_IS_ONBOARDING to true)
    }
}