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
    private val onDeleteClick: (Contador) -> Unit
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

        // Aplicar fondo verde y mostrar botón eliminar solo si la lectura es de hoy
        if (contador.isToday()) {
            itemContainer.setBackgroundResource(R.drawable.bg_today_item)

            // Mostrar botón de eliminar y configurarlo
            deleteButton.visibility = View.VISIBLE
            ImageViewCompat.setImageTintList(
                deleteButton,
                ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            )
            deleteButton.setOnClickListener {
                onDeleteClick(contador)
            }
        } else {
            // Restaurar fondo blanco con bordes redondeados
            itemContainer.setBackgroundResource(R.drawable.bg_item_contador)

            // Ocultar botón de eliminar
            deleteButton.visibility = View.GONE
        }

        return view
    }
}

