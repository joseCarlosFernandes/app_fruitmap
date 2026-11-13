package com.example.map_fruit

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private lateinit var btnLogin: Button
    private lateinit var btnCriar: Button
    private lateinit var imgPerfil: ImageView
    private lateinit var nomeUsuario: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        btnLogin = view.findViewById(R.id.btnLogin)
        btnCriar = view.findViewById(R.id.btnCriar)
        imgPerfil = view.findViewById(R.id.imgPerfil)
        nomeUsuario = view.findViewById(R.id.nomeUsuario)

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
            imgPerfil.visibility = View.GONE
            nomeUsuario.visibility = View.GONE
        } else {
            btnLogin.visibility = View.GONE
            btnCriar.visibility = View.GONE
            imgPerfil.visibility = View.VISIBLE
            nomeUsuario.visibility = View.VISIBLE

            // Buscando o nome do usuário na collection "usuarios" do Firestore
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
        }
    }
}
