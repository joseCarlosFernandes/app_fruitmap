package com.example.map_fruit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class FrutaAdapter(
    private val frutasList: List<FrutaItem>,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<FrutaAdapter.FrutaViewHolder>() {

    class FrutaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCardNomeFruta: TextView = view.findViewById(R.id.tvCardNomeFruta)
        val tvCardData: TextView = view.findViewById(R.id.tvCardData)
        val tvCardEndereco: TextView = view.findViewById(R.id.tvEndereco)
        val btnExcluir: Button = view.findViewById(R.id.btnExcluir)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrutaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fruta_card, parent, false)
        return FrutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrutaViewHolder, position: Int) {
        val fruta = frutasList[position]
        holder.tvCardNomeFruta.text = fruta.tipoFruta.replaceFirstChar { it.uppercase() }

        val dataFormatada = if (fruta.dtCadastro != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(fruta.dtCadastro.toDate())
        } else {
            "Data não disponível"
        }

        holder.tvCardData.text = "Cadastrado em: $dataFormatada"

        // Converte coordenadas para endereço
        try {
            val geocoder = android.location.Geocoder(holder.itemView.context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(fruta.latitudeFruta, fruta.longitudeFruta, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                holder.tvCardEndereco.text = address.getAddressLine(0) ?: "Endereço desconhecido"
            } else {
                holder.tvCardEndereco.text = "Endereço não encontrado"
            }
        } catch (e: Exception) {
            holder.tvCardEndereco.text = "Localização: ${fruta.latitudeFruta}, ${fruta.longitudeFruta}"
        }

        holder.btnExcluir.setOnClickListener {
            onDeleteClick(fruta.id)
        }
    }

    override fun getItemCount() = frutasList.size
}
