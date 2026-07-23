package com.detox.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/**
 * Deliberately plain: no icons, no color, just app name + optional
 * usage-time subtitle + a pin indicator. This is the whole point of a
 * digital-detox launcher — remove the visual bait.
 */
class AppListAdapter(
    private val inflater: LayoutInflater,
    private var items: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit,
    private val onLongPress: (AppInfo) -> Unit
) : ArrayAdapter<AppInfo>(inflater.context, 0) {

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): AppInfo = items[position]

    fun updateItems(newItems: List<AppInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_app, parent, false)
        val app = items[position]

        val nameView = view.findViewById<TextView>(R.id.app_name)
        val metaView = view.findViewById<TextView>(R.id.app_meta)

        val pinMark = if (app.isPinned) "\u2605 " else ""
        nameView.text = pinMark + app.label

        metaView.text = if (app.usageMinutesToday > 0) {
            "${app.usageMinutesToday} min today"
        } else {
            "—"
        }

        view.setOnClickListener { onClick(app) }
        view.setOnLongClickListener { onLongPress(app); true }

        return view
    }
}
