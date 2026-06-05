package com.streamvault.ui.sources

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.streamvault.data.model.SavedSource
import com.streamvault.data.repository.MainRepository
import com.streamvault.databinding.ActivityAddSourceBinding
import java.util.UUID

class AddSourceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddSourceBinding
    private lateinit var repo: MainRepository
    private val PICK_FILE = 101
    private var editingSource: SavedSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSourceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = MainRepository(getSharedPreferences("streamvault", Context.MODE_PRIVATE))
        binding.btnBack.setOnClickListener { finish() }
        setupTabs()
        setupButtons()
        loadExistingSources()
    }

    private fun setupTabs() {
        binding.tabM3uUrl.setOnClickListener { showTab(0) }
        binding.tabXtream.setOnClickListener { showTab(1) }
        binding.tabFile.setOnClickListener { showTab(2) }
        showTab(0)
    }

    private fun showTab(tab: Int) {
        binding.panelM3uUrl.visibility = if (tab == 0) View.VISIBLE else View.GONE
        binding.panelXtream.visibility = if (tab == 1) View.VISIBLE else View.GONE
        binding.panelFile.visibility = if (tab == 2) View.VISIBLE else View.GONE
    }

    private fun setupButtons() {
        binding.btnAddM3u.setOnClickListener {
            val name = binding.etM3uName.text.toString().trim()
            val url = binding.etM3uUrl.text.toString().trim()
            if (name.isEmpty() || url.isEmpty()) { toast("Completa todos los campos"); return@setOnClickListener }
            val id = editingSource?.id ?: UUID.randomUUID().toString()
            repo.saveSource(SavedSource(id = id, name = name, type = "M3U_URL", m3uUrl = url))
            toast(if (editingSource != null) "Lista actualizada" else "Lista M3U agregada")
            editingSource = null
            clearM3uForm()
            loadExistingSources()
        }
        binding.btnAddXtream.setOnClickListener {
            val name = binding.etXtreamName.text.toString().trim()
            val url = binding.etXtreamUrl.text.toString().trim()
            val user = binding.etXtreamUser.text.toString().trim()
            val pass = binding.etXtreamPass.text.toString().trim()
            if (name.isEmpty() || url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                toast("Completa todos los campos"); return@setOnClickListener
            }
            val id = editingSource?.id ?: UUID.randomUUID().toString()
            repo.saveSource(SavedSource(id = id, name = name, type = "XTREAM",
                xtreamUrl = url, xtreamUser = user, xtreamPass = pass))
            toast(if (editingSource != null) "Fuente actualizada" else "Fuente Xtream agregada")
            editingSource = null
            clearXtreamForm()
            loadExistingSources()
        }
        binding.btnPickFile.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }, PICK_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val name = binding.etFileName.text.toString().trim().ifEmpty { uri.lastPathSegment ?: "M3U Local" }
            repo.saveSource(SavedSource(id = UUID.randomUUID().toString(), name = name,
                type = "M3U_FILE", localFilePath = uri.toString()))
            toast("Archivo M3U agregado")
            loadExistingSources()
        }
    }

    private fun loadExistingSources() {
        val srcs = repo.getSources()
        binding.sourcesContainer.removeAllViews()
        if (srcs.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No hay fuentes guardadas"
            tv.setTextColor(0xFF8888AA.toInt())
            tv.setPadding(8, 8, 8, 8)
            binding.sourcesContainer.addView(tv)
            return
        }
        srcs.forEach { src ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 10)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            // Icono tipo
            val icon = TextView(this).apply {
                text = when (src.type) { "XTREAM" -> "⚡"; "M3U_FILE" -> "📁"; else -> "🌐" }
                textSize = 18f
                setPadding(0, 0, 10, 0)
            }
            // Nombre y tipo
            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvName = TextView(this).apply {
                text = src.name
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                maxLines = 1
            }
            val tvType = TextView(this).apply {
                text = when (src.type) { "XTREAM" -> "Xtream Codes"; "M3U_FILE" -> "Archivo local"; else -> src.m3uUrl ?: "" }
                setTextColor(0xFF8888AA.toInt())
                textSize = 11f
                maxLines = 1
            }
            info.addView(tvName)
            info.addView(tvType)
            // Botón editar
            val btnEdit = Button(this).apply {
                text = "✏️"
                textSize = 14f
                setPadding(12, 4, 12, 4)
                setBackgroundColor(0xFF1A1A26.toInt())
                setTextColor(0xFF00C8FF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = 6 }
                setOnClickListener { editSource(src) }
            }
            // Botón eliminar
            val btnDel = Button(this).apply {
                text = "🗑️"
                textSize = 14f
                setPadding(12, 4, 12, 4)
                setBackgroundColor(0xFF1A1A26.toInt())
                setTextColor(0xFFFF4444.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = 4 }
                setOnClickListener { confirmDelete(src) }
            }
            row.addView(icon)
            row.addView(info)
            row.addView(btnEdit)
            row.addView(btnDel)
            // Divider
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 10 }
                setBackgroundColor(0xFF2A2A3A.toInt())
            }
            binding.sourcesContainer.addView(row)
            binding.sourcesContainer.addView(divider)
        }
    }

    private fun editSource(src: SavedSource) {
        editingSource = src
        when (src.type) {
            "M3U_URL" -> {
                showTab(0)
                binding.etM3uName.setText(src.name)
                binding.etM3uUrl.setText(src.m3uUrl)
                binding.btnAddM3u.text = "ACTUALIZAR LISTA"
                toast("Editando: ${src.name}")
            }
            "XTREAM" -> {
                showTab(1)
                binding.etXtreamName.setText(src.name)
                binding.etXtreamUrl.setText(src.xtreamUrl)
                binding.etXtreamUser.setText(src.xtreamUser)
                binding.etXtreamPass.setText(src.xtreamPass)
                binding.btnAddXtream.text = "ACTUALIZAR XTREAM"
                toast("Editando: ${src.name}")
            }
        }
        // Scroll arriba para ver el formulario
        binding.scrollView.smoothScrollTo(0, 0)
    }

    private fun confirmDelete(src: SavedSource) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar fuente")
            .setMessage("Eliminar '${src.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                repo.deleteSource(src.id)
                toast("Fuente eliminada")
                loadExistingSources()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearM3uForm() {
        binding.etM3uName.text?.clear()
        binding.etM3uUrl.text?.clear()
        binding.btnAddM3u.text = "AGREGAR LISTA M3U"
    }

    private fun clearXtreamForm() {
        binding.etXtreamName.text?.clear()
        binding.etXtreamUrl.text?.clear()
        binding.etXtreamUser.text?.clear()
        binding.etXtreamPass.text?.clear()
        binding.btnAddXtream.text = "AGREGAR XTREAM CODES"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
