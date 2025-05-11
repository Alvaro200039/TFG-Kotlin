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

    /**
     * Valida LoginPersona: correo + contraseña.
     */
    fun validarLoginPersona(
        tilEmail: TextInputLayout, etEmail: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText
    ): Boolean {
        val okEmail = validarCampoRequerido(tilEmail,etEmail) && validarFormatoEmail(tilEmail,etEmail)
        val okPass = validarCampoRequerido(tilPass,etPass)
        return okEmail && okPass
    }

    /**
     * Valida LoginEmpresa: correo + contraseña + NIF.
     */
    fun validarLoginEmpresa(
        tilEmail: TextInputLayout, etEmail: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText,
        tilNif: TextInputLayout, etNif: TextInputEditText
    ): Boolean {
        val okEmail = validarCampoRequerido(tilEmail,etEmail) && validarFormatoEmail(tilEmail,etEmail)
        val okPass = validarCampoRequerido(tilPass,etPass)
        val okNif = validarCampoRequerido(tilNif,etNif, "Introduce el NIF")
        return okEmail && okPass && okNif
    }

    /**
     * Valida RegistroEmpresa: nombre empresa + correo + contraseña + NIF.
     */
    fun validarRegistroEmpresa(
        tilEmpresa: TextInputLayout, etEmpresa: TextInputEditText,
        tilEmail: TextInputLayout, etEmail: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText,
        tilRepContrasena: TextInputLayout, etRepContrasena: TextInputEditText,
        tilNif: TextInputLayout, etNif: TextInputEditText
    ): Boolean {
        val okEmpresa = validarCampoRequerido(tilEmpresa,etEmpresa, "Introduce el nombre de la empresa")
        val okEmail = validarCampoRequerido(tilEmail,etEmail) && validarFormatoEmail(tilEmail,etEmail)
        val okPass = validarCampoRequerido(tilPass,etPass)
        val okRep = validarCampoRequerido(tilRepContrasena, etRepContrasena, "Repite la contraseña")

        // Solo si contraseña y repetición no están vacías, comprobamos coincidencia
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
        val okNif = validarCampoRequerido(tilNif, etNif, "Introduce el NIF de la empresa")
        return okEmpresa && okEmail && okPass && okRep && okMatch && okNif
    }


    /**
     * Valida RegistroPersona: nombre + apellidos + correo + contraseña.
     */
    fun validarRegistroPersona(
        tilNombre: TextInputLayout, etNombre: TextInputEditText,
        tilApellidos: TextInputLayout, etApellidos: TextInputEditText,
        tilEmail: TextInputLayout, etEmail: TextInputEditText,
        tilRepContrasena: TextInputLayout, etRepContrasena: TextInputEditText,
        tilPass: TextInputLayout, etPass: TextInputEditText,

    ): Boolean {
        val okNombre = validarCampoRequerido(tilNombre,etNombre, "Introduce tu nombre")
        val okApellidos = validarCampoRequerido(tilApellidos,etApellidos, "Introduce tus apellidos")
        val okEmail = validarCampoRequerido(tilEmail,etEmail) && validarFormatoEmail(tilEmail,etEmail)
        val okPass = validarCampoRequerido(tilPass,etPass, "Introduce la contraseña")
        val okRep = validarCampoRequerido(tilRepContrasena, etRepContrasena, "Repite la contraseña")
        return okNombre && okApellidos && okEmail && okPass
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

        return okNombre && okApellidos && okEmail && okPass && okRep && okMatch
    }


}