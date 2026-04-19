package com.dante.workcycle.ui.template

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.dante.workcycle.R

class TemplateDropdownAdapter(
    context: Context,
    private val items: List<TemplateDropdownItem>
) : ArrayAdapter<TemplateDropdownItem>(context, 0, items.toMutableList()), Filterable {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_OPTION = 1
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): TemplateDropdownItem = items[position]

    override fun isEnabled(position: Int): Boolean {
        return getItem(position) is TemplateDropdownItem.Option
    }

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is TemplateDropdownItem.Header) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_OPTION
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        val text = view.findViewById<TextView>(android.R.id.text1)

        when (val item = getItem(position)) {
            is TemplateDropdownItem.Option -> text.text = item.title
            is TemplateDropdownItem.Header -> text.text = item.title
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val item = getItem(position)) {

            is TemplateDropdownItem.Header -> {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)

                val text = view.findViewById<TextView>(android.R.id.text1)
                text.text = context.getString(
                    R.string.template_dropdown_header_format,
                    item.title.uppercase()
                )
                text.textSize = 13f
                text.setTypeface(text.typeface, android.graphics.Typeface.BOLD)
                text.alpha = 0.75f
                text.setPadding(24, 20, 16, 8)

                view.isEnabled = false
                view
            }

            is TemplateDropdownItem.Option -> {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)

                val text = view.findViewById<TextView>(android.R.id.text1)
                text.text = context.getString(R.string.template_dropdown_option_format, item.title)
                text.textSize = 16f
                text.setPadding(24, 16, 16, 16)

                view
            }
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                return FilterResults().apply {
                    values = items
                    count = items.size
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                addAll(items)
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return when (resultValue) {
                    is TemplateDropdownItem.Option -> resultValue.title
                    is TemplateDropdownItem.Header -> resultValue.title
                    else -> ""
                }
            }
        }
    }
}
