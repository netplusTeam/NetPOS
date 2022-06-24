package com.woleapp.netpos.ui.fragments

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.netpluspay.netpossdk.NetPosSdk
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.databinding.FragmentReprintBinding
import com.woleapp.netpos.model.GetEodFromNewServiceModel
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.HISTORY_ACTION_REPRINT
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class ReprintFragment : BaseFragment() {
    private lateinit var binding: FragmentReprintBinding
    private lateinit var adapter: ServiceAdapter
    private val stormApiService = StormApiClient.getStormApiLoginInstance()
    private var compositeDisposable = CompositeDisposable()
    private lateinit var endOfDayProgressDialog: ProgressDialog
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReprintBinding.inflate(inflater, container, false)
        endOfDayProgressDialog = ProgressDialog(requireContext()).apply {
            this.setCancelable(false)
            this.setMessage("Please wait...")
            this.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel") { dialog, _ ->
                compositeDisposable.clear()
                dialog.cancel()
            }
        }
        adapter = ServiceAdapter {
            if (it.id == 0) {
//                endOfDayProgressDialog.show()
                val parameters = GetEodFromNewServiceModel(
                    NetPosTerminalConfig.getTerminalId(),
                    page = 1,
                    pageSize = 20
                )
                addFragmentWithoutRemove(TransactionHistoryFragment.newInstance(action = HISTORY_ACTION_REPRINT))
            }
//            else if (it.id == 1)
//                printAllDialog()
//            else  if (it.id == 2)
//                printAllDialog()
//            else if (it.id == 3)
//                loadCapk()
//            else if (it.id == 4)
//                getThem()
        }
        return binding.root
    }

    private fun getThem() {
        Timber.e(NetPosSdk.getAids()?.size.toString())
        Timber.e(NetPosSdk.getCapks()?.size.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvReprint.layoutManager = GridLayoutManager(context, 2)
        binding.rvReprint.adapter = adapter
        setService()
    }

    private fun setService() {
        val listOfService = ArrayList<Service>()
            .apply {
                add(Service(0, "Reprint One Transaction", R.drawable.ic_print_one))
                add(Service(1, "Reprint All Transactions", R.drawable.ic_print_all))
            }
        adapter.submitList(listOfService)
    }
}
