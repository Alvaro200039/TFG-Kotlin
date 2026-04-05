import androidx.activity.viewModels
import com.example.tfg_kotlin.databinding.ActivityMenuCreadorBinding
import com.example.tfg_kotlin.ui.viewmodel.MenuViewModel

class MenuCreadorActivity : BaseMenuActivity() {

    private lateinit var binding: ActivityMenuCreadorBinding
    override val menuViewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuCreadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.menucreacion) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupObservers()
        setupListeners()
        
        setupBaseObservers(binding.textProximaReserva)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
            setHomeAsUpIndicator(R.drawable.ic_adagora)
        }
    }

    private fun setupObservers() {
        menuViewModel.usuario.observe(this) { usuario ->
            // Update UI if needed based on user data
        }

        menuViewModel.error.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnEditarSalas.setOnClickListener {
            startActivity(Intent(this, CreacionActivity::class.java))
        }

        binding.btnNuevaReserva.setOnClickListener {
            handleNuevaReserva()
        }

        binding.btnVerReservas.setOnClickListener {
            mostrarDialogoReservas()
        }

        binding.btnLogout.setOnClickListener {
            menuViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun handleNuevaReserva() {
        lifecycleScope.launch {
            try {
                val sesion = Sesion.datos ?: return@launch
                val empresaId = sesion.empresa.nombre
                
                if (empresaId.isEmpty()) {
                    Toast.makeText(this@MenuCreadorActivity, getString(R.string.err_empresa_no_definida), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val empresaActualizada = firestoreRepo.getEmpresaByNombre(empresaId)
                if (empresaActualizada == null) {
                    Toast.makeText(this@MenuCreadorActivity, getString(R.string.err_empresa_no_encontrada), Toast.LENGTH_SHORT).show()
                    return@launch
                }
                sesion.empresa = empresaActualizada

                val pisos = firestoreRepo.getPisosByEmpresa(empresaId)
                if (pisos.isNotEmpty()) {
                    sesion.pisos = listOf(pisos.last())
                    startActivity(Intent(this@MenuCreadorActivity, EmpleadosActivity::class.java))
                } else {
                    Toast.makeText(this@MenuCreadorActivity, getString(R.string.msg_no_piso_creado), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MenuCreadorActivity, getString(R.string.err_cargar_pisos), Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}

