package com.example.map_fruit

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private lateinit var btnLogin: Button
    private lateinit var btnCriar: Button
    private lateinit var nomeUsuario: TextView
    private lateinit var rvMinhasFrutas: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        btnLogin = view.findViewById(R.id.btnLogin)
        btnCriar = view.findViewById(R.id.btnCriar)
        nomeUsuario = view.findViewById(R.id.nomeUsuario)
        rvMinhasFrutas = view.findViewById(R.id.rvMinhasFrutas)
        rvMinhasFrutas.layoutManager = LinearLayoutManager(requireContext())

        btnCriar.setOnClickListener {
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        atualizarEstadoUsuario()
    }

    private fun atualizarEstadoUsuario() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid.isNullOrEmpty()) {
            btnLogin.visibility = View.VISIBLE
            btnCriar.visibility = View.VISIBLE
            nomeUsuario.visibility = View.GONE
            rvMinhasFrutas.visibility = View.GONE
        } else {
            btnLogin.visibility = View.GONE
            btnCriar.visibility = View.GONE
            nomeUsuario.visibility = View.VISIBLE
            rvMinhasFrutas.visibility = View.VISIBLE

            // Buscando o nome do usuário na collection "usuarios"
            FirebaseFirestore.getInstance().collection("usuarios")
                .document(currentUserUid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nome = document.getString("nome") ?: "Usuário"
                        nomeUsuario.text = nome
                    } else {
                        nomeUsuario.text = "Usuário"
                    }
                }
                .addOnFailureListener {
                    nomeUsuario.text = "Erro ao carregar nome"
                }

            // Buscando as frutas cadastradas pelo usuário na collection "pontos_frutas"
            FirebaseFirestore.getInstance().collection("pontos_frutas")
                .whereEqualTo("id_user_cad", currentUserUid)
                .get()
                .addOnSuccessListener { documents ->
                    val listaFrutas = mutableListOf<FrutaItem>()
                    for (doc in documents) {
                        val id = doc.id
                        val tipo = doc.getString("tipo_fruta") ?: ""
                        val latitude = doc.getDouble("latitude_fruta") ?: 0.0
                        val longitude = doc.getDouble("longitude_fruta") ?: 0.0
                        val data = doc.getTimestamp("dt_cadastro")
                        listaFrutas.add(FrutaItem(id, tipo, latitude, longitude, data))
                    }
                    val adapter = FrutaAdapter(listaFrutas) { frutaId ->
                        // Ação de exclusão do documento no Firestore
                        FirebaseFirestore.getInstance().collection("pontos_frutas")
                            .document(frutaId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Fruta excluída com sucesso!", Toast.LENGTH_SHORT).show()
                                atualizarEstadoUsuario() // Atualiza a lista após excluir
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    rvMinhasFrutas.adapter = adapter
                }
                .addOnFailureListener {
                    rvMinhasFrutas.adapter = null
                    rvMinhasFrutas.visibility = View.GONE
                }
        }
    }
}
