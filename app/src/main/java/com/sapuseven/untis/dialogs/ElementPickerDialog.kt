package com.sapuseven.untis.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.GridViewDatabaseItemAdapter
import com.sapuseven.untis.adapters.GridViewDatabaseItemCheckBoxAdapter
import com.sapuseven.untis.helpers.timetable.TimetableDatabaseInterface
import com.sapuseven.untis.models.untis.timetable.PeriodElement

class ElementPickerDialog : DialogFragment() {
	private var timetableDatabaseInterface: TimetableDatabaseInterface? = null
	private var type: TimetableDatabaseInterface.Type? = null
	private var config: ElementPickerDialogConfig? = null

	private lateinit var adapter: GridViewDatabaseItemAdapter

	private lateinit var defaultTextColor: ColorStateList
	private lateinit var holder: Holder

	private lateinit var listener: ElementPickerDialogListener

	interface ElementPickerDialogListener {
		fun onDialogDismissed(dialog: DialogInterface?)
		fun onPeriodElementClick(dialog: DialogFragment, element: PeriodElement?, useOrgId: Boolean)
		fun onPositiveButtonClicked(dialog: ElementPickerDialog)
	}

	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (context is ElementPickerDialogListener)
			listener = context
		else
			throw ClassCastException("$context must implement ElementPickerDialogListener")

		timetableDatabaseInterface?.let { timetableDatabaseInterface ->
			adapter = if (config?.multiSelect == true) GridViewDatabaseItemCheckBoxAdapter(context) else GridViewDatabaseItemAdapter(context)
			adapter.timetableDatabaseInterface = timetableDatabaseInterface
			adapter.notifyDataSetChanged()
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return activity?.let { activity ->
			val builder = AlertDialog.Builder(activity)

			builder.setView(generateView(activity))

			config?.let {
				builder.setPositiveButton(it.positiveButtonText) { dialogInterface: DialogInterface, which: Int ->
					listener.onPositiveButtonClicked(this)
				}

			}

			builder.create()
		} ?: throw IllegalStateException("Activity cannot be null")
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		listener.onDialogDismissed(dialog)
	}

	@SuppressLint("InflateParams")
	private fun generateView(activity: FragmentActivity): View {
		val root = activity.layoutInflater.inflate(R.layout.dialog_element_picker, null) as LinearLayout

		val gridView = root.findViewById<GridView>(R.id.gridview_elementpicker_list)
		gridView.setOnItemClickListener { _, _, position, _ -> listener.onPeriodElementClick(this@ElementPickerDialog, adapter.itemAt(position), false) }
		gridView.adapter = adapter

		holder = Holder(
				etSearch = root.findViewById(R.id.textinputlayout_elementpicker_search),
				tvTeachers = root.findViewById(R.id.textview_elementpicker_teachers),
				tvPersonal = root.findViewById(R.id.textview_elementpicker_personal),
				tvClasses = root.findViewById(R.id.textview_elementpicker_classes),
				tvRooms = root.findViewById(R.id.textview_elementpicker_rooms))

		defaultTextColor = holder.tvPersonal.textColors

		holder.tvPersonal.setOnClickListener {
			listener.onPeriodElementClick(this@ElementPickerDialog, null, false)
			dismiss()
		}

		holder.tvClasses.setOnClickListener {
			if (type != TimetableDatabaseInterface.Type.CLASS) {
				type = TimetableDatabaseInterface.Type.CLASS
				select(getTextViewFromElemType(type))
			}
		}

		holder.tvTeachers.setOnClickListener {
			if (type != TimetableDatabaseInterface.Type.TEACHER) {
				type = TimetableDatabaseInterface.Type.TEACHER
				select(getTextViewFromElemType(type))
			}
		}

		holder.tvRooms.setOnClickListener {
			if (type != TimetableDatabaseInterface.Type.ROOM) {
				type = TimetableDatabaseInterface.Type.ROOM
				select(getTextViewFromElemType(type))
			}
		}

		select(getTextViewFromElemType(type))

		if (config?.hideTypeSelection == true)
			root.findViewById<ConstraintLayout>(R.id.constraintlayout_elementpicker_typeselect).visibility = View.GONE

		val searchField = root.findViewById<TextInputEditText>(R.id.textinputedittext_elementpicker_search)
		searchField.addTextChangedListener(object : TextWatcher {
			override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
				adapter.filter.filter(s.toString())
			}

			override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

			override fun afterTextChanged(s: Editable) {}
		})

		return root
	}

	private fun getHint(): String {
		return getString(when (type) {
			TimetableDatabaseInterface.Type.CLASS -> R.string.hint_search_classes
			TimetableDatabaseInterface.Type.TEACHER -> R.string.hint_search_teachers
			TimetableDatabaseInterface.Type.ROOM -> R.string.hint_search_rooms
			else -> R.string.hint_search
		})
	}

	private fun select(tv: TextView) {
		deselect(holder.tvPersonal)
		deselect(holder.tvClasses)
		deselect(holder.tvTeachers)
		deselect(holder.tvRooms)

		//val oldPosition = selectedPosition
		//selectedPosition = -1
		//updateView(oldPosition)

		context?.let {
			tv.setTextColor(ContextCompat.getColor(it, R.color.colorPrimary))
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				tv.compoundDrawables[1 /* TOP */].setTint(ContextCompat.getColor(it, R.color.colorPrimary))
		}

		holder.etSearch.hint = getHint()

		adapter.type = type
		adapter.notifyDataSetChanged()
	}

	private fun deselect(tv: TextView) {
		tv.setTextColor(defaultTextColor)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			tv.compoundDrawables[1 /* TOP */].setTintList(null)
	}

	/*private fun updateView(position: Int) {
		GridView gridView = helper.getView().findViewById(R.id.gv);

		if (position >= gridView.getFirstVisiblePosition() && position <= gridView.getLastVisiblePosition())
			refreshStyling(gridView.getChildAt(position - gridView.getFirstVisiblePosition()), position);
	}*/

	private fun getTextViewFromElemType(elemType: TimetableDatabaseInterface.Type?): TextView {
		return when (elemType) {
			TimetableDatabaseInterface.Type.CLASS -> holder.tvClasses
			TimetableDatabaseInterface.Type.TEACHER -> holder.tvTeachers
			TimetableDatabaseInterface.Type.ROOM -> holder.tvRooms
			else -> holder.tvPersonal
		}
	}

	fun getSelectedItems(): List<PeriodElement> {
		return if (adapter is GridViewDatabaseItemCheckBoxAdapter)
			(adapter as GridViewDatabaseItemCheckBoxAdapter).getSelectedItems()
		else
			emptyList()
	}

	/*private fun checkIfValid() {
		if (dialog != null)
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled((selectedType == Type.UNKNOWN || selectedPosition >= 0));
	}*/

	data class Holder(
			val etSearch: TextInputLayout,
			val tvTeachers: TextView,
			val tvPersonal: TextView,
			val tvClasses: TextView,
			val tvRooms: TextView
	)

	companion object {
		fun createInstance(
				timetableDatabaseInterface: TimetableDatabaseInterface,
				config: ElementPickerDialogConfig): ElementPickerDialog {
			val fragment = ElementPickerDialog()
			fragment.timetableDatabaseInterface = timetableDatabaseInterface
			fragment.type = config.startPage
			fragment.config = config
			return fragment
		}

		data class ElementPickerDialogConfig(
				val startPage: TimetableDatabaseInterface.Type,
				val multiSelect: Boolean = false,
				val hideTypeSelection: Boolean = false,
				val positiveButtonText: String? = null
		)
	}
}