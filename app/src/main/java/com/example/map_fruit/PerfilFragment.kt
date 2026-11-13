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

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PerfilFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnCriar = view.findViewById<Button>(R.id.btnCriar)
        val imgPerfil = view.findViewById<ImageView>(R.id.imgPerfil)
        val nomeUsuario = view.findViewById<TextView>(R.id.nomeUsuario)

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

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PerfilFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
