package com.example.map_fruit

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

val db = FirebaseFirestore.getInstance()

class MainActivity : AppCompatActivity() {

    private fun abrirFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }




        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        //estado inicial quando o app é aberto
        if(savedInstanceState == null){
            abrirFragment(MapaFragment())
        }

        //barra de navegação
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId){
                R.id.nav_map -> {
                    abrirFragment(MapaFragment())
                    true
                }

                R.id.nav_perfil -> {
                    true
                }
                else -> false
            }
        }
    }
}