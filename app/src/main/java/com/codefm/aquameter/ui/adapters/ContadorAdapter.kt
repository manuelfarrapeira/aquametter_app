package com.codefm.aquameter.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.codefm.aquameter.R
import com.codefm.aquameter.model.Contador

/**
 * Adapter para mostrar la lista de contadores en un ListView
 */
class ContadorAdapter(
    context: Context,
    private val contadores: List<Contador>,
    private val onDeleteClick: (Contador) -> Unit,
    private val onItemClick: (Contador) -> Unit
) : ArrayAdapter<Contador>(context, R.layout.item_contador, contadores) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_contador, parent, false)

        val contador = contadores[position]

        val itemContainer = view.findViewById<View>(R.id.itemContainer)
        val nombreText = view.findViewById<TextView>(R.id.nombreText)
        val contadorText = view.findViewById<TextView>(R.id.contadorText)
        val deleteButton = view.findViewById<ImageView>(R.id.deleteButton)

        nombreText.text = contador.nombre
        contadorText.text = context.getString(R.string.contador_format, contador.codigoContador)

        // Click en el item completo
        itemContainer.setOnClickListener {
            onItemClick(contador)
        }

        // Determinar el drawable según el exceso y si es de hoy
        val isToday = contador.isToday()

        val backgroundDrawable = if (isToday) {
            // Items de hoy: aplicar esquema de colores según exceso
            val exceso = contador.getLastExceso()
            when {
                exceso > 0 -> R.drawable.bg_today_exceso
                exceso < 0 -> R.drawable.bg_today_sin_exceso
                else -> R.drawable.bg_today_0
            }
        } else {
            // Items de otros días: siempre borde verde con fondo blanco
            R.drawable.bg_item_border_green
        }

        itemContainer.setBackgroundResource(backgroundDrawable)

        // Mostrar botón de eliminar solo si la lectura es de hoy
        if (isToday) {
            deleteButton.visibility = View.VISIBLE
            ImageViewCompat.setImageTintList(
                deleteButton,
                ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            )
            deleteButton.setOnClickListener {
                onDeleteClick(contador)
            }
        } else {
            deleteButton.visibility = View.GONE
        }

        return view
    }
}

