package com.example.map_fruit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.Normalizer

class MapaFragment : Fragment() {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private val db = FirebaseFirestore.getInstance()

    // Launcher para solicitar a permissão de localização em tempo de execução
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mapa, container, false)

        val btnAdd = view.findViewById<Button>(R.id.btnAdd)

        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid.isNullOrEmpty()) {
            btnAdd.visibility = View.GONE
        } else {
            btnAdd.visibility = View.VISIBLE
        }

        btnAdd.setOnClickListener {
            // adicionando o layout customizado da caixa de dialogo
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.add_fruta_dialog, null)
            val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

            dialog.show()

            val btnSalvar = dialogView.findViewById<Button>(R.id.btnSalvar)
            val btnGetLocation = dialogView.findViewById<Button>(R.id.btnGetLocation)
            val etLocalizacao = dialogView.findViewById<EditText>(R.id.etLocalizacao)
            val etNomeFruta = dialogView.findViewById<AutoCompleteTextView>(R.id.etNomeFruta)

            // Configurando o AutoCompleteTextView com frutas pré-definidas
            val frutasPredefinidas = arrayOf("Maçã", "Banana", "Manga", "Laranja", "Abacate", "Pêra", "Uva", "Limão", "Goiaba", "Maracuja", "Abacaxi", "Melancia", "Pitaya", "Acerola", "Jabuticaba", "Caqui")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frutasPredefinidas)
            etNomeFruta.setAdapter(adapter)
            etNomeFruta.threshold = 1

            var latitude = 0.0
            var longitude = 0.0

            // Botão para obter a localização do Usuário
            btnGetLocation.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            latitude = it.latitude
                            longitude = it.longitude
                            etLocalizacao.setText("$latitude, $longitude")
                        }
                    }
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            // Botão para salvar a fruta
            btnSalvar.setOnClickListener {
                val nomeFruta = Normalizer.normalize( // retirando espaços, caracteres especiais, acentos e colocando em minúsculo
                    etNomeFruta.text.toString().trim().lowercase(),
                    Normalizer.Form.NFD
                ).replace("\\p{Mn}+".toRegex(), "")
                val enderecoStr = etLocalizacao.text.toString().trim()

                if (nomeFruta.isEmpty() || enderecoStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Se latitude e longitude ainda forem 0.0, converte o endereço digitado usando Geocoder
                if (latitude == 0.0 && longitude == 0.0) {
                    try {
                        val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(enderecoStr, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            latitude = address.latitude
                            longitude = address.longitude
                        } else {
                            Toast.makeText(requireContext(), "Endereço não encontrado", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Erro ao converter endereço: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                // Salvando no Firestore
                val frutaMap = hashMapOf(
                    "tipo_fruta" to nomeFruta,
                    "latitude_fruta" to latitude,
                    "longitude_fruta" to longitude,
                    "id_user_cad" to currentUserUid,
                    "endereco_fruta" to enderecoStr,
                    "ic_fruta" to "ic_${nomeFruta}"
                )

                db.collection("pontos_frutas")
                    .add(frutaMap)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Fruta salva com sucesso!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        montaMapa()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        // Inicializando o MapView
        mapView = view.findViewById(R.id.mapaFrutas)
        mapView.onCreate(savedInstanceState)
        
        mapView.getMapAsync { map ->
            googleMap = map
            checkLocationPermissionAndSetup()
            montaMapa()
        }

        return view
    }

    private fun checkLocationPermissionAndSetup() {
        val context = context ?: return
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun enableUserLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun montaMapa() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { userLocation ->
                db.collection("pontos_frutas").get().addOnSuccessListener { documents ->
                    googleMap?.clear()
                    for (document in documents) {
                        val latitude = document.data["latitude_fruta"]?.toString()?.toDoubleOrNull() ?: continue
                        val longitude = document.data["longitude_fruta"]?.toString()?.toDoubleOrNull() ?: continue
                        val nome = document.data["tipo_fruta"]?.toString() ?: "Ponto de Fruta"
                        val ic_fruta = document.data["ic_fruta"]?.toString() ?: "ic_default"

                        // Se tivermos a localização do usuário, filtramos apenas num raio de 5km (5000 metros)
                        if (userLocation != null) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                userLocation.latitude, userLocation.longitude,
                                latitude, longitude,
                                results
                            )
                            val distanceInMeters = results[0]
                            if (distanceInMeters > 5000f) {
                                continue
                            }
                        }

                        //pega o icone da fruta dentro do drawable
                        var idIcone = resources.getIdentifier(
                            ic_fruta,
                            "drawable",
                            requireContext().packageName
                        )

                        val ponto = LatLng(latitude, longitude)
                        googleMap?.addMarker(MarkerOptions().position(ponto).title(nome).icon(
                            BitmapDescriptorFactory.fromResource(idIcone)))
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro ao carregar pontos: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener {
                carregarTodosOsPontos()
            }
        } catch (e: SecurityException) {
            carregarTodosOsPontos()
        }
    }

    private fun carregarTodosOsPontos() {
        db.collection("pontos_frutas").get().addOnSuccessListener { documents ->
            googleMap?.clear()
            for (document in documents) {
                val latitude = document.data["latitude_fruta"]?.toString()?.toDoubleOrNull() ?: continue
                val longitude = document.data["longitude_fruta"]?.toString()?.toDoubleOrNull() ?: continue
                val nome = document.data["tipo_fruta"]?.toString() ?: "Ponto de Fruta"
                val ic_fruta = document.data["ic_fruta"]?.toString() ?: "ic_default"

                val ponto = LatLng(latitude, longitude)

                //pega o icone da fruta dentro do drawable
                var idIcone = resources.getIdentifier(
                    ic_fruta,
                    "drawable",
                    requireContext().packageName
                )

                googleMap?.addMarker(MarkerOptions().position(ponto).title(nome).icon(
                    BitmapDescriptorFactory.fromResource(idIcone)))
            }
        }
    }

    // Repassando o ciclo de vida para o MapView
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
