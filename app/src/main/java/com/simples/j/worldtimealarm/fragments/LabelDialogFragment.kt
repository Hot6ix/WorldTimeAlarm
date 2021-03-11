package com.simples.j.worldtimealarm.fragments

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.databinding.LabelDialogViewBinding
import com.simples.j.worldtimealarm.models.AlarmGeneratorViewModel

class LabelDialogFragment: DialogFragment() {

    private var listener: OnDialogEventListener? = null
    private var lastLabel: String = ""
    private var currentLabel: String = ""

    private lateinit var fragmentContext: Context
    private lateinit var binding: LabelDialogViewBinding
    private lateinit var viewModel: AlarmGeneratorViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.fragmentContext = context
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.run {
            viewModel = ViewModelProvider(this)[AlarmGeneratorViewModel::class.java]
        }

        if(savedInstanceState != null) {
            currentLabel = savedInstanceState.getString(CURRENT_LABEL, "")
        }

        viewModel.label.value?.let {
            lastLabel = it
            currentLabel = it
        }

        binding = LabelDialogViewBinding.inflate(layoutInflater, null, false)
        binding.label.apply {
            setText(lastLabel)
            setTextColor(ContextCompat.getColor(fragmentContext, R.color.colorPrimary))
            text?.length?.let {
                setSelection(it)
            }
        }

        binding.label.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable) {
                currentLabel = s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        val dialog = AlertDialog.Builder(fragmentContext)
                .setTitle(resources.getString(R.string.label))
                .setView(binding.root)
                .setPositiveButton(resources.getString(R.string.ok)) { dialogInterface, _ ->
                    listener?.onPositiveButtonClick(dialogInterface, currentLabel)
                }
                .setNegativeButton(resources.getString(R.string.cancel)) { dialogInterface, _ ->
                    listener?.onNegativeButtonClick(dialogInterface)
                }
                .setOnCancelListener { currentLabel = lastLabel }

        return dialog.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(CURRENT_LABEL, binding.label.text.toString())
    }

    fun setOnDialogEventListener(listener: OnDialogEventListener) {
        this.listener = listener
    }

    fun setLastLabel(label: String) {
        this.lastLabel = label
        this.currentLabel = label
    }

    interface OnDialogEventListener {
        fun onPositiveButtonClick(inter: DialogInterface, label: String)
        fun onNegativeButtonClick(inter: DialogInterface)
        fun onNeutralButtonClick(inter: DialogInterface)
    }

    companion object {
        fun newInstance() = LabelDialogFragment()

        const val CURRENT_LABEL = "CURRENT_LABEL"
    }
}