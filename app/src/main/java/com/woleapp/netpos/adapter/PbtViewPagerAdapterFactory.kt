package com.woleapp.netpos.adapter

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import dagger.assisted.AssistedFactory

@AssistedFactory
interface PbtViewPagerAdapterFactory {
    fun createViewPagerAdapter(
        fm: FragmentManager,
        lifecycle: Lifecycle
    ): PbtViewPagerAdapter
}
