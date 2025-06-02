package com.example.tfg_kotlin

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.tfg_kotlin.BBDD_Global.Database.GlobalDB
import com.example.tfg_kotlin.BBDD_Maestra.Database.MasterDB
import com.example.tfg_kotlin.BBDD_Maestra.Entities.Empresa
import kotlinx.coroutines.launch

class RegistroEmpresa : AppCompatActivity() {
    private lateinit var editNombre: EditText
    private lateinit var editDominio: EditText
    private lateinit var editCif: EditText
    private lateinit var btnRegistrar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_empresa)

        editNombre = findViewById(R.id.editNombreEmpresa)
        editDominio = findViewById(R.id.editDominio)
        editCif = findViewById(R.id.editCif)
        btnRegistrar = findViewById(R.id.btnRegistrarEmpresa)

        // Acción al pulsar el botón
        btnRegistrar.setOnClickListener {
            registrarEmpresa()
        }
    }

    private fun registrarEmpresa() {
        val nombre = editNombre.text.toString().trim()
        val dominio = editDominio.text.toString().trim()
        val cif = editCif.text.toString().trim().uppercase()

        //Definición - creación BD_Maestra
        val db = Room.databaseBuilder(
            applicationContext,
            MasterDB::class.java, "db_maestra.db"
        ).build()

        val dao = db.empresaDao()


        // Validacion comprobacion todos los dato en los textView
        if (nombre.isEmpty() || dominio.isEmpty() || cif.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validacion formato dominio
        if (!dominio.startsWith("@") || !dominio.contains(".")) {
            Toast.makeText(this, "Dominio inválido (ej: @empresa.com)", Toast.LENGTH_SHORT).show()
            return
        }

        //Definición de los datos a introducir en la tabla Empleados -> BD_Maestra
        val empresa = Empresa(cif, nombre, dominio)

        // Room puede tardar vastante tiempo en acceder o realizar acciones en bd incluso puede congelar la aplicacoón
        // Para que esto no suceda, dejamos que la interfaz gráfica se ejecute en un hilo principal
        // y hacemos que room se ejecute como en un segundo plano
        // Para ello usamos esta opcón
        lifecycleScope.launch {

            val cifExiste =dao.buscarPorCif(cif)
            val dominioExiste = dao.buscarPorDominio(dominio)

            // Validación de cif existente
            if (cifExiste != null) {
                Toast.makeText(this@RegistroEmpresa, "Cif Existente", Toast.LENGTH_SHORT).show()
                editCif.requestFocus()
            }
            // Validación de dominio existente
            else if (dominioExiste != null) {
                Toast.makeText(this@RegistroEmpresa, "Dominio Existente", Toast.LENGTH_SHORT).show()
                editDominio.requestFocus()
            }
            // Una vez se han validado todos los datos, se realizan las acciones de inserción de datos
            // y cración de las bd_individuales
            else {
                // Inserción de datos
                dao.insertarEmpresa(empresa)
                Log.d("DB_LOG", "Empresa registrada: CIF=${empresa.cif}, Nombre=${empresa.nombre}, Dominio=${empresa.dominio}")

                //Creación del nombre de las bd_individuales
                val dbnombre = "db_${nombre.lowercase().replace(" ", "_")}"
                val dbGlobal = Room.databaseBuilder(
                    applicationContext,
                    GlobalDB::class.java,
                    "$dbnombre.db"
                ).build()

                //Room como tal no crea la bd, por ello hay que realizar alguna acción para que estas se creen
                //Con esta acción accedemos a la bd individual y se crea
                dbGlobal.openHelper.writableDatabase
                Log.d("DB_LOG", "BD de empresa creada $dbnombre.db")

                // Como hemos dicho, room se está ejecutando en un hilo secundario
                // Lo que hace esta línea es volvernos a dejar ejecutar acciones en el hilo principal
                runOnUiThread {
                    Toast.makeText(this@RegistroEmpresa, "Empresa registrada", Toast.LENGTH_SHORT).show()
                    editCif.text.clear()
                    editNombre.text.clear()
                    editDominio.text.clear()
                }
            }
        }
    }
}