package com.woleapp.netpos.ui.fragments

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.netpluspay.netpossdk.NetPosSdk
import com.woleapp.netpos.R
import com.woleapp.netpos.adapter.ServiceAdapter
import com.woleapp.netpos.database.AppDatabase
import com.woleapp.netpos.databinding.FragmentReprintBinding
import com.woleapp.netpos.model.GetEodFromNewServiceModel
import com.woleapp.netpos.model.Service
import com.woleapp.netpos.network.StormApiClient
import com.woleapp.netpos.nibss.NetPosTerminalConfig
import com.woleapp.netpos.util.HISTORY_ACTION_REPRINT
import com.woleapp.netpos.util.ModelMapper.mapRowToTransactionResponse
import com.woleapp.netpos.util.disposeWith
import com.woleapp.netpos.viewmodels.NetPosViewModelFactories
import com.woleapp.netpos.viewmodels.TransactionsViewModel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
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
//                stormApiService.getTransactionsFromNewServiceByTerminalId(
//                    parameters.terminalId,
//                    parameters.page,
//                    parameters.pageSize
//                ).flatMap {
//
//                    println("====CHECKING_PAYLOAD" + it.data.rows.toString())
//
//                    transactionViewModel.insertIntoDatabase(it.data.rows.mapRowToTransactionResponse())
//                    Timber.d(it.data.rows.toString())
//
//                    it.data.rows = it.data.rows.map { transaction ->
//                        transaction.amount = transaction.amount.times(100)
//                        transaction
//                    }
//                    Single.just(it)
//                }.subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .doFinally {
//                        endOfDayProgressDialog.dismiss()
//                    }
//                    .subscribe { t1, t2 ->
//                        t1?.let {
//                        }
//                        t2?.let {
//                            Timber.d(it)
//                            Toast.makeText(
//                                requireContext(),
//                                "An error occurred while fetching end of day, try again",
//                                Toast.LENGTH_LONG
//                            ).show()
//                        }
//                    }.disposeWith(compositeDisposable)
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
