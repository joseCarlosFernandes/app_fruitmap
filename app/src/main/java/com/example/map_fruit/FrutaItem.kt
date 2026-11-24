package com.example.map_fruit

import com.google.firebase.Timestamp

data class FrutaItem(
    val id: String = "",
    val tipoFruta: String = "",
    val latitudeFruta: Double = 0.0,
    val longitudeFruta: Double = 0.0,
    val dtCadastro: Timestamp? = null
)
