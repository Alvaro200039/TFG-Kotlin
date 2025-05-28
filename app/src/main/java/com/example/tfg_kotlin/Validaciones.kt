package com.example.tfg_kotlin

import android.util.Patterns
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object Validaciones {
    /**
     * Extensión para validar campo obligatorio.
     */
    fun validarCampoRequerido(
        til: TextInputLayout,
        et: TextInputEditText,
        message: String = "Este campo es obligatorio"
    ): Boolean {
        val text = et.text?.toString()?.trim().orEmpty()
        return if (text.isEmpty()) {
            til.error = message
            et.requestFocus()
            false
        } else {
            til.error = null
            true
        }
    }

    /**
     * Extensión para validar formato de correo.
     */
    fun validarFormatoEmail(
        til: TextInputLayout,
        et: TextInputEditText
    ): Boolean {
        val email = et.text?.toString()?.trim().orEmpty()
        return when {
            email.isEmpty() -> {
                til.error = "El correo es obligatorio"
                et.requestFocus()
                false
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                til.error = "Formato de correo inválido"
                et.requestFocus()
                false
            }

            else -> {
                til.error = null
                true
            }
        }
    }
    fun validarCif(
        til: TextInputLayout,
        et: TextInputEditText
    ): Boolean {
        val cif = et.text?.toString()?.trim().orEmpty()
        val regex = Regex("^[A-Z]{1}[0-9]{7}[A-Z0-9]{1}")

        return when {
            cif.isEmpty() -> {
                til.error = "El CIF es obligatorio"
                et.requestFocus()
                false
            }
            !regex.matches(cif) -> {
                til.error = "Formato de CIF inválido (1 letra + 7 dígitos + 1 carácter de control)"
                et.requestFocus()
                false
            }
            else -> {
                til.error = null
                true
            }
        }
    }
    fun validarCifOpcional(
        til: TextInputLayout,
        et: TextInputEditText
    ): Boolean {
        val cif = et.text?.toString()?.trim().orEmpty()
        if (cif.isEmpty()) {
            til.error = null
            return true
        }

        val regex = Regex("^[A-Z]{1}[0-9]{7}[A-Z0-9]{1}")
        return if (!regex.matches(cif)) {
            til.error = "Formato de CIF inválido (1 letra + 7 dígitos + 1 carácter de control)"
            et.requestFocus()
            false
        } else {
            til.error = null
            true
        }
    }

    /**
     * Valida LoginPersona: correo + contraseña.
     */
    fun validarLogin(
        tilEmail: TextInputLayout, etEmail: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText

    ): Boolean {
        val okEmail = validarCampoRequerido(tilEmail,etEmail) && validarFormatoEmail(tilEmail,etEmail)
        val okPass = validarCampoRequerido(tilPass,etPass, "Introduce la contraseña")

        return okEmail && okPass
    }


    /**
     * Valida RegistroEmpresa: nombre empresa + correo + contraseña + NIF.
     */
    fun validarRegistroEmpresa(
        tilNombre: TextInputLayout, etNombre: TextInputEditText,
        tilDominio: TextInputLayout, etDominio: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText,
        tilRep: TextInputLayout, etRep: TextInputEditText,
        tilCif: TextInputLayout, etCif: TextInputEditText
    ): Boolean {
        val okNombre = validarCampoRequerido(tilNombre, etNombre)
        val okDominio = validarCampoRequerido(tilDominio, etDominio)
        val okPass = validarCampoRequerido(tilPass, etPass)
        val okRep = validarCampoRequerido(tilRep, etRep)
        val okCif = validarCif(tilCif, etCif)

        return okNombre && okDominio && okPass && okRep && okCif
    }


    /**
     * Valida RegistroPersona: nombre + apellidos + correo + contraseña.
     */
    fun validarRegistroPersona(
        tilNombre: TextInputLayout, etNombre: TextInputEditText,
        tilApellidos: TextInputLayout, etApellidos: TextInputEditText,
        tilEmail: TextInputLayout, etEmail: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText,
        tilRepContrasena: TextInputLayout, etRepContrasena: TextInputEditText,
        tilCif: TextInputLayout, etCif: TextInputEditText

    ): Boolean {
        val okNombre = validarCampoRequerido(tilNombre,etNombre, "Introduce tu nombre")
        val okApellidos = validarCampoRequerido(tilApellidos,etApellidos, "Introduce tus apellidos")
        val okEmail = validarCampoRequerido(tilEmail,etEmail) && validarFormatoEmail(tilEmail,etEmail)
        val okPass = validarCampoRequerido(tilPass,etPass, "Introduce la contraseña")
        val okRep = validarCampoRequerido(tilRepContrasena, etRepContrasena, "Repite la contraseña")
        val okCif =  validarCifOpcional(tilCif, etCif)

        //  // Solo si contraseña y repetición no están vacías, comprobamos coincidencia
        val okMatch = if (okPass && okRep) {
            val pass = etPass.text?.toString().orEmpty()
            val rep = etRepContrasena.text?.toString().orEmpty()
            if (pass == rep) {
                tilRepContrasena.error = null
                true
            } else {
                tilRepContrasena.error = "Las contraseñas no coinciden"
                false
            }
        } else false

        return okNombre && okApellidos && okEmail && okPass && okRep && okMatch && okCif
    }

    fun construirNombreBD(dominio: String): String {
        return "empresa_${dominio.lowercase().replace(".", "_")}.db"
    }



}