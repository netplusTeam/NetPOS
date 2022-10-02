package com.woleapp.netpos.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.PbtViewPagerAdapter
import com.woleapp.netpos.databinding.FragmentZenithPayByTransferTransactionPageBinding
import com.woleapp.netpos.ui.fragments.pbt.GetLastTransactionPage
import com.woleapp.netpos.ui.fragments.pbt.ViewAllPBTTransactions
import com.woleapp.netpos.viewmodels.PayByZenithViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ZenithPayByTransferTransactionPage : Fragment() {
    private lateinit var binding: FragmentZenithPayByTransferTransactionPageBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPagerDestinationTexts: Array<String>

    private lateinit var viewPagerAdapter: PbtViewPagerAdapter
    private var screensToPassToViewPager: ArrayList<Fragment> = arrayListOf()
    private val lastTransactionPage: GetLastTransactionPage = GetLastTransactionPage()
    private val allTransactionPage: ViewAllPBTTransactions = ViewAllPBTTransactions()
    private val viewModel by activityViewModels<PayByZenithViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_zenith_pay_by_transfer_transaction_page,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getAllTransaction()
        screensToPassToViewPager = arrayListOf(lastTransactionPage, allTransactionPage)
        viewPagerDestinationTexts =
            resources.getStringArray(R.array.viewPagerDestinationsTabLayoutTexts)
        tabLayout = binding.pageTabLayout
        viewPagerAdapter = PbtViewPagerAdapter(parentFragmentManager, lifecycle)
        viewPagerAdapter.setScreens(screensToPassToViewPager)
        viewPager = binding.pageViewPager.apply {
            isUserInputEnabled = false
            adapter = viewPagerAdapter
        }
        // Setup the viewpager and the tab layout with Tab layout mediator
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = viewPagerDestinationTexts[position]
        }.attach()
        initViews()
    }

    private fun initViews() {
        with(binding) {
            viewPager = pageViewPager
            tabLayout = pageTabLayout
        }
    }
}
